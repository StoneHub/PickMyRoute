package com.stonecode.mapsroutepicker.ui.map

import com.google.android.gms.maps.model.LatLng
import com.stonecode.mapsroutepicker.domain.model.Route
import com.stonecode.mapsroutepicker.domain.model.Waypoint

/**
 * UI state for the map screen
 */
data class MapState(
    val currentLocation: LatLng? = null,
    val destination: LatLng? = null,
    val route: Route? = null,
    val waypoints: List<Waypoint> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasLocationPermission: Boolean = false,
    val showDestinationInput: Boolean = false
)

/**
 * Events that can be triggered from the map screen
 */
sealed class MapEvent {
    data class MapTapped(val location: LatLng) : MapEvent()
    data class SetDestination(val location: LatLng) : MapEvent()
    data class AddWaypoint(val location: LatLng) : MapEvent()
    data class RemoveWaypoint(val waypointId: String) : MapEvent()
    data class ReorderWaypoints(val waypoints: List<Waypoint>) : MapEvent()
    data object ClearRoute : MapEvent()
    data object RequestLocationPermission : MapEvent()
    data object ToggleDestinationInput : MapEvent()
}
