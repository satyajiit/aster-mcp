package com.aster.service.handlers

import android.content.Context
import com.aster.data.model.Command
import com.aster.data.model.PhotoMetadata
import com.aster.data.model.SearchFilters
import com.aster.data.model.LocationFilter
import com.aster.data.model.SortOption
import com.aster.service.CommandHandler
import com.aster.service.CommandResult
import com.aster.service.GeocodingService
import com.aster.util.MediaMetadataExtractor
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * Highly optimized storage analysis handler.
 * Provides comprehensive storage breakdown and large file detection.
 */
class StorageHandler(
    private val context: Context? = null
) : CommandHandler {

    private val geocodingService by lazy {
        context?.let { GeocodingService(it) }
    }

    override fun supportedActions() = listOf(
        "analyze_storage",
        "find_large_files",
        "index_media_metadata",
        "search_media"
    )

    override suspend fun handle(command: Command): CommandResult {
        return when (command.action) {
            "analyze_storage" -> analyzeStorage(command)
            "find_large_files" -> findLargeFiles(command)
            "index_media_metadata" -> indexMediaMetadata(command)
            "search_media" -> searchMedia(command)
            else -> CommandResult.failure("Unknown action: ${command.action}")
        }
    }

    /**
     * Comprehensive storage analysis with optimized recursive traversal.
     */
    private fun analyzeStorage(command: Command): CommandResult {
        val path = command.params?.get("path")?.jsonPrimitive?.contentOrNull ?: "/sdcard"
        val maxDepth = command.params?.get("maxDepth")?.jsonPrimitive?.intOrNull ?: 3
        val minSizeMB = command.params?.get("minSizeMB")?.jsonPrimitive?.doubleOrNull ?: 0.0
        val includeHidden = command.params?.get("includeHidden")?.jsonPrimitive?.booleanOrNull ?: false

        val rootDir = File(path)
        if (!rootDir.exists() || !rootDir.isDirectory) {
            return CommandResult.failure("Path does not exist or is not a directory: $path")
        }

        try {
            // Optimized data structures for analysis
            val directoryStats = mutableListOf<DirectoryInfo>()
            val fileTypeStats = HashMap<String, FileTypeInfo>()
            val largeFiles = mutableListOf<FileInfo>()
            var totalSize = 0L

            // Recursive analysis with depth limiting
            fun analyzeDirectory(dir: File, currentDepth: Int): Long {
                if (currentDepth > maxDepth) return 0L

                var dirSize = 0L
                val files = dir.listFiles() ?: return 0L

                for (file in files) {
                    // Skip hidden files if requested
                    if (!includeHidden && file.isHidden) continue

                    try {
                        if (file.isDirectory) {
                            val subDirSize = analyzeDirectory(file, currentDepth + 1)
                            dirSize += subDirSize

                            // Track directory if above minimum size
                            val sizeMB = subDirSize / (1024.0 * 1024.0)
                            if (sizeMB >= minSizeMB) {
                                directoryStats.add(
                                    DirectoryInfo(
                                        path = file.absolutePath,
                                        size = subDirSize,
                                        fileCount = countFiles(file)
                                    )
                                )
                            }
                        } else if (file.isFile) {
                            val fileSize = file.length()
                            dirSize += fileSize

                            // Track file type statistics
                            val extension = file.extension.lowercase().ifEmpty { "no_extension" }
                            val typeInfo = fileTypeStats.getOrPut(extension) {
                                FileTypeInfo(extension, 0, 0L)
                            }
                            typeInfo.count++
                            typeInfo.size += fileSize

                            // Track large files (> 10MB)
                            val sizeMB = fileSize / (1024.0 * 1024.0)
                            if (sizeMB >= 10.0) {
                                largeFiles.add(
                                    FileInfo(
                                        path = file.absolutePath,
                                        name = file.name,
                                        size = fileSize,
                                        type = extension,
                                        lastModified = file.lastModified(),
                                        canDelete = file.canWrite()
                                    )
                                )
                            }
                        }
                    } catch (e: SecurityException) {
                        // Skip files we don't have permission to read
                        continue
                    }
                }

                return dirSize
            }

            // Perform analysis
            totalSize = analyzeDirectory(rootDir, 0)

            // Sort and limit results
            directoryStats.sortByDescending { it.size }
            val topDirectories = directoryStats.take(20)

            largeFiles.sortByDescending { it.size }
            val topLargeFiles = largeFiles.take(50)

            // Calculate percentages
            val totalSizeMB = totalSize / (1024.0 * 1024.0)

            // Build file type breakdown with percentages
            val fileTypeBreakdown = fileTypeStats.values
                .sortedByDescending { it.size }
                .take(15)
                .map { info ->
                    buildJsonObject {
                        put("type", info.extension)
                        put("count", info.count)
                        put("size", info.size / (1024.0 * 1024.0))
                        put("percentage", if (totalSize > 0) (info.size * 100.0 / totalSize) else 0.0)
                    }
                }

            // Build directory breakdown with percentages
            val directoryBreakdown = topDirectories.map { info ->
                buildJsonObject {
                    put("path", info.path)
                    put("size", info.size / (1024.0 * 1024.0))
                    put("fileCount", info.fileCount)
                    put("percentage", if (totalSize > 0) (info.size * 100.0 / totalSize) else 0.0)
                }
            }

            // Build large files list
            val largeFilesList = topLargeFiles.map { info ->
                buildJsonObject {
                    put("path", info.path)
                    put("name", info.name)
                    put("size", info.size / (1024.0 * 1024.0))
                    put("type", info.type)
                    put("lastModified", formatDate(info.lastModified))
                    put("canDelete", info.canDelete)
                }
            }

            // Build final result
            return CommandResult.success(buildJsonObject {
                put("path", path)
                put("totalSize", totalSizeMB)
                put("fileCount", countFiles(rootDir))
                put("breakdown", buildJsonObject {
                    put("byDirectory", JsonArray(directoryBreakdown))
                    put("byFileType", JsonArray(fileTypeBreakdown))
                    put("largeFiles", JsonArray(largeFilesList))
                })
            })
        } catch (e: Exception) {
            return CommandResult.failure("Storage analysis failed: ${e.message}")
        }
    }

    /**
     * Optimized large file finder with filtering.
     */
    private fun findLargeFiles(command: Command): CommandResult {
        val minSizeMB = command.params?.get("minSizeMB")?.jsonPrimitive?.doubleOrNull
            ?: return CommandResult.failure("Missing 'minSizeMB' parameter")
        val path = command.params?.get("path")?.jsonPrimitive?.contentOrNull ?: "/sdcard"
        val fileTypesJson = command.params?.get("fileTypes")?.jsonArray
        val fileTypes = fileTypesJson?.mapNotNull { it.jsonPrimitive.contentOrNull }?.toSet()
        val limit = command.params?.get("limit")?.jsonPrimitive?.intOrNull ?: 50

        val rootDir = File(path)
        if (!rootDir.exists() || !rootDir.isDirectory) {
            return CommandResult.failure("Path does not exist or is not a directory: $path")
        }

        val minSizeBytes = (minSizeMB * 1024 * 1024).toLong()
        val largeFiles = mutableListOf<FileInfo>()
        var totalSize = 0L

        try {
            // Optimized recursive search
            fun searchDirectory(dir: File) {
                val files = dir.listFiles() ?: return

                for (file in files) {
                    try {
                        if (file.isDirectory) {
                            searchDirectory(file)
                        } else if (file.isFile && file.length() >= minSizeBytes) {
                            val extension = file.extension.lowercase()

                            // Apply file type filter if specified
                            if (fileTypes == null || extension in fileTypes) {
                                largeFiles.add(
                                    FileInfo(
                                        path = file.absolutePath,
                                        name = file.name,
                                        size = file.length(),
                                        type = extension.ifEmpty { "no_extension" },
                                        lastModified = file.lastModified(),
                                        canDelete = file.canWrite()
                                    )
                                )
                                totalSize += file.length()
                            }
                        }
                    } catch (e: SecurityException) {
                        // Skip files we don't have permission to read
                        continue
                    }
                }
            }

            searchDirectory(rootDir)

            // Sort by size descending and apply limit
            largeFiles.sortByDescending { it.size }
            val results = largeFiles.take(limit)

            // Build result
            val filesList = results.map { info ->
                buildJsonObject {
                    put("path", info.path)
                    put("name", info.name)
                    put("size", info.size / (1024.0 * 1024.0))
                    put("type", info.type)
                    put("lastModified", formatDate(info.lastModified))
                    put("canDelete", info.canDelete)
                }
            }

            return CommandResult.success(buildJsonObject {
                put("path", path)
                put("minSizeMB", minSizeMB)
                put("totalSize", totalSize / (1024.0 * 1024.0))
                put("count", largeFiles.size)
                put("showing", results.size)
                put("files", JsonArray(filesList))
            })
        } catch (e: Exception) {
            return CommandResult.failure("Large file search failed: ${e.message}")
        }
    }

    /**
     * Index media metadata from photos and videos.
     * Extracts EXIF data, GPS coordinates, camera info, and dimensions.
     */
    private suspend fun indexMediaMetadata(command: Command): CommandResult {
        val path = command.params?.get("path")?.jsonPrimitive?.contentOrNull ?: "/sdcard/DCIM"
        val includeLocation = command.params?.get("includeLocation")?.jsonPrimitive?.booleanOrNull ?: true
        val includeExif = command.params?.get("includeExif")?.jsonPrimitive?.booleanOrNull ?: true
        val limit = command.params?.get("limit")?.jsonPrimitive?.intOrNull ?: 100

        val rootDir = File(path)
        if (!rootDir.exists() || !rootDir.isDirectory) {
            return CommandResult.failure("Path does not exist or is not a directory: $path")
        }

        try {
            val mediaFiles = mutableListOf<PhotoMetadata>()
            var processedCount = 0

            // Supported media extensions
            val mediaExtensions = setOf("jpg", "jpeg", "png", "gif", "webp", "heic", "mp4", "mov", "avi")

            // Recursive scan for media files
            suspend fun scanDirectory(dir: File) {
                if (processedCount >= limit) return

                val files = dir.listFiles() ?: return

                for (file in files) {
                    try {
                        if (processedCount >= limit) break

                        if (file.isDirectory) {
                            scanDirectory(file)
                        } else if (file.isFile) {
                            val extension = file.extension.lowercase()

                            // Check if it's a media file
                            if (extension in mediaExtensions && MediaMetadataExtractor.isSupportedMediaType(file)) {
                                val metadata = MediaMetadataExtractor.extractMetadata(file)

                                if (metadata != null) {
                                    // Perform reverse geocoding if location exists and service available
                                    val enrichedMetadata = if (includeLocation && metadata.location != null) {
                                        val service = geocodingService
                                        if (service != null) {
                                            try {
                                                val enrichedLoc = service.reverseGeocode(
                                                    metadata.location.latitude,
                                                    metadata.location.longitude,
                                                    metadata.location.altitude
                                                )
                                                metadata.copy(location = enrichedLoc ?: metadata.location)
                                            } catch (e: Exception) {
                                                metadata
                                            }
                                        } else {
                                            metadata
                                        }
                                    } else {
                                        metadata
                                    }

                                    // Filter based on parameters
                                    val filteredMetadata = if (!includeLocation || !includeExif) {
                                        enrichedMetadata.copy(
                                            location = if (includeLocation) enrichedMetadata.location else null,
                                            camera = if (includeExif) enrichedMetadata.camera else null
                                        )
                                    } else {
                                        enrichedMetadata
                                    }

                                    mediaFiles.add(filteredMetadata)
                                    processedCount++
                                }
                            }
                        }
                    } catch (e: SecurityException) {
                        // Skip files we don't have permission to read
                        continue
                    }
                }
            }

            // Perform scan
            scanDirectory(rootDir)

            // Serialize metadata using kotlinx.serialization
            val json = Json {
                prettyPrint = false
                encodeDefaults = true
            }
            val mediaArray = mediaFiles.map { metadata ->
                JsonObject(json.encodeToJsonElement(metadata).jsonObject)
            }

            return CommandResult.success(buildJsonObject {
                put("path", path)
                put("count", mediaFiles.size)
                put("media", JsonArray(mediaArray))
            })
        } catch (e: Exception) {
            return CommandResult.failure("Media indexing failed: ${e.message}")
        }
    }

    /**
     * Efficiently count files in a directory recursively.
     */
    private fun countFiles(dir: File): Int {
        var count = 0
        val files = dir.listFiles() ?: return 0

        for (file in files) {
            try {
                if (file.isFile) {
                    count++
                } else if (file.isDirectory) {
                    count += countFiles(file)
                }
            } catch (e: SecurityException) {
                continue
            }
        }

        return count
    }

    /**
     * Format timestamp to readable date string.
     */
    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Search media files with structured filters.
     */
    private suspend fun searchMedia(command: Command): CommandResult {
        val path = command.params?.get("path")?.jsonPrimitive?.contentOrNull ?: "/sdcard/DCIM"
        val params = command.params?.let { JsonObject(it) }
        val filters = parseSearchFilters(params)

        val rootDir = File(path)
        if (!rootDir.exists() || !rootDir.isDirectory) {
            return CommandResult.failure("Path does not exist or is not a directory: $path")
        }

        try {
            val matchingFiles = mutableListOf<PhotoMetadata>()

            suspend fun scanDirectory(dir: File) {
                if (matchingFiles.size >= filters.limit) return

                val files = dir.listFiles() ?: return

                for (file in files) {
                    if (matchingFiles.size >= filters.limit) break

                    try {
                        if (file.isDirectory) {
                            scanDirectory(file)
                        } else if (file.isFile && MediaMetadataExtractor.isSupportedMediaType(file)) {
                            val rawMetadata = MediaMetadataExtractor.extractMetadata(file) ?: continue

                            // Enrich with geocoding BEFORE filtering if:
                            // 1. Location filter is set (city or country), AND
                            // 2. Metadata has GPS coordinates
                            val metadata = if (filters.location != null &&
                                               (filters.location.city != null || filters.location.country != null) &&
                                               rawMetadata.location != null) {
                                val service = geocodingService
                                if (service != null) {
                                    try {
                                        val enrichedLoc = service.reverseGeocode(
                                            rawMetadata.location.latitude,
                                            rawMetadata.location.longitude,
                                            rawMetadata.location.altitude
                                        )
                                        rawMetadata.copy(location = enrichedLoc ?: rawMetadata.location)
                                    } catch (e: Exception) {
                                        rawMetadata
                                    }
                                } else {
                                    rawMetadata
                                }
                            } else {
                                rawMetadata
                            }

                            // Apply filters with enriched metadata
                            if (matchesFilters(metadata, filters, file)) {
                                matchingFiles.add(metadata)
                            }
                        }
                    } catch (e: SecurityException) {
                        continue
                    }
                }
            }

            scanDirectory(rootDir)

            // Sort results
            val sortedFiles = sortMediaFiles(matchingFiles, filters.sortBy)

            // Build result
            val filesJson = sortedFiles.map { metadata ->
                buildJsonObject {
                    put("path", metadata.path)
                    put("name", metadata.name)
                    put("size", metadata.size / (1024.0 * 1024.0))
                    put("mimeType", metadata.mimeType)
                    metadata.dateTaken?.let { put("dateTaken", it) }
                    put("dateModified", formatDate(metadata.dateModified))

                    metadata.location?.let { loc ->
                        put("location", buildJsonObject {
                            put("latitude", loc.latitude)
                            put("longitude", loc.longitude)
                            loc.altitude?.let { put("altitude", it) }
                            loc.city?.let { put("city", it) }
                            loc.country?.let { put("country", it) }
                            loc.address?.let { put("address", it) }
                        })
                    }

                    metadata.camera?.let { cam ->
                        put("camera", buildJsonObject {
                            cam.make?.let { put("make", it) }
                            cam.model?.let { put("model", it) }
                        })
                    }

                    metadata.dimensions?.let { dim ->
                        put("dimensions", buildJsonObject {
                            put("width", dim.width)
                            put("height", dim.height)
                        })
                    }
                }
            }

            return CommandResult.success(buildJsonObject {
                put("path", path)
                put("matched", sortedFiles.size)
                put("files", JsonArray(filesJson))
            })
        } catch (e: Exception) {
            return CommandResult.failure("Media search failed: ${e.message}")
        }
    }

    /**
     * Parse search filters from command parameters.
     */
    private fun parseSearchFilters(params: JsonObject?): SearchFilters {
        if (params == null) return SearchFilters()

        val locationFilter = params["location"]?.jsonObject?.let { loc ->
            LocationFilter(
                city = loc["city"]?.jsonPrimitive?.contentOrNull,
                country = loc["country"]?.jsonPrimitive?.contentOrNull,
                latitude = loc["latitude"]?.jsonPrimitive?.doubleOrNull,
                longitude = loc["longitude"]?.jsonPrimitive?.doubleOrNull,
                radiusKm = loc["radiusKm"]?.jsonPrimitive?.doubleOrNull ?: 50.0
            )
        }

        val fileTypes = params["fileTypes"]?.jsonArray?.mapNotNull {
            it.jsonPrimitive.contentOrNull
        }

        val sortByStr = params["sortBy"]?.jsonPrimitive?.contentOrNull
        val sortBy = when (sortByStr?.uppercase()) {
            "DATE_ASC" -> SortOption.DATE_ASC
            "SIZE_ASC" -> SortOption.SIZE_ASC
            "SIZE_DESC" -> SortOption.SIZE_DESC
            "NAME_ASC" -> SortOption.NAME_ASC
            "NAME_DESC" -> SortOption.NAME_DESC
            else -> SortOption.DATE_DESC
        }

        return SearchFilters(
            dateFrom = params["dateFrom"]?.jsonPrimitive?.contentOrNull,
            dateTo = params["dateTo"]?.jsonPrimitive?.contentOrNull,
            location = locationFilter,
            fileTypes = fileTypes,
            minSizeMB = params["minSizeMB"]?.jsonPrimitive?.doubleOrNull,
            maxSizeMB = params["maxSizeMB"]?.jsonPrimitive?.doubleOrNull,
            cameraModel = params["cameraModel"]?.jsonPrimitive?.contentOrNull,
            sortBy = sortBy,
            limit = params["limit"]?.jsonPrimitive?.intOrNull ?: 100
        )
    }

    /**
     * Check if metadata matches search filters.
     */
    private fun matchesFilters(metadata: PhotoMetadata, filters: SearchFilters, file: File): Boolean {
        // Date filter
        if (filters.dateFrom != null || filters.dateTo != null) {
            val dateTaken = metadata.dateTaken ?: metadata.dateModified

            filters.dateFrom?.let { from ->
                val fromDate = parseIsoDate(from) ?: return false
                if (dateTaken < fromDate) return false
            }

            filters.dateTo?.let { to ->
                val toDate = parseIsoDate(to) ?: return false
                if (dateTaken > toDate) return false
            }
        }

        // Location filter
        filters.location?.let { locFilter ->
            val loc = metadata.location ?: return false

            // City or country match
            locFilter.city?.let { city ->
                if (loc.city?.contains(city, ignoreCase = true) != true) return false
            }

            locFilter.country?.let { country ->
                if (loc.country?.contains(country, ignoreCase = true) != true) return false
            }

            // Radius match
            if (locFilter.latitude != null && locFilter.longitude != null) {
                val distance = calculateDistance(
                    locFilter.latitude, locFilter.longitude,
                    loc.latitude, loc.longitude
                )
                if (distance > locFilter.radiusKm) return false
            }
        }

        // File type filter
        filters.fileTypes?.let { types ->
            val extension = file.extension.lowercase()
            if (!types.contains(extension)) return false
        }

        // Size filter
        val sizeMB = metadata.size / (1024.0 * 1024.0)
        filters.minSizeMB?.let { min ->
            if (sizeMB < min) return false
        }
        filters.maxSizeMB?.let { max ->
            if (sizeMB > max) return false
        }

        // Camera model filter
        filters.cameraModel?.let { model ->
            if (metadata.camera?.model?.contains(model, ignoreCase = true) != true) return false
        }

        return true
    }

    /**
     * Sort media files by specified option.
     */
    private fun sortMediaFiles(files: List<PhotoMetadata>, sortBy: SortOption): List<PhotoMetadata> {
        return when (sortBy) {
            SortOption.DATE_ASC -> files.sortedBy {
                it.dateTaken ?: it.dateModified
            }
            SortOption.DATE_DESC -> files.sortedByDescending {
                it.dateTaken ?: it.dateModified
            }
            SortOption.SIZE_ASC -> files.sortedBy { it.size }
            SortOption.SIZE_DESC -> files.sortedByDescending { it.size }
            SortOption.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
        }
    }

    /**
     * Parse ISO date string to timestamp.
     */
    private fun parseIsoDate(isoDate: String): Long? {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            format.parse(isoDate)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate distance between two GPS coordinates in kilometers.
     * Uses Haversine formula.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // km

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)

        val c = 2 * kotlin.math.atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    // Data classes for internal storage
    private data class DirectoryInfo(
        val path: String,
        val size: Long,
        val fileCount: Int
    )

    private data class FileTypeInfo(
        val extension: String,
        var count: Int,
        var size: Long
    )

    private data class FileInfo(
        val path: String,
        val name: String,
        val size: Long,
        val type: String,
        val lastModified: Long,
        val canDelete: Boolean
    )
}
