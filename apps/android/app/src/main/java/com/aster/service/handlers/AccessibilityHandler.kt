package com.aster.service.handlers

import com.aster.data.model.Command
import com.aster.service.AsterAccessibilityService
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import kotlinx.serialization.json.*

/**
 * Handler for accessibility-based commands.
 * Handles screen hierarchy, gestures, global actions, text input, and screenshots.
 */
class AccessibilityHandler : CommandHandler {

    override fun supportedActions() = listOf(
        "get_screen_hierarchy",
        "input_gesture",
        "global_action",
        "input_text",
        "take_screenshot",
        "find_element",
        "click_by_text",
        "click_by_view_id",
        "scroll"
    )

    override suspend fun handle(command: Command): CommandResult {
        val service = AsterAccessibilityService.getInstance()
            ?: return CommandResult.failure("Accessibility service not enabled. Please enable it in Settings > Accessibility > Aster")

        return when (command.action) {
            "get_screen_hierarchy" -> getScreenHierarchyFiltered(service, command)
            "input_gesture" -> inputGesture(service, command)
            "global_action" -> globalAction(service, command)
            "input_text" -> inputText(service, command)
            "take_screenshot" -> takeScreenshot(service)
            "find_element" -> findElement(service, command)
            "click_by_text" -> clickByText(service, command)
            "click_by_view_id" -> clickByViewId(service, command)
            "scroll" -> scroll(service, command)
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
                    CommandResult.success(buildJsonObject {
                        put("gestureType", "TAP")
                        put("x", x)
                        put("y", y)
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
                    CommandResult.success(buildJsonObject {
                        put("gestureType", "LONG_PRESS")
                        put("x", x)
                        put("y", y)
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
                    CommandResult.success(buildJsonObject {
                        put("gestureType", "SWIPE")
                        put("startX", startX)
                        put("startY", startY)
                        put("endX", endX)
                        put("endY", endY)
                    })
                } else {
                    CommandResult.failure("Failed to perform swipe gesture")
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

    private fun globalAction(
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

    private suspend fun takeScreenshot(service: AsterAccessibilityService): CommandResult {
        val base64 = service.takeScreenshot()
        return if (base64 != null) {
            CommandResult.success(buildJsonObject {
                put("screenshot", base64)
                put("format", "png")
                put("encoding", "base64")
            })
        } else {
            CommandResult.failure("Failed to take screenshot. Requires Android 11+ with accessibility screenshot permission.")
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

    private fun clickByText(
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
            CommandResult.failure("Could not find or click element with text: $text")
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

    private fun scroll(
        service: AsterAccessibilityService,
        command: Command
    ): CommandResult {
        val direction = command.params?.get("direction")?.jsonPrimitive?.contentOrNull
            ?: return CommandResult.failure("Missing 'direction' parameter")

        val success = service.scroll(direction)
        return if (success) {
            CommandResult.success(buildJsonObject {
                put("direction", direction)
                put("scrolled", true)
            })
        } else {
            CommandResult.failure("Failed to scroll. No scrollable element found.")
        }
    }
}
