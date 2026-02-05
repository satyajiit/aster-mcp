package com.aster.util

import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import com.aster.data.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object MediaMetadataExtractor {

    /**
     * Extract comprehensive metadata from image file.
     */
    fun extractMetadata(file: File): PhotoMetadata? {
        if (!file.exists() || !file.isFile) return null

        return try {
            val mimeType = getMimeType(file) ?: "application/octet-stream"

            // Only extract EXIF from images
            if (!mimeType.startsWith("image/")) {
                return createBasicMetadata(file, mimeType)
            }

            val exif = ExifInterface(file.absolutePath)

            PhotoMetadata(
                path = file.absolutePath,
                name = file.name,
                size = file.length(),
                mimeType = mimeType,
                dateTaken = parseExifDate(
                    exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                        ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                ),
                dateModified = file.lastModified(),
                location = extractLocationInfo(exif),
                camera = extractCameraInfo(exif),
                dimensions = extractDimensions(exif)
            )
        } catch (e: Exception) {
            // If EXIF extraction fails, return basic metadata
            createBasicMetadata(file, getMimeType(file) ?: "application/octet-stream")
        }
    }

    /**
     * Create basic metadata without EXIF data.
     */
    private fun createBasicMetadata(file: File, mimeType: String): PhotoMetadata {
        return PhotoMetadata(
            path = file.absolutePath,
            name = file.name,
            size = file.length(),
            mimeType = mimeType,
            dateTaken = null,
            dateModified = file.lastModified(),
            location = null,
            camera = null,
            dimensions = null
        )
    }

    /**
     * Extract GPS location information from EXIF.
     */
    private fun extractLocationInfo(exif: ExifInterface): LocationInfo? {
        val latLong = exif.latLong ?: return null

        val altitude = exif.getAltitude(Double.NaN)

        return LocationInfo(
            latitude = latLong[0],
            longitude = latLong[1],
            altitude = if (altitude.isNaN()) null else altitude,
            city = null, // Will be populated by geocoding in Phase 2
            country = null,
            address = null
        )
    }

    /**
     * Extract camera information from EXIF.
     */
    private fun extractCameraInfo(exif: ExifInterface): CameraInfo? {
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)

        // Only return camera info if we have at least make or model
        if (make == null && model == null) return null

        return CameraInfo(
            make = make,
            model = model,
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL),
            flash = exif.getAttribute(ExifInterface.TAG_FLASH)?.let { it != "0" },
            focalLength = parseDoubleFromExif(exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)),
            aperture = parseDoubleFromExif(exif.getAttribute(ExifInterface.TAG_APERTURE_VALUE)),
            iso = parseIntFromExif(
                exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                    ?: exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
            ),
            exposureTime = parseDoubleFromExif(exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME))
        )
    }

    /**
     * Extract image dimensions from EXIF.
     */
    private fun extractDimensions(exif: ExifInterface): ImageDimensions? {
        val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

        return if (width > 0 && height > 0) {
            ImageDimensions(width, height)
        } else {
            null
        }
    }

    /**
     * Get MIME type from file extension.
     */
    private fun getMimeType(file: File): String? {
        val extension = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    /**
     * Parse EXIF date string to timestamp.
     * Creates a new SimpleDateFormat instance for thread safety.
     */
    fun parseExifDate(exifDate: String?): Long? {
        if (exifDate == null) return null

        return try {
            SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(exifDate)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse EXIF string value to Double.
     * Handles fraction format (e.g., "50/1" for focal length).
     */
    private fun parseDoubleFromExif(value: String?): Double? {
        if (value == null) return null

        return try {
            // Handle fraction format (e.g., "50/1")
            if (value.contains("/")) {
                val parts = value.split("/")
                if (parts.size == 2) {
                    val numerator = parts[0].toDouble()
                    val denominator = parts[1].toDouble()
                    if (denominator != 0.0) {
                        numerator / denominator
                    } else {
                        null
                    }
                } else {
                    null
                }
            } else {
                value.toDouble()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse EXIF string value to Int.
     */
    private fun parseIntFromExif(value: String?): Int? {
        if (value == null) return null

        return try {
            value.toInt()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if file is a supported media type.
     */
    fun isSupportedMediaType(file: File): Boolean {
        val mimeType = getMimeType(file) ?: return false
        return mimeType.startsWith("image/") || mimeType.startsWith("video/")
    }
}
