package com.aster.service.mode

import android.os.Binder
import android.os.ParcelFileDescriptor
import android.util.Log
import com.aster.data.local.db.ToolCallLogger
import com.aster.data.model.Command
import com.aster.ipc.IAsterCallback
import com.aster.ipc.IAsterService
import com.aster.service.CommandHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

class IpcMode(
    private val commandHandlers: Map<String, CommandHandler>,
    private val toolCallLogger: ToolCallLogger
) : ConnectionMode {

    companion object {
        private const val TAG = "IpcMode"
        private const val TOKEN_LENGTH = 32
        /** Results above this byte size are offloaded to PFD pipe instead of Binder. */
        private const val LARGE_RESULT_THRESHOLD = 500_000 // ~500 KB

        /**
         * Screen-control action names (Screen Control /goal P7). While the kill
         * switch is engaged, an executeCommand for any of these is fast-rejected
         * (the in-flight loop is aborted within one action).
         */
        private val SCREEN_CONTROL_ACTIONS = setOf(
            "tap", "set_text", "long_press", "set_toggle", "perform", "scroll",
            "input_gesture", "press_key", "global_action", "input_text",
            "click_by_text", "click_by_view_id", "launch_intent",
            // App Automations /goal R-C — interactive overlay dialogs. Listed so
            // the kill-switch fast-reject covers them and the kernel's stamped
            // `ai_name` is read into the audit log. They render the companion's
            // own overlay (NOT the tool-execution border), so they are
            // intentionally absent from ToolExecutionOverlay's own action set.
            "screen_prompt", "screen_approve",
            // App Automations login/register wall + payment/explicit hand-off —
            // non-blocking on-device "the run is waiting for you" banners. Same
            // rationale as the dialogs above: kill-switch fast-reject + `ai_name`
            // audit, own overlay (no border).
            "screen_signin_wait", "screen_handoff"
        )
    }

    override val modeType = ModeType.IPC
    override val displayName = "IPC (Binder)"

    private val _statusFlow = MutableStateFlow(ModeStatus())
    override val statusFlow: StateFlow<ModeStatus> = _statusFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val authenticatedUids = ConcurrentHashMap<Int, Long>()
    private val callbacks = ConcurrentHashMap<Int, IAsterCallback>()
    private val largeResults = ConcurrentHashMap<String, ByteArray>()
    private var currentToken: String = ""

    /** Kill-switch flag (P7). Set true on STOP; cleared on the next start(). */
    @Volatile
    private var killed = false

    val token: String get() = currentToken

    val binder: IAsterService.Stub = object : IAsterService.Stub() {

        override fun authenticate(token: String): String {
            val callingUid = Binder.getCallingUid()
            Log.d(TAG, "authenticate() from UID=$callingUid")

            if (!constantTimeEquals(token, currentToken)) {
                Log.w(TAG, "Authentication failed: invalid token from UID=$callingUid")
                throw SecurityException("Invalid authentication token")
            }

            authenticatedUids[callingUid] = System.currentTimeMillis()
            Log.i(TAG, "UID=$callingUid authenticated successfully")
            updateClientCount()
            return "authenticated"
        }

        override fun executeCommand(action: String, paramsJson: String): String {
            val callingUid = Binder.getCallingUid()
            requireAuthenticated(callingUid)

            // Kill switch (P7): once engaged, refuse every screen-control action
            // until a fresh connect (start() clears `killed`). The session is
            // already severed (authenticatedUids cleared), so the typical path is
            // a SecurityException above; this is the belt-and-braces second stop.
            if (killed && action in SCREEN_CONTROL_ACTIONS) {
                return Json.encodeToString(
                    JsonObject.serializer(),
                    buildJsonObject {
                        put("success", JsonPrimitive(false))
                        put("error", JsonPrimitive("Screen control stopped by the kill switch. Reconnect to resume."))
                    }
                )
            }

            val handler = commandHandlers[action]
                ?: run {
                    toolCallLogger.log(
                        action = action,
                        connectionType = "IPC",
                        success = false,
                        errorMessage = "Unknown action: $action"
                    )
                    return Json.encodeToString(
                        JsonObject.serializer(),
                        buildJsonObject {
                            put("success", JsonPrimitive(false))
                            put("error", JsonPrimitive("Unknown action: $action"))
                        }
                    )
                }

            val params = if (paramsJson.isNotBlank()) {
                try {
                    Json.parseToJsonElement(paramsJson).jsonObject.mapValues { it.value }
                } catch (e: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            val command = Command(
                type = "command",
                id = java.util.UUID.randomUUID().toString(),
                action = action,
                params = params
            )

            // Screen Control /goal P7 audit context: the observed target text
            // rides in the action params; `resolved_by` comes back in the
            // action result. Only meaningful for screen-control actions; other
            // actions log null for these (defaulted).
            val isScreenAction = action in SCREEN_CONTROL_ACTIONS
            val targetText = if (isScreenAction) {
                (params["target_text"] as? JsonPrimitive)?.contentOrNull
            } else null
            // P7 audit (SPEC §3.6): the kernel stamps `risk` ("low"|"high" — high only after
            // the owner confirms a high-risk dispatch) and `approval` ("approved" on the
            // screen:confirm re-dispatch) into device.execute params. Read them honestly:
            // absent → null (no fabrication). Only meaningful for screen-control actions.
            val risk = if (isScreenAction) {
                (params["risk"] as? JsonPrimitive)?.contentOrNull
            } else null
            val approval = if (isScreenAction) {
                (params["approval"] as? JsonPrimitive)?.contentOrNull
            } else null
            // App Automations /goal I4 (SPEC §I4/D6): the kernel stamps `ai_name`
            // (the EA's display name) into device.execute params — same side-channel
            // as `target_text`/`risk`. Read it honestly: absent → null (no
            // fabrication). The overlay renders it (fallback "your assistant").
            // Only meaningful for screen-control actions.
            val aiName = if (isScreenAction) {
                (params["ai_name"] as? JsonPrimitive)?.contentOrNull
            } else null

            val startTime = System.currentTimeMillis()
            toolCallLogger.onToolStarted(action, "IPC", target = targetText, risk = risk, aiName = aiName)
            val result = runBlocking {
                handler.handle(command)
            }
            val duration = System.currentTimeMillis() - startTime

            val resolvedBy = if (isScreenAction) {
                ((result.data as? JsonObject)?.get("resolved_by") as? JsonPrimitive)?.contentOrNull
            } else null

            toolCallLogger.log(
                action = action,
                connectionType = "IPC",
                success = result.success,
                durationMs = duration,
                errorMessage = result.error,
                target = targetText,
                resolvedBy = resolvedBy,
                risk = risk,
                approval = approval
            )

            val resultJson = Json.encodeToString(
                JsonObject.serializer(),
                buildJsonObject {
                    put("success", JsonPrimitive(result.success))
                    result.data?.let { put("data", it) }
                    result.error?.let { put("error", JsonPrimitive(it)) }
                }
            )

            val resultBytes = resultJson.toByteArray(Charsets.UTF_8)
            if (resultBytes.size > LARGE_RESULT_THRESHOLD) {
                val resultId = java.util.UUID.randomUUID().toString()
                largeResults[resultId] = resultBytes
                Log.d(TAG, "Large result for '$action' (${resultBytes.size} bytes) stored as $resultId")
                return """{"_largeResult":"$resultId"}"""
            }

            return resultJson
        }

        override fun readLargeResult(resultId: String): ParcelFileDescriptor {
            val callingUid = Binder.getCallingUid()
            requireAuthenticated(callingUid)

            val data = largeResults.remove(resultId)
                ?: throw IllegalArgumentException("No large result found for ID: $resultId")

            val pipe = ParcelFileDescriptor.createPipe()
            val readSide = pipe[0]
            val writeSide = pipe[1]

            Thread {
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { out ->
                        out.write(data)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to write large result to pipe", e)
                }
            }.start()

            Log.d(TAG, "Streaming large result $resultId (${data.size} bytes) via PFD pipe")
            return readSide
        }

        override fun registerCallback(callback: IAsterCallback) {
            val callingUid = Binder.getCallingUid()
            requireAuthenticated(callingUid)
            callbacks[callingUid] = callback
            Log.d(TAG, "Callback registered for UID=$callingUid")
        }

        override fun unregisterCallback(callback: IAsterCallback) {
            val callingUid = Binder.getCallingUid()
            callbacks.remove(callingUid)
            Log.d(TAG, "Callback unregistered for UID=$callingUid")
        }

        override fun getAvailableTools(): List<String> {
            val callingUid = Binder.getCallingUid()
            requireAuthenticated(callingUid)
            return commandHandlers.keys.toList()
        }

        override fun disconnect() {
            val callingUid = Binder.getCallingUid()
            authenticatedUids.remove(callingUid)
            callbacks.remove(callingUid)
            Log.i(TAG, "UID=$callingUid disconnected")
            updateClientCount()
        }
    }

    override suspend fun start(config: ModeConfig) {
        val ipcConfig = config as? ModeConfig.IpcConfig
            ?: throw IllegalArgumentException("IpcMode requires IpcConfig")

        _statusFlow.value = ModeStatus(state = ModeState.STARTING, message = "Initializing IPC...")
        currentToken = ipcConfig.token
        authenticatedUids.clear()
        callbacks.clear()
        // P7: a fresh connect resumes screen control (clears any prior kill).
        killed = false
        _statusFlow.value =
            ModeStatus(state = ModeState.RUNNING, message = "IPC Active — Ready for aster-one")
        Log.i(TAG, "IPC mode started")
    }

    override suspend fun stop() {
        _statusFlow.value = ModeStatus(state = ModeState.STOPPING, message = "Stopping IPC...")
        authenticatedUids.clear()
        callbacks.clear()
        largeResults.clear()
        scope.coroutineContext.cancelChildren()
        _statusFlow.value = ModeStatus(state = ModeState.IDLE)
        Log.i(TAG, "IPC mode stopped")
    }

    override fun getAvailableTools(): List<ToolInfo> {
        return ToolCatalog.resolve(commandHandlers.keys)
    }

    fun generateToken(): String {
        val bytes = ByteArray(TOKEN_LENGTH / 2)
        SecureRandom().nextBytes(bytes)
        currentToken = bytes.joinToString("") { "%02x".format(it) }
        return currentToken
    }

    fun broadcastEvent(eventType: String, payloadJson: String) {
        callbacks.values.forEach { callback ->
            try {
                callback.onEvent(eventType, payloadJson)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send event to callback", e)
            }
        }
    }

    /**
     * Kill switch (P7): broadcast a `screen_stop` so aster-one aborts the
     * in-flight loop, then sever every authenticated session so the NEXT
     * executeCommand is refused (both the SecurityException from the cleared
     * auth AND the [killed] fast-reject). Two independent stops = defense in
     * depth — the active run is aborted within one action.
     */
    fun killActiveControl() {
        killed = true
        broadcastEvent("screen_stop", "{}")
        authenticatedUids.clear()
        Log.w(TAG, "Kill switch engaged — sessions severed, screen_stop broadcast")
        updateClientCount()
    }

    private fun requireAuthenticated(uid: Int) {
        if (!authenticatedUids.containsKey(uid)) {
            throw SecurityException("Caller UID=$uid is not authenticated")
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    private fun updateClientCount() {
        val current = _statusFlow.value
        if (current.state == ModeState.RUNNING) {
            _statusFlow.value = current.copy(connectedClients = authenticatedUids.size)
        }
    }
}
