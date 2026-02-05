package com.aster.data.model

import kotlinx.serialization.Serializable

/**
 * Metadata extracted from photos and videos using EXIF data.
 * Used for intelligent media search and organization.
 */

@Serializable
data class PhotoMetadata(
    val path: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val dateTaken: Long? = null,
    val dateModified: Long,
    val location: LocationInfo? = null,
    val camera: CameraInfo? = null,
    val dimensions: ImageDimensions? = null
)

@Serializable
data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val city: String? = null,
    val country: String? = null,
    val address: String? = null
)

@Serializable
data class CameraInfo(
    val make: String? = null,
    val model: String? = null,
    val orientation: Int? = null,
    val flash: Boolean? = null,
    val focalLength: Double? = null,
    val aperture: Double? = null,
    val iso: Int? = null,
    val exposureTime: Double? = null
)

@Serializable
data class ImageDimensions(
    val width: Int,
    val height: Int
)
