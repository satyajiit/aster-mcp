package com.aster.service

import android.content.Context
import android.location.Geocoder
import android.location.Address
import android.os.Build
import com.aster.data.model.LocationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Service for reverse geocoding (coordinates to place names).
 * Uses Android Geocoder API with LRU caching for performance.
 */
class GeocodingService(private val context: Context) {

    companion object {
        private const val MAX_CACHE_SIZE = 500 // Maximum number of cached locations
    }

    private val geocoder = Geocoder(context, Locale.getDefault())

    // LRU cache implementation using LinkedHashMap with access-order
    private val cache = object : LinkedHashMap<Pair<Double, Double>, LocationInfo?>(
        MAX_CACHE_SIZE,
        0.75f,
        true // Access-order for LRU behavior
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Double, Double>, LocationInfo?>?): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }

    /**
     * Convert GPS coordinates to location information with place names.
     * Uses caching to avoid redundant API calls.
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double, altitude: Double? = null): LocationInfo? {
        // Round coordinates to 4 decimal places for cache key (~11m precision)
        val roundedLat = String.format("%.4f", latitude).toDouble()
        val roundedLon = String.format("%.4f", longitude).toDouble()
        val cacheKey = Pair(roundedLat, roundedLon)

        // Check cache first (synchronized for thread safety)
        synchronized(cache) {
            cache[cacheKey]?.let { return it }
        }

        return try {
            val locationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Use modern async API for Android 13+
                getAddressAsync(latitude, longitude, altitude)
            } else {
                // Use legacy blocking API
                getAddressLegacy(latitude, longitude, altitude)
            }

            // Cache the result (even if null)
            synchronized(cache) {
                cache[cacheKey] = locationInfo
            }
            locationInfo
        } catch (e: Exception) {
            // Return location without place names on error
            LocationInfo(
                latitude = latitude,
                longitude = longitude,
                altitude = altitude,
                city = null,
                country = null,
                address = null
            )
        }
    }

    /**
     * Modern async geocoding for Android 13+
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getAddressAsync(
        latitude: Double,
        longitude: Double,
        altitude: Double?
    ): LocationInfo? = suspendCoroutine { continuation ->
        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
            val address = addresses.firstOrNull()
            continuation.resume(address?.let { createLocationInfo(it, latitude, longitude, altitude) })
        }
    }

    /**
     * Legacy blocking geocoding for older Android versions
     */
    private suspend fun getAddressLegacy(
        latitude: Double,
        longitude: Double,
        altitude: Double?
    ): LocationInfo? = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.let { createLocationInfo(it, latitude, longitude, altitude) }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * Create LocationInfo from Geocoder Address
     */
    private fun createLocationInfo(
        address: Address,
        latitude: Double,
        longitude: Double,
        altitude: Double?
    ): LocationInfo {
        return LocationInfo(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            city = address.locality ?: address.subAdminArea,
            country = address.countryName,
            address = address.getAddressLine(0)
        )
    }

    /**
     * Clear geocoding cache
     */
    fun clearCache() {
        synchronized(cache) {
            cache.clear()
        }
    }

    /**
     * Get cache size
     */
    fun getCacheSize(): Int = synchronized(cache) { cache.size }
}
