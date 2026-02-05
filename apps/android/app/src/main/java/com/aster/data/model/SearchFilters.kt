package com.aster.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SearchFilters(
    val dateFrom: String? = null,      // ISO 8601 format: "2025-02-01"
    val dateTo: String? = null,        // ISO 8601 format: "2025-02-28"
    val location: LocationFilter? = null,
    val fileTypes: List<String>? = null,  // ["jpg", "png", "mp4"]
    val minSizeMB: Double? = null,
    val maxSizeMB: Double? = null,
    val cameraModel: String? = null,
    val sortBy: SortOption = SortOption.DATE_DESC,
    val limit: Int = 100
)

@Serializable
data class LocationFilter(
    val city: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusKm: Double = 50.0  // Default 50km radius
)

enum class SortOption {
    DATE_ASC,
    DATE_DESC,
    SIZE_ASC,
    SIZE_DESC,
    NAME_ASC,
    NAME_DESC
}
