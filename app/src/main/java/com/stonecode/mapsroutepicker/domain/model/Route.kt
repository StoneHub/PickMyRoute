package com.stonecode.mapsroutepicker.domain.model

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

/**
 * Represents a complete route from origin to destination, potentially through waypoints
 */
data class Route(
    val overviewPolyline: String,           // Encoded polyline for the entire route
    val legs: List<RouteLeg>,
    val bounds: LatLngBounds,               // Geographic bounds for camera positioning
    val totalDurationSeconds: Int,
    val totalDistanceMeters: Int,
    val waypoints: List<Waypoint> = emptyList(),
    val summary: String? = null,            // e.g., "I-90 W"
    val warnings: List<String> = emptyList(),
    val copyrights: String? = null
) {
    /**
     * Formatted distance string (e.g., "5.2 mi" or "8.4 km")
     */
    fun getFormattedDistance(useMetric: Boolean = false): String {
        return if (useMetric) {
            val km = totalDistanceMeters / 1000.0
            String.format("%.1f km", km)
        } else {
            val miles = totalDistanceMeters * 0.000621371
            String.format("%.1f mi", miles)
        }
    }

    /**
     * Formatted duration string (e.g., "1h 23m" or "45m")
     */
    fun getFormattedDuration(): String {
        val hours = totalDurationSeconds / 3600
        val minutes = (totalDurationSeconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}

/**
 * Represents one leg of a route (between two consecutive stops/waypoints)
 */
data class RouteLeg(
    val steps: List<NavigationStep>,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val startLocation: LatLng,
    val endLocation: LatLng,
    val startAddress: String? = null,
    val endAddress: String? = null
)
