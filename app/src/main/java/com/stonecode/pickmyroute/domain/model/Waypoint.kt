package com.stonecode.pickmyroute.domain.model

import com.google.android.gms.maps.model.LatLng
import java.util.UUID

/**
 * Represents a waypoint (intermediate stop) on a route.
 * User-selected waypoints force the route to pass through specific roads.
 */
data class Waypoint(
    val id: String = UUID.randomUUID().toString(),
    val location: LatLng,
    val roadName: String? = null,
    val isLocked: Boolean = true,  // User-selected waypoints are locked (vs auto-generated)
    val order: Int
) {
    /**
     * Converts to "via:" format for Google Directions API
     * The "via:" prefix means route through this point without treating it as a stop
     */
    fun toDirectionsApiFormat(): String {
        return "via:${location.latitude},${location.longitude}"
    }
}
