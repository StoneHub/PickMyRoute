package com.stonecode.pickmyroute.ui.map

import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.model.LatLng
import com.stonecode.pickmyroute.domain.model.PlacePrediction
import com.stonecode.pickmyroute.domain.model.Route
import com.stonecode.pickmyroute.domain.model.Waypoint

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
    val showDestinationInput: Boolean = false,
    val cameraAnimationTarget: CameraUpdate? = null,
    // Search functionality
    val searchQuery: String = "",
    val searchPredictions: List<PlacePrediction> = emptyList(),
    val isSearching: Boolean = false,
    val isSearchBarExpanded: Boolean = false,
    val searchError: String? = null,
    // Navigation mode
    val isNavigating: Boolean = false,
    val deviceBearing: Float = 0f,      // Phone's compass heading (0-360 degrees)
    val cameraTilt: Float = 0f           // Camera tilt angle (0 = top-down, 45 = angled)
)

/**
 * Events that can be triggered from the map screen
 */
sealed class MapEvent {
    data class MapTapped(val location: LatLng) : MapEvent()
    data class SetDestination(val location: LatLng) : MapEvent()
    data class AddWaypoint(val location: LatLng) : MapEvent()
    data class RemoveWaypoint(val waypointId: String) : MapEvent()
    data class UndoRemoveWaypoint(val waypoint: Waypoint) : MapEvent()
    data class ReorderWaypoints(val waypoints: List<Waypoint>) : MapEvent()
    data object ClearRoute : MapEvent()
    data object RequestLocationPermission : MapEvent()
    data object ToggleDestinationInput : MapEvent()
    data object DismissError : MapEvent()
    data class AnimateToLocation(val location: LatLng) : MapEvent()
    data class ResetCompass(val location: LatLng, val zoom: Float) : MapEvent()
    // Search events
    data class SearchQueryChanged(val query: String) : MapEvent()
    data class SearchResultSelected(val placeId: String) : MapEvent()
    data object ExpandSearchBar : MapEvent()
    data object CollapseSearchBar : MapEvent()
    data object ClearSearch : MapEvent()
    // Navigation events
    data object StartNavigation : MapEvent()
    data object StopNavigation : MapEvent()
    data class UpdateDeviceBearing(val bearing: Float) : MapEvent()
}
