package com.aster.service.mode

import android.content.Context
import android.util.Log
import com.aster.data.local.db.ToolCallLogger
import com.aster.service.CommandHandler
import com.aster.util.TailscaleUtils
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCMessage
import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Custom Ktor plugin that serializes JSONRPCMessage responses to JSON
 * without interfering with request body reading (unlike ContentNegotiation).
 */
private val McpResponseSerializer = createApplicationPlugin("McpResponseSerializer") {
    onCallRespond { _ ->
        transformBody { body ->
            when (body) {
                is JSONRPCMessage -> TextContent(
                    McpJson.encodeToString(body),
                    ContentType.Application.Json
                )

                is List<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val messages = body as List<JSONRPCMessage>
                    TextContent(
                        McpJson.encodeToString(
                            ListSerializer(JSONRPCMessage.serializer()),
                            messages
                        ),
                        ContentType.Application.Json
                    )
                }

                else -> body
            }
        }
    }
}

class McpMode(
    private val commandHandlers: Map<String, CommandHandler>,
    private val context: Context,
    private val toolCallLogger: ToolCallLogger
) : ConnectionMode {

    companion object {
        private const val TAG = "McpMode"
    }

    override val modeType = ModeType.LOCAL_MCP
    override val displayName = "Local MCP Server"

    private val _statusFlow = MutableStateFlow(ModeStatus())
    override val statusFlow: StateFlow<ModeStatus> = _statusFlow.asStateFlow()

    private var ktorServer: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? =
        null
    private var transport: StreamableHttpServerTransport? = null
    private var currentPort: Int = 8080

    override suspend fun start(config: ModeConfig) {
        val mcpConfig = config as? ModeConfig.McpConfig
            ?: throw IllegalArgumentException("McpMode requires McpConfig")

        currentPort = mcpConfig.port
        _statusFlow.value = ModeStatus(
            state = ModeState.STARTING,
            message = "Starting MCP server on port $currentPort..."
        )

        try {
            // 1. Create MCP server with tools
            val mcpServer = Server(
                serverInfo = Implementation(
                    name = "aster-mcp-android",
                    version = "1.1.5"
                ),
                options = ServerOptions(
                    capabilities = ServerCapabilities(
                        tools = ServerCapabilities.Tools(listChanged = true)
                    )
                )
            ) {
                "Aster Android device control MCP server"
            }.apply {
                McpToolRegistry.registerTools(this, commandHandlers, toolCallLogger)
            }

            // 2. Create Streamable HTTP transport (JSON responses, no SSE streaming)
            val httpTransport = StreamableHttpServerTransport(enableJsonResponse = true)
            transport = httpTransport

            // 3. Create Ktor server with /mcp endpoint
            val sessionReady = CompletableDeferred<Unit>()

            ktorServer = embeddedServer(CIO, port = currentPort) {
                // Response-only serializer — doesn't consume request body like ContentNegotiation
                install(McpResponseSerializer)
                install(SSE)

                routing {
                    route("/mcp") {
                        post {
                            sessionReady.await()
                            httpTransport.handlePostRequest(null, call)
                        }
                        delete {
                            sessionReady.await()
                            httpTransport.handleDeleteRequest(call)
                        }
                    }
                }
            }

            ktorServer?.start(wait = false)

            // 4. Connect MCP server to transport (calls transport.start() internally)
            mcpServer.createSession(httpTransport)
            sessionReady.complete(Unit)

            _statusFlow.value = ModeStatus(
                state = ModeState.RUNNING,
                message = "MCP Server — Port $currentPort"
            )
            Log.i(TAG, "MCP server started on port $currentPort (Streamable HTTP at /mcp)")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MCP server", e)
            _statusFlow.value = ModeStatus(
                state = ModeState.ERROR,
                message = "Failed: ${e.message}"
            )
            throw e
        }
    }

    override suspend fun stop() {
        _statusFlow.value =
            ModeStatus(state = ModeState.STOPPING, message = "Stopping MCP server...")
        try {
            transport?.close()
            transport = null
            ktorServer?.stop(1000, 2000)
            ktorServer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping MCP server", e)
        }
        _statusFlow.value = ModeStatus(state = ModeState.IDLE)
        Log.i(TAG, "MCP server stopped")
    }

    override fun getAvailableTools(): List<ToolInfo> {
        return ToolCatalog.resolve(commandHandlers.keys)
    }

    fun getLocalhostUrl(): String = "http://127.0.0.1:$currentPort/mcp"

    fun getWifiIp(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get WiFi IP", e)
        }
        return null
    }

    fun getLanUrl(): String? {
        val ip = getWifiIp() ?: return null
        return "http://$ip:$currentPort/mcp"
    }

    fun getTailscaleUrl(): String? {
        val ip = TailscaleUtils.getTailscaleIp() ?: return null
        return "http://$ip:$currentPort/mcp"
    }

    fun getTailscaleDnsUrl(): String? {
        val dnsName = TailscaleUtils.getTailscaleDnsName() ?: return null
        return "http://$dnsName:$currentPort/mcp"
    }
}
