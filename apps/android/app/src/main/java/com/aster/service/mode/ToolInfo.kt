package com.aster.service.mode

/**
 * Metadata about an available tool/action for display in dashboards.
 */
data class ToolInfo(
    val name: String,
    val displayName: String = name,
    val description: String,
    val category: String
)
