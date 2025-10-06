package com.stonecode.pickmyroute.domain.repository

import com.google.android.gms.maps.model.LatLng
import com.stonecode.pickmyroute.domain.model.Route
import com.stonecode.pickmyroute.domain.model.Waypoint

/**
 * Repository for calculating routes using Google Directions API
 */
interface RoutingRepository {
    /**
     * Calculate route from origin to destination
     * Optionally through specified waypoints
     *
     * @throws Exception if route calculation fails
     */
    suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<Waypoint> = emptyList()
    ): Route
}
