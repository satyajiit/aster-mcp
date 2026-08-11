package com.aster.service.handlers

import android.graphics.Rect
import com.aster.data.model.Command
import com.aster.service.AsterAccessibilityService
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import com.aster.service.Mark
import com.aster.service.ScreenActionResult
import kotlinx.serialization.json.*

/**
 * Handler for accessibility-based commands.
 * Handles screen hierarchy, gestures, global actions, text input, and screenshots.
 *
 * Guarded autonomy (Screen Control /goal P7): control actions are gated by
 * `PackagePolicyGuard`, applied to EVERY handler by
 * [com.aster.service.safety.GuardedCommandHandler] at map-construction time
 * (fail-closed) — defense in depth alongside the kernel-side denylist. It used
 * to be a call at the top of [handle], which covered only this handler's verbs
 * and left `launch_intent` ungated.
 */
class AccessibilityHandler : CommandHandler {

    companion object {
        /**
         * Post-act auto-settle bounds (SPEC §3.3). The timeout is a ceiling that
         * only matters on never-idle screens; kept short so a busy app (autoplay
         * feed, live map) doesn't make every action wait the full window. See
         * [settleAndVerify].
         */
        private const val SETTLE_QUIET_MS = 350L
        private const val SETTLE_TIMEOUT_MS = 1_500L

        /**
         * How long to wait for an app to START reacting before concluding the
         * action was inert (phase 1 of [AsterAccessibilityService.waitForSettle]).
         *
         * Sized from measurement, not taste: a stock Settings row resumes its
         * target activity ~130 ms after the tap. 600 ms is ~4.5× that headroom
         * while still bounded by [SETTLE_TIMEOUT_MS] overall, so a no-op action
         * costs 600 ms rather than the full ceiling.
         */
        private const val SETTLE_REACTION_MS = 600L
    }

    override fun supportedActions() = listOf(
        "observe",
        "get_screen_hierarchy",
        "input_gesture",
        "global_action",
        "input_text",
        "take_screenshot",
        "find_element",
        "click_by_text",
        "click_by_view_id",
        "scroll",
        // -- P2: ref-addressed actions (SPEC §3.2) --
        "tap",
        "set_text",
        "long_press",
        "set_toggle",
        "perform",
        "press_key",
        // -- P3: synchronization (SPEC §3.3) --
        "wait_for_idle",
        "wait_for"
    )

