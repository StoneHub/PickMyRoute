package com.stonecode.mapsroutepicker.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.stonecode.mapsroutepicker.domain.model.Waypoint
import com.stonecode.mapsroutepicker.domain.repository.LocationRepository
import com.stonecode.mapsroutepicker.domain.repository.RoutingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the map screen
 * Manages route state, waypoints, and interactions with routing repository
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val routingRepository: RoutingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    init {
        // Location updates will start when permission is granted
    }

    fun onEvent(event: MapEvent) {
        when (event) {
            is MapEvent.MapTapped -> handleMapTap(event.location)
            is MapEvent.SetDestination -> setDestination(event.location)
            is MapEvent.AddWaypoint -> addWaypoint(event.location)
            is MapEvent.RemoveWaypoint -> removeWaypoint(event.waypointId)
            is MapEvent.ReorderWaypoints -> reorderWaypoints(event.waypoints)
            is MapEvent.ClearRoute -> clearRoute()
            is MapEvent.RequestLocationPermission -> requestLocationPermission()
            is MapEvent.ToggleDestinationInput -> toggleDestinationInput()
        }
    }

    private fun handleMapTap(location: LatLng) {
        val currentState = _state.value

        when {
            // If no destination, set it
            currentState.destination == null -> {
                setDestination(location)
            }
            // If destination exists, add as waypoint
            else -> {
                addWaypoint(location)
            }
        }
    }

    private fun setDestination(location: LatLng) {
        _state.update { it.copy(
            destination = location,
            showDestinationInput = false
        )}

        // Calculate route if we have current location
        if (_state.value.currentLocation != null) {
            calculateRoute()
        }
    }

    private fun addWaypoint(location: LatLng) {
        val currentWaypoints = _state.value.waypoints
        val newWaypoint = Waypoint(
            location = location,
            order = currentWaypoints.size + 1,
            isLocked = true
        )

        _state.update { it.copy(
            waypoints = currentWaypoints + newWaypoint
        )}

        // Recalculate route with new waypoint
        calculateRoute()
    }

    private fun removeWaypoint(waypointId: String) {
        val updatedWaypoints = _state.value.waypoints
            .filterNot { it.id == waypointId }
            .mapIndexed { index, waypoint ->
                waypoint.copy(order = index + 1)
            }

        _state.update { it.copy(waypoints = updatedWaypoints) }

        // Recalculate route without removed waypoint
        if (updatedWaypoints.isNotEmpty() || _state.value.destination != null) {
            calculateRoute()
        }
    }

    private fun reorderWaypoints(waypoints: List<Waypoint>) {
        val reorderedWaypoints = waypoints.mapIndexed { index, waypoint ->
            waypoint.copy(order = index + 1)
        }

        _state.update { it.copy(waypoints = reorderedWaypoints) }

        // Recalculate route with new order
        calculateRoute()
    }

    private fun clearRoute() {
        _state.update { it.copy(
            destination = null,
            route = null,
            waypoints = emptyList(),
            error = null
        )}
    }

    private fun calculateRoute() {
        val currentState = _state.value

        // Need at least origin and destination
        if (currentState.currentLocation == null || currentState.destination == null) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val route = routingRepository.getRoute(
                    origin = currentState.currentLocation!!,
                    destination = currentState.destination!!,
                    waypoints = currentState.waypoints
                )
                _state.update { it.copy(route = route, isLoading = false) }

            } catch (e: Exception) {
                _state.update { it.copy(
                    isLoading = false,
                    error = "Failed to calculate route: ${e.message}"
                )}
            }
        }
    }

    private fun requestLocationPermission() {
        _state.update { it.copy(hasLocationPermission = true) }

        // Start location updates when permission is granted
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            // Get current location first
            locationRepository.getCurrentLocation()?.let { location ->
                _state.update { it.copy(currentLocation = location) }
            }

            // Then start continuous updates
            locationRepository.getLocationUpdates().collect { location ->
                onLocationUpdate(location)
            }
        }
    }

    private fun toggleDestinationInput() {
        _state.update { it.copy(
            showDestinationInput = !it.showDestinationInput
        )}
    }

    private fun onLocationUpdate(location: LatLng) {
        _state.update { it.copy(currentLocation = location) }

        // If we have a destination but no route yet, calculate it
        if (_state.value.destination != null && _state.value.route == null) {
            calculateRoute()
        }
    }
}