    override suspend fun handle(command: Command): CommandResult {
        val service = AsterAccessibilityService.getInstance()
            ?: return CommandResult.failure("Accessibility service not enabled. Please enable it in Settings > Accessibility > Aster by OpenAlly")

        return when (command.action) {
            "observe" -> observe(service, command)
            "get_screen_hierarchy" -> getScreenHierarchyFiltered(service, command)
            "input_gesture" -> inputGesture(service, command)
            "global_action" -> globalAction(service, command)
            "input_text" -> inputText(service, command)
            "take_screenshot" -> takeScreenshot(service, command)
            "find_element" -> findElement(service, command)
            "click_by_text" -> clickByText(service, command)
            "click_by_view_id" -> clickByViewId(service, command)
            "scroll" -> scroll(service, command)
            "tap" -> tap(service, command)
            "set_text" -> setText(service, command)
            "long_press" -> longPress(service, command)
            "set_toggle" -> setToggle(service, command)
            "perform" -> perform(service, command)
            "press_key" -> pressKey(service, command)
            "wait_for_idle" -> waitForIdle(service, command)
            "wait_for" -> waitFor(service, command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    private fun getScreenHierarchy(service: AsterAccessibilityService): CommandResult {
        val hierarchy = service.getScreenHierarchy()
        return CommandResult.success(hierarchy)
    }

    private fun getScreenHierarchyFiltered(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val mode = command.params?.get("mode")?.jsonPrimitive?.contentOrNull ?: "interactive"
        val maxDepth = command.params?.get("maxDepth")?.jsonPrimitive?.intOrNull
        val includeInvisible = command.params?.get("includeInvisible")?.jsonPrimitive?.booleanOrNull ?: false
        val searchText = command.params?.get("searchText")?.jsonPrimitive?.contentOrNull

        val hierarchy = service.getScreenHierarchy()
        val filtered = filterHierarchy(hierarchy, mode, maxDepth, includeInvisible, searchText)

        return CommandResult.success(filtered)
    }

    private suspend fun observe(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val mode = command.params?.get("mode")?.jsonPrimitive?.contentOrNull ?: "actionable"
        val searchText = command.params?.get("searchText")?.jsonPrimitive?.contentOrNull
        val maxElements = command.params?.get("maxElements")?.jsonPrimitive?.intOrNull
            ?: com.aster.service.accessibility.ElementFilter.MAX_ELEMENTS_DEFAULT
        // P4 OCR gate (SPEC §3.4): omitted = auto (OCR only when the a11y tree is sparse);
        // true = force OCR; false = a11y only.
        val ocr = command.params?.get("ocr")?.jsonPrimitive?.booleanOrNull

        val observation = service.observe(mode, searchText, maxElements, ocr)
        return CommandResult.success(observation)
    }

    private fun filterHierarchy(
        node: JsonElement,
        mode: String,
        maxDepth: Int?,
        includeInvisible: Boolean,
        searchText: String?,
        currentDepth: Int = 0
    ): JsonElement? {
        if (node !is JsonObject) return null

        // Check depth limit
        if (maxDepth != null && currentDepth > maxDepth) return null

        // Check visibility
        val isVisible = node["isVisibleToUser"]?.jsonPrimitive?.booleanOrNull ?: true
        if (!includeInvisible && !isVisible) return null

        // Check search text
        if (searchText != null) {
            val text = node["text"]?.jsonPrimitive?.contentOrNull ?: ""
            val contentDesc = node["contentDescription"]?.jsonPrimitive?.contentOrNull ?: ""
            if (!text.contains(searchText, ignoreCase = true) &&
                !contentDesc.contains(searchText, ignoreCase = true)) {
                // Check children for text match
                val children = node["children"]?.jsonArray
                val hasMatchingChild = children?.any { child ->
                    containsText(child, searchText)
                } ?: false
                if (!hasMatchingChild) return null
            }
        }

        // Filter based on mode
        val isInteractive = when (mode) {
            "interactive" -> {
                val isClickable = node["isClickable"]?.jsonPrimitive?.booleanOrNull ?: false
                val isEditable = node["isEditable"]?.jsonPrimitive?.booleanOrNull ?: false
                val isCheckable = node["isCheckable"]?.jsonPrimitive?.booleanOrNull ?: false
                isClickable || isEditable || isCheckable
            }
            "summary" -> {
                val isClickable = node["isClickable"]?.jsonPrimitive?.booleanOrNull ?: false
                val isEditable = node["isEditable"]?.jsonPrimitive?.booleanOrNull ?: false
                val text = node["text"]?.jsonPrimitive?.contentOrNull ?: ""
                val contentDesc = node["contentDescription"]?.jsonPrimitive?.contentOrNull ?: ""
                isClickable || isEditable || text.isNotEmpty() || contentDesc.isNotEmpty()
            }
            else -> true // "full" mode includes everything
        }

        // Process children
        val children = node["children"]?.jsonArray
        val filteredChildren = children?.mapNotNull { child ->
            filterHierarchy(child, mode, maxDepth, includeInvisible, searchText, currentDepth + 1)
        }?.let { JsonArray(it) }

        // Include node if it's interactive OR has interactive children
        if (!isInteractive && (filteredChildren == null || filteredChildren.isEmpty())) {
            return null
        }

        // Build filtered node
        return buildJsonObject {
            node.forEach { (key, value) ->
                if (key == "children") {
                    if (filteredChildren != null && filteredChildren.isNotEmpty()) {
                        put(key, filteredChildren)
                    }
                } else if (mode == "summary" && key in listOf("depth", "actions")) {
                    // Skip verbose fields in summary mode
                } else {
                    put(key, value)
                }
            }
        }
    }

    private fun containsText(node: JsonElement, searchText: String): Boolean {
        if (node !is JsonObject) return false

        val text = node["text"]?.jsonPrimitive?.contentOrNull ?: ""
        val contentDesc = node["contentDescription"]?.jsonPrimitive?.contentOrNull ?: ""

        if (text.contains(searchText, ignoreCase = true) ||
            contentDesc.contains(searchText, ignoreCase = true)) {
            return true
        }

        val children = node["children"]?.jsonArray
        return children?.any { containsText(it, searchText) } ?: false
    }

    private suspend fun inputGesture(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val gestureType = command.params?.get("gestureType")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'gestureType' parameter")

        val duration = command.params?.get("duration")?.jsonPrimitive?.longOrNull ?: 300L

        return when (gestureType.uppercase()) {
            AsterAccessibilityService.GESTURE_TAP -> {
                // Try to get coordinates from points array first, then fall back to direct x,y
                val x: Float
                val y: Float

                val pointsJson = command.params?.get("points")
                if (pointsJson != null && pointsJson is JsonArray && pointsJson.isNotEmpty()) {
                    val firstPoint = pointsJson[0]
                    if (firstPoint is JsonObject) {
                        x = firstPoint["x"]?.jsonPrimitive?.floatOrNull
                            ?: return CommandResult.failure("Missing 'x' coordinate in points array")
                        y = firstPoint["y"]?.jsonPrimitive?.floatOrNull
                            ?: return CommandResult.failure("Missing 'y' coordinate in points array")
                    } else {
                        return CommandResult.failure("Invalid point format in points array")
                    }
                } else {
                    // Fallback to direct x,y parameters
                    x = command.params?.get("x")?.jsonPrimitive?.floatOrNull
                        ?: return CommandResult.failure("Missing 'x' coordinate")
                    y = command.params?.get("y")?.jsonPrimitive?.floatOrNull
                        ?: return CommandResult.failure("Missing 'y' coordinate")
                }

                val success = service.performTap(x, y, duration.coerceAtLeast(50L))
                if (success) {
                    val (w, h, density) = service.screenMetrics()
                    CommandResult.success(buildJsonObject {
                        put("gestureType", "TAP")
                        put("x", x)
                        put("y", y)
                        put("screen", buildJsonObject {
                            put("width", w); put("height", h); put("density", density)
                        })
                    })
                } else {
                    CommandResult.failure("Failed to perform tap gesture")
                }
            }

            AsterAccessibilityService.GESTURE_LONG_PRESS -> {
                // Try to get coordinates from points array first, then fall back to direct x,y
                val x: Float
                val y: Float

                val pointsJson = command.params?.get("points")
                if (pointsJson != null && pointsJson is JsonArray && pointsJson.isNotEmpty()) {
                    val firstPoint = pointsJson[0]
                    if (firstPoint is JsonObject) {
                        x = firstPoint["x"]?.jsonPrimitive?.floatOrNull
                            ?: return CommandResult.failure("Missing 'x' coordinate in points array")
                        y = firstPoint["y"]?.jsonPrimitive?.floatOrNull
                            ?: return CommandResult.failure("Missing 'y' coordinate in points array")
                    } else {
                        return CommandResult.failure("Invalid point format in points array")
                    }
                } else {
                    // Fallback to direct x,y parameters
                    x = command.params?.get("x")?.jsonPrimitive?.floatOrNull
                        ?: return CommandResult.failure("Missing 'x' coordinate")
                    y = command.params?.get("y")?.jsonPrimitive?.floatOrNull
                        ?: return CommandResult.failure("Missing 'y' coordinate")
                }

                val success = service.performLongPress(x, y, duration.coerceAtLeast(500L))
                if (success) {
                    val (w, h, density) = service.screenMetrics()
                    CommandResult.success(buildJsonObject {
                        put("gestureType", "LONG_PRESS")
                        put("x", x)
                        put("y", y)
                        put("screen", buildJsonObject {
                            put("width", w); put("height", h); put("density", density)
                        })
                    })
                } else {
                    CommandResult.failure("Failed to perform long press gesture")
                }
            }

            AsterAccessibilityService.GESTURE_SWIPE -> {
                // Try to get coordinates from points array first, then fall back to direct coordinates
                val startX: Float
                val startY: Float
                val endX: Float
                val endY: Float

                val pointsJson = command.params?.get("points")
                if (pointsJson != null && pointsJson is JsonArray && pointsJson.size >= 2) {
                    val startPoint = pointsJson[0]
                    val endPoint = pointsJson[1]

                    if (startPoint is JsonObject && endPoint is JsonObject) {
                        startX = startPoint["x"]?.jsonPrimitive?.floatOrNull
                            ?: return CommandResult.failure("Missing 'x' coordinate in start point")
                        startY = startPoint["y"]?.jsonPrimitive?.floatOrNull
                            ?: return CommandResult.failure("Missing 'y' coordinate in start point")
                        endX = endPoint["x"]?.jsonPrimitive?.floatOrNull
                            ?: return CommandResult.failure("Missing 'x' coordinate in end point")
                        endY = endPoint["y"]?.jsonPrimitive?.floatOrNull
                            ?: return CommandResult.failure("Missing 'y' coordinate in end point")
                    } else {
                        return CommandResult.failure("Invalid point format in points array")
                    }
                } else {
                    // Fallback to direct coordinates
                    startX = command.params?.get("startX")?.jsonPrimitive?.floatOrNull
                        ?: return CommandResult.failure("Missing 'startX' coordinate")
                    startY = command.params?.get("startY")?.jsonPrimitive?.floatOrNull
                        ?: return CommandResult.failure("Missing 'startY' coordinate")
                    endX = command.params?.get("endX")?.jsonPrimitive?.floatOrNull
                        ?: return CommandResult.failure("Missing 'endX' coordinate")
                    endY = command.params?.get("endY")?.jsonPrimitive?.floatOrNull
                        ?: return CommandResult.failure("Missing 'endY' coordinate")
                }

                val success = service.performSwipe(startX, startY, endX, endY, duration)
                if (success) {
                    val (w, h, density) = service.screenMetrics()
                    CommandResult.success(buildJsonObject {
                        put("gestureType", "SWIPE")
                        put("startX", startX)
                        put("startY", startY)
                        put("endX", endX)
                        put("endY", endY)
                        put("screen", buildJsonObject {
                            put("width", w); put("height", h); put("density", density)
                        })
                    })
                } else {
                    CommandResult.failure("Failed to perform swipe gesture")
                }
            }

            "PINCH" -> {
                val cx = command.params?.get("cx")?.jsonPrimitive?.floatOrNull
                    ?: return CommandResult.failure("Missing 'cx' (pinch center x)")
                val cy = command.params?.get("cy")?.jsonPrimitive?.floatOrNull
                    ?: return CommandResult.failure("Missing 'cy' (pinch center y)")
                val startGap = command.params?.get("startGap")?.jsonPrimitive?.floatOrNull
                    ?: return CommandResult.failure("Missing 'startGap'")
                val endGap = command.params?.get("endGap")?.jsonPrimitive?.floatOrNull
                    ?: return CommandResult.failure("Missing 'endGap'")
                val success = service.pinchGesture(cx, cy, startGap, endGap, duration)
                if (success) {
                    val (w, h, density) = service.screenMetrics()
                    CommandResult.success(buildJsonObject {
                        put("gestureType", "PINCH")
                        put("cx", cx); put("cy", cy)
                        put("startGap", startGap); put("endGap", endGap)
                        put("screen", buildJsonObject {
                            put("width", w); put("height", h); put("density", density)
                        })
                    })
                } else {
                    CommandResult.failure("Failed to perform pinch gesture")
                }
            }

            "DRAG" -> {
                val startX = command.params?.get("startX")?.jsonPrimitive?.floatOrNull
                    ?: return CommandResult.failure("Missing 'startX'")
                val startY = command.params?.get("startY")?.jsonPrimitive?.floatOrNull
                    ?: return CommandResult.failure("Missing 'startY'")
                val endX = command.params?.get("endX")?.jsonPrimitive?.floatOrNull
                    ?: return CommandResult.failure("Missing 'endX'")
                val endY = command.params?.get("endY")?.jsonPrimitive?.floatOrNull
                    ?: return CommandResult.failure("Missing 'endY'")
                val success = service.dragGesture(startX, startY, endX, endY, duration.coerceAtLeast(300L))
                if (success) {
                    val (w, h, density) = service.screenMetrics()
                    CommandResult.success(buildJsonObject {
                        put("gestureType", "DRAG")
                        put("startX", startX); put("startY", startY)
                        put("endX", endX); put("endY", endY)
                        put("screen", buildJsonObject {
                            put("width", w); put("height", h); put("density", density)
                        })
                    })
                } else {
                    CommandResult.failure("Failed to perform drag gesture")
                }
            }

            else -> {
                // Try to handle points-based gesture
                val pointsJson = command.params?.get("points")
                if (pointsJson != null && pointsJson is JsonArray) {
                    val points = pointsJson.mapNotNull { point ->
                        if (point is JsonObject) {
                            val x = point["x"]?.jsonPrimitive?.floatOrNull
                            val y = point["y"]?.jsonPrimitive?.floatOrNull
                            if (x != null && y != null) Pair(x, y) else null
                        } else null
                    }

                    if (points.isEmpty()) {
                        return CommandResult.failure("Invalid points array")
                    }

                    val success = service.performGestureWithPoints(points, duration)
                    if (success) {
                        CommandResult.success(buildJsonObject {
                            put("gestureType", gestureType)
                            put("pointCount", points.size)
                        })
                    } else {
                        CommandResult.failure("Failed to perform gesture")
                    }
                } else {
                    CommandResult.failure("Unknown gesture type: $gestureType")
                }
            }
        }
    }

    // `suspend` because ENTER/SEARCH route through the input router (see
    // AsterAccessibilityService.performImeAction). The cascade stops here: this is
    // private and its only caller is [handle], which is already suspend.
    private suspend fun globalAction(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val action = command.params?.get("action")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'action' parameter")

        val success = service.performGlobalActionByName(action)
        return if (success) {
            CommandResult.success(buildJsonObject {
                put("action", action)
                put("performed", true)
            })
        } else {
            CommandResult.failure("Failed to perform global action: $action")
        }
    }

    private fun inputText(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val text = command.params?.get("text")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'text' parameter")

        val append = command.params?.get("append")?.jsonPrimitive?.booleanOrNull ?: false
        val clear = command.params?.get("clear")?.jsonPrimitive?.booleanOrNull ?: false

        val success = when {
            clear -> service.clearText()
            append -> service.appendText(text)
            else -> service.inputText(text)
        }

        return if (success) {
            CommandResult.success(buildJsonObject {
                put("text", text)
                put("inputted", true)
            })
        } else {
            CommandResult.failure("Failed to input text. Ensure a text field is focused.")
        }
    }

    private suspend fun takeScreenshot(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val annotate = command.params?.get("annotate")?.jsonPrimitive?.booleanOrNull ?: false
        val supplied: List<Mark> =
            if (annotate) parseMarks(command.params?.get("marks")) else emptyList()
        // DERIVE the marks from the caller's own observation when it didn't send
        // any. The kernel's `screen_screenshot` never sent a `marks` array, and an
        // empty array means "nothing to draw" — so `annotated` was permanently
        // false and the annotated-screenshot path was dead on both sides despite
        // being fully implemented on each. The companion already holds exactly the
        // data needed (the SnapshotCache the refs came from), so the caller
        // shouldn't have to round-trip it back.
        val marks: List<Mark> = when {
            !annotate -> emptyList()
            supplied.isNotEmpty() -> supplied
            else -> derivedMarks(service, command.params?.get("from_snapshot"))
        }

        val result = service.takeScreenshot(annotate = annotate, marks = marks)
        return if (result != null) {
            CommandResult.success(buildJsonObject {
                put("filePath", result.file.absolutePath)
                put("format", "jpeg")
                put("sizeKB", result.file.length() / 1024)
                put("scale", result.scale)
                put("width", result.realW)
                put("height", result.realH)
                put("annotated", annotate && marks.isNotEmpty())
            })
        } else {
            CommandResult.failure("Failed to take screenshot. Requires Android 11+ with accessibility screenshot permission.")
        }
    }

    /**
     * Build Set-of-Marks boxes from a cached observation.
     *
     * `from_snapshot` names which one; absent means the latest. The mark INDEX is
     * parsed out of the `e<N>` ref so the number drawn on the screenshot is the
     * same number the agent already knows the element by — a numbered box the
     * model cannot map back to a ref would be decoration.
     *
     * Fail-soft throughout: an evicted snapshot, or one with no parseable refs,
     * yields no marks and the screenshot comes back un-annotated rather than
     * failing. Annotation is an aid, never a precondition.
     */
    private fun derivedMarks(
        service: AsterAccessibilityService,
        fromSnapshot: JsonElement?,
    ): List<Mark> {
        val requested = (fromSnapshot as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }
        val snapshotId = requested ?: service.snapshotCache.latestId() ?: return emptyList()
        return service.snapshotCache.descriptors(snapshotId)
            .mapNotNull { (ref, d) ->
                val index = ref.removePrefix("e").toIntOrNull() ?: return@mapNotNull null
                val b = d.bounds
                // A zero-area box draws nothing and only adds a floating number.
                if (b.w <= 0 || b.h <= 0) return@mapNotNull null
                Mark(index = index, bounds = Rect(b.x, b.y, b.x + b.w, b.y + b.h))
            }
            .sortedBy { it.index }
    }

    /**
     * Parse a real-px marks array `[{ "index": 7, "x":, "y":, "w":, "h": }, …]` into [Mark]s.
     * Each mark's bounds are REAL screen pixels (the service multiplies by `scale` before drawing).
     * Malformed entries are skipped (fail-soft — annotation is best-effort overlay).
     */
    private fun parseMarks(element: JsonElement?): List<Mark> {
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { entry ->
            val obj = entry as? JsonObject ?: return@mapNotNull null
            val index = obj["index"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val x = obj["x"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val y = obj["y"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val w = obj["w"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val h = obj["h"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            Mark(index = index, bounds = Rect(x, y, x + w, y + h))
        }
    }

    private fun findElement(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val text = command.params?.get("text")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'text' parameter")
        val exact = command.params?.get("exact")?.jsonPrimitive?.booleanOrNull ?: false

        val hierarchy = service.getScreenHierarchy()
        val elements = findElementsInHierarchy(hierarchy, text, exact)

        return CommandResult.success(buildJsonObject {
            put("query", text)
            put("exact", exact)
            put("count", elements.size)
            put("elements", JsonArray(elements))
        })
    }

    private fun findElementsInHierarchy(
        node: JsonElement,
        searchText: String,
        exact: Boolean
    ): List<JsonElement> {
        if (node !is JsonObject) return emptyList()

        val results = mutableListOf<JsonElement>()

        val text = node["text"]?.jsonPrimitive?.contentOrNull ?: ""
        val contentDesc = node["contentDescription"]?.jsonPrimitive?.contentOrNull ?: ""

        val matches = if (exact) {
            text.equals(searchText, ignoreCase = true) ||
            contentDesc.equals(searchText, ignoreCase = true)
        } else {
            text.contains(searchText, ignoreCase = true) ||
            contentDesc.contains(searchText, ignoreCase = true)
        }

        if (matches) {
            results.add(node)
        }

        // Search children
        val children = node["children"]?.jsonArray
        children?.forEach { child ->
            results.addAll(findElementsInHierarchy(child, searchText, exact))
        }

        return results
    }

    private suspend fun clickByText(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val text = command.params?.get("text")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'text' parameter")

        val success = service.clickByText(text)
        return if (success) {
            CommandResult.success(buildJsonObject {
                put("clickedText", text)
            })
        } else {
            // Help the AI debug: check if the element exists at all
            val hierarchy = service.getScreenHierarchy()
            val found = findElementsInHierarchy(hierarchy, text, exact = false)
            val hint = if (found.isEmpty()) {
                "No element with text '$text' found on screen. Try take_screenshot or get_screen_hierarchy to see what's visible, or use input_gesture to tap by coordinates."
            } else {
                "Found ${found.size} element(s) containing '$text' but none were clickable. Try input_gesture with tap coordinates instead, or use find_element to locate the element and get its bounds."
            }
            CommandResult.failure(hint)
        }
    }

    private fun clickByViewId(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val viewId = command.params?.get("viewId")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'viewId' parameter")

        val success = service.clickByViewId(viewId)
        return if (success) {
            CommandResult.success(buildJsonObject {
                put("clickedViewId", viewId)
            })
        } else {
            CommandResult.failure("Could not find or click element with viewId: $viewId")
        }
    }

    /**
     * Ref-addressed scroll (SPEC §3.2). Accepts direction|amount|ref|untilText and routes to
     * [AsterAccessibilityService.scrollRef]; the legacy `{direction}` shape still works
     * (amount/ref/untilText all optional → single page scroll on the auto-picked scrollable).
     */
    private suspend fun scroll(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val direction = command.params?.get("direction")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'direction' parameter")
        val amount = command.params?.get("amount")?.jsonPrimitive?.contentOrNull
        val ref = command.params?.get("ref")?.jsonPrimitive?.contentOrNull
        val untilText = command.params?.get("untilText")?.jsonPrimitive?.contentOrNull

        // P3 verify-after-act: snapshot revision + foreground BEFORE scrolling (SPEC §3.4).
        val pre = preAct(service, command)

        val (scrolled, found) = service.scrollRef(
            ref, snapshotIdOf(command), direction, amount, untilText,
            timeoutMs = command.params?.get("timeout")?.jsonPrimitive?.longOrNull
                ?: AsterAccessibilityService.SCROLL_FIND_TIMEOUT_MS,
        )
        return if (scrolled || found == true) {
            // Scroll auto-resolves its container (no single ResolvedBy strategy), so resolvedBy
            // is null; settleAndVerify still adds changed/foreground_after/settled. The
            // scroll-specific fields ride along as `extra`.
            settleAndVerify(
                service, command, pre,
                resolvedBy = null, actOk = true,
                extra = buildJsonObject {
                    put("direction", direction)
                    put("scrolled", scrolled)
                    amount?.let { put("amount", it) }
                    ref?.let { put("ref", it) }
                    if (untilText != null) put("found", found ?: false)
                }
            )
        } else if (untilText != null && found == false) {
            CommandResult.failure("Scrolled to the end without finding '$untilText'.")
        } else {
            CommandResult.failure("Failed to scroll. No scrollable element found.")
        }
    }

    // ------------------------------------------------------------------
    // P2 — ref-addressed action handlers (SPEC §3.2)
    // ------------------------------------------------------------------

    private fun snapshotIdOf(command: Command): String? =
        command.params?.get("snapshot_id")?.jsonPrimitive?.contentOrNull

    /**
     * Whether this call opts in to the blind coordinate fallback (`center_tap`).
     *
     * Default **false**. That branch is reached only after every verified strategy
     * has failed, so taking it means acting on stale coordinates while reporting
     * `{ok:true}` — which made `tap`/`long_press` structurally incapable of
     * returning `stale_ref`. Off by default, an unresolvable ref becomes an honest
     * failure the kernel can escalate; a caller that genuinely wants coordinates
     * should use `input_gesture` or opt in explicitly.
     */
    private fun coordinateFallbackOf(command: Command): Boolean =
        command.params?.get("allow_coordinate_fallback")?.jsonPrimitive?.booleanOrNull ?: false

    private suspend fun tap(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        // P3 verify-after-act: snapshot revision + foreground BEFORE acting (SPEC §3.4).
        val pre = preAct(service, command)

        val ref = command.params?.get("ref")?.jsonPrimitive?.contentOrNull
        if (ref != null) {
            val resolvedBy = service.tapRef(ref, snapshotIdOf(command), coordinateFallbackOf(command))
                ?: return ScreenActionResult.staleRef(ref, snapshotIdOf(command))
            return settleAndVerify(
                service, command, pre,
                resolvedBy = resolvedBy.wire, actOk = true,
                extra = buildJsonObject { put("ref", ref) }
            )
        }
        // Coordinate fallback (vision path): {x,y}.
        val x = command.params?.get("x")?.jsonPrimitive?.floatOrNull
            ?: return CommandResult.failure("Provide 'ref' or both 'x' and 'y'")
        val y = command.params?.get("y")?.jsonPrimitive?.floatOrNull
            ?: return CommandResult.failure("Provide 'ref' or both 'x' and 'y'")
        val ok = service.performTap(x, y, 100L)
        return if (ok) {
            settleAndVerify(
                service, command, pre,
                resolvedBy = ScreenActionResult.ResolvedBy.CENTER_TAP.wire, actOk = true,
                extra = buildJsonObject { put("x", x); put("y", y) }
            )
        } else {
            CommandResult.failure("Failed to perform tap at ($x,$y)")
        }
    }

    private suspend fun longPress(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val duration = command.params?.get("duration")?.jsonPrimitive?.longOrNull ?: 600L
        // P3 verify-after-act: snapshot revision + foreground BEFORE acting (SPEC §3.4).
        val pre = preAct(service, command)

        val ref = command.params?.get("ref")?.jsonPrimitive?.contentOrNull
        if (ref != null) {
            val resolvedBy = service.longPressRef(
                ref, snapshotIdOf(command), duration, coordinateFallbackOf(command),
            )
                ?: return ScreenActionResult.staleRef(ref, snapshotIdOf(command))
            return settleAndVerify(
                service, command, pre,
                resolvedBy = resolvedBy.wire, actOk = true,
                extra = buildJsonObject { put("ref", ref) }
            )
        }
        val x = command.params?.get("x")?.jsonPrimitive?.floatOrNull
            ?: return CommandResult.failure("Provide 'ref' or both 'x' and 'y'")
        val y = command.params?.get("y")?.jsonPrimitive?.floatOrNull
            ?: return CommandResult.failure("Provide 'ref' or both 'x' and 'y'")
        val ok = service.performLongPress(x, y, duration.coerceAtLeast(500L))
        return if (ok) {
            settleAndVerify(
                service, command, pre,
                resolvedBy = ScreenActionResult.ResolvedBy.CENTER_TAP.wire, actOk = true,
                extra = buildJsonObject { put("x", x); put("y", y) }
            )
        } else {
            CommandResult.failure("Failed to perform long press at ($x,$y)")
        }
    }

    private suspend fun setText(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val ref = command.params?.get("ref")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'ref' parameter")
        val text = command.params?.get("text")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'text' parameter")
        val mode = command.params?.get("mode")?.jsonPrimitive?.contentOrNull ?: "replace"
        val submit = command.params?.get("submit")?.jsonPrimitive?.booleanOrNull ?: false
        // P3 verify-after-act: snapshot revision + foreground BEFORE acting (SPEC §3.4).
        val pre = preAct(service, command)
        val resolvedBy = service.setTextRef(ref, snapshotIdOf(command), text, mode, submit)
            ?: return ScreenActionResult.staleRef(ref, snapshotIdOf(command))
        return settleAndVerify(
            service, command, pre,
            resolvedBy = resolvedBy.wire, actOk = true,
            extra = buildJsonObject { put("ref", ref); put("mode", mode); put("submit", submit) }
        )
    }

    private suspend fun setToggle(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val ref = command.params?.get("ref")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'ref' parameter")
        val on = command.params?.get("on")?.jsonPrimitive?.booleanOrNull
            ?: return CommandResult.failure("Missing 'on' (boolean) parameter")
        // P3 verify-after-act: snapshot revision + foreground BEFORE acting (SPEC §3.4).
        val pre = preAct(service, command)
        val result = service.setToggleRef(ref, snapshotIdOf(command), on)
            ?: return ScreenActionResult.staleRef(ref, snapshotIdOf(command))
        val (resolvedBy, alreadyInState) = result
        return settleAndVerify(
            service, command, pre,
            resolvedBy = resolvedBy.wire, actOk = true,
            extra = buildJsonObject { put("ref", ref); put("on", on); put("already_in_state", alreadyInState) }
        )
    }

    private suspend fun perform(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val ref = command.params?.get("ref")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'ref' parameter")
        val action = command.params?.get("action")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'action' parameter")
        // `set_text` is in the closed actions[] vocabulary (SPEC §7.2) and `observe` emits it,
        // but `perform` cannot carry the required text argument. Reject honestly here — the ref
        // is fine; the action just isn't performable via perform. NOT a stale_ref (which would
        // misleadingly tell the agent to re-observe), NOT a silent null.
        if (action.equals("set_text", ignoreCase = true)) {
            return CommandResult.failure(
                "'set_text' is not performable via perform — use the set_text action, which carries the text argument"
            )
        }
        // P3 verify-after-act: snapshot revision + foreground BEFORE acting (SPEC §3.4).
        val pre = preAct(service, command)
        val resolvedBy = service.performActionOnRef(ref, snapshotIdOf(command), action)
            ?: return ScreenActionResult.staleRef(ref, snapshotIdOf(command))
        return settleAndVerify(
            service, command, pre,
            resolvedBy = resolvedBy.wire, actOk = true,
            extra = buildJsonObject { put("ref", ref); put("action", action) }
        )
    }

    /**
     * `press_key` — routed through the input router, and reported honestly.
     *
     * The failure message is the router's REAL reason. It used to be the fixed
     * string "unknown key or input keyevent failed", which named two causes and
     * was wrong about both: the shell path it described (`input keyevent`) needs
     * the signature-level `INJECT_EVENTS`, so it failed for EVERY key — including
     * keys that were perfectly well known. An agent reading that message would
     * retry with a different key name forever.
     *
     * Deliberately does NOT add an action name: `supportedActions()` is unchanged,
     * which is what keeps every entry keyed through
     * [com.aster.service.safety.GuardedCommandHandler] and leaves `press_key`
     * inside `PackagePolicyGuard.GATED_ACTIONS`.
     */
    private suspend fun pressKey(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val key = command.params?.get("key")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'key' parameter")
        val result = service.pressKey(key)
        return if (result.ok) {
            CommandResult.success(buildJsonObject {
                put("ok", true)
                put("key", key)
                // Which mechanism actually delivered it. A caller that cannot see
                // this cannot tell a real capability from a lucky path.
                put("backend", result.backend?.name?.lowercase())
            })
        } else {
            CommandResult.failure("Failed to press key '$key': ${result.reason ?: "no reason reported"}")
        }
    }

    // ------------------------------------------------------------------
    // P3 — synchronization handlers (SPEC §3.3)
    // ------------------------------------------------------------------

    private suspend fun waitForIdle(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val quietMs = command.params?.get("quietMs")?.jsonPrimitive?.longOrNull ?: 500L
        val timeout = command.params?.get("timeout")?.jsonPrimitive?.longOrNull ?: 5_000L

        val start = System.currentTimeMillis()
        val idle = service.waitForIdle(quietMs = quietMs, timeoutMs = timeout)
        val waited = System.currentTimeMillis() - start

        return CommandResult.success(buildJsonObject {
            put("idle", idle)
            put("waited_ms", waited)
        })
    }

    private suspend fun waitFor(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val text = command.params?.get("text")?.jsonPrimitive?.contentOrNull
        val viewId = command.params?.get("viewId")?.jsonPrimitive?.contentOrNull
        val role = command.params?.get("role")?.jsonPrimitive?.contentOrNull
        val gone = command.params?.get("gone")?.jsonPrimitive?.booleanOrNull ?: false
        val timeout = command.params?.get("timeout")?.jsonPrimitive?.longOrNull ?: 5_000L

        if (text == null && viewId == null && role == null) {
            return CommandResult.failure(
                "wait_for requires at least one target: 'text', 'viewId', or 'role'"
            )
        }

        val matched = service.waitFor(
            text = text,
            viewId = viewId,
            role = role,
            gone = gone,
            timeoutMs = timeout,
            observe = { observeElements(service) }
        )

        return CommandResult.success(buildJsonObject {
            // `ok` mirrors `matched` so a TIMEOUT reads as a failure through the
            // kernel's universal `result_failed` predicate (which inspects `ok` /
            // `code`). Without it a timed-out wait was a *successful call reporting
            // matched:false*, so every `verify.wait_for` post-condition — the
            // engine's strongest "did this actually work" assertion — passed on
            // timeout, and every template wait step silently advanced.
            // A `gone:true` wait succeeds when the element is absent, which
            // `matched` already encodes, so the mirror is correct for both senses.
            put("ok", matched)
            put("matched", matched)
            put("gone", gone)
            // Real JSON null when there is no foreground package (the kotlinx put(String, String?)
            // overload writes JsonNull), consistent with settleAndVerify — NOT the literal "null"
            // string JsonNull.toString() used to emit.
            put("foreground_after", service.foregroundPackage())
        })
    }

    /**
     * Returns the current observe-shaped element array for wait_for matching.
     *
     * Backed by the synchronous a11y-only `observeA11y()` builder (SPEC §3.1) so wait_for matches
     * against the same indexed element model the rest of the loop uses, WITHOUT pulling P4's
     * OCR/screenshot coroutine into the tight event-nudged wait loop. ScreenWaitMatcher reads only
     * text/viewId/role — all a11y-element fields; OCR `o<idx>` pseudo-elements carry no viewId/role
     * and only a "text" role, so omitting them does not change wait_for semantics.
     */
    private fun observeElements(service: AsterAccessibilityService): JsonArray {
        val observation = service.observeA11y(
            mode = "actionable",
            searchText = null,
            maxElements = com.aster.service.accessibility.ElementFilter.MAX_ELEMENTS_DEFAULT,
        )
        // observe() returns a JsonObject with an `elements` array, or `{error}` when there is
        // no active window — in the latter case treat it as "nothing on screen yet".
        val obj = observation as? JsonObject ?: return JsonArray(emptyList())
        return obj["elements"]?.jsonArray ?: JsonArray(emptyList())
    }

    // ------------------------------------------------------------------
    // P3 — auto-settle + verify-after-act (SPEC §3.3 / §3.4)
    // ------------------------------------------------------------------

    /**
     * Whether a ref action should auto-settle (SPEC §3.3 auto-settle, default true).
     * Read once per action from params.
     */
    private fun shouldSettle(command: Command): Boolean =
        command.params?.get("settle")?.jsonPrimitive?.booleanOrNull ?: true

    /**
     * Auto-settle + verify-after-act (SPEC §3.3 / §3.4). Called by every ref action handler
     * AFTER the act. When [settle] is on, waits for quiescence, then derives `changed` from the
     * ScreenSyncTracker revision delta OR a foreground-package change, and reports
     * `foreground_after`. `changed` is HONEST: false when nothing moved (no fake-success).
     *
     * Single source of truth for the SPEC {ok, resolved_by, changed, foreground_after, settled}
     * ref-action result shape — do NOT duplicate this shape elsewhere.
     *
     * @param pre           [preAct] captured BEFORE the act.
     * @param resolvedBy    which re-resolution strategy resolved the ref (the 4-strategy wire
     *                      value from ScreenActionResult.ResolvedBy.wire); pass "" / null if a
     *                      coordinate action with no ref resolution.
     * @param actOk         whether the underlying act returned success.
     * @param extra         optional extra fields the specific action wants merged into the result.
     */
    /**
     * The window the command's `ref` lives in, from the descriptor it was
     * observed with — `null` for a coordinate action with no ref.
     *
     * Used to dimension the `changed` signal: an element's OWN window is the only
     * surface whose churn is evidence that acting on it did something.
     */
    private fun actedWindowId(service: AsterAccessibilityService, command: Command): Int? {
        val ref = command.params?.get("ref")?.jsonPrimitive?.contentOrNull ?: return null
        val snapshotId = snapshotIdOf(command) ?: service.snapshotCache.latestId() ?: return null
        return service.snapshotCache.get(snapshotId, ref)?.windowId
    }

    /**
     * The three pre-act samples `changed` is derived from. One value so a handler
     * cannot capture two of the three and silently weaken the signal.
     */
    data class PreAct(
        val revision: Long,
        val foreground: String?,
        val windows: Long,
        val globalRevision: Long,
    )

    /** Sample [PreAct] immediately BEFORE acting. */
    private fun preAct(service: AsterAccessibilityService, command: Command) = PreAct(
        revision = service.screenRevision(actedWindowId(service, command)),
        foreground = service.foregroundPackage(),
        windows = service.windowsFingerprint(),
        globalRevision = service.globalRevision(),
    )

    suspend fun settleAndVerify(
        service: AsterAccessibilityService,
        command: Command,
        pre: PreAct,
        resolvedBy: String?,
        actOk: Boolean,
        extra: JsonObject = JsonObject(emptyMap())
    ): CommandResult {
        val settle = shouldSettle(command)
        var settled = true
        if (settle) {
            // Post-act settle: react-then-quiesce (see waitForSettle). Plain
            // quiescence answered "was the screen already quiet?", which right after
            // a tap is normally YES — so it returned at t≈0 and everything below
            // sampled a screen the action had not touched yet.
            //
            // The timeout is a CEILING that only bites on screens which never idle
            // (feeds with autoplay video, live maps): there quiescence is never
            // observable and the whole budget is spent on EVERY action — the
            // dominant cause of an automation that "gets stuck". Keep it short: a
            // tap's effect lands well within it, and `changed`/`foreground_after`
            // below are read regardless of whether true quiescence was reached.
            settled = service.waitForSettle(
                preGlobalRevision = pre.globalRevision,
                reactionMs = SETTLE_REACTION_MS,
                quietMs = SETTLE_QUIET_MS,
                timeoutMs = SETTLE_TIMEOUT_MS,
            )
        }
        val postRevision = service.screenRevision(actedWindowId(service, command))
        val foregroundAfter = service.foregroundPackage()
        // Three independent witnesses, OR'd — each catches what the others cannot:
        //   revision  — content churn inside the SAME window (a toggle flipped, a
        //               row expanded), the only evidence when nothing navigates;
        //   windows   — the window set changed: navigation, a dialog, the IME. The
        //               acted window is often DESTROYED here, which is precisely
        //               why the revision alone reads false on a successful tap;
        //   foreground— we left the app entirely.
        val changed = (postRevision != pre.revision) ||
            (service.windowsFingerprint() != pre.windows) ||
            (foregroundAfter != pre.foreground)

        val body = buildJsonObject {
            put("ok", actOk)
            if (resolvedBy != null) put("resolved_by", resolvedBy)
            put("changed", changed)
            if (foregroundAfter != null) put("foreground_after", foregroundAfter)
            if (settle) put("settled", settled)
            extra.forEach { (k, v) -> put(k, v) }
        }
        return if (actOk) CommandResult.success(body) else CommandResult(success = false, data = body, error = "Action failed")
    }
}
