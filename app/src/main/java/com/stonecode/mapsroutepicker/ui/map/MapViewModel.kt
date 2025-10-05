package com.stonecode.mapsroutepicker.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.stonecode.mapsroutepicker.domain.model.Waypoint
import com.stonecode.mapsroutepicker.domain.repository.LocationRepository
import com.stonecode.mapsroutepicker.domain.repository.PlacesRepository
import com.stonecode.mapsroutepicker.domain.repository.RoutingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the map screen
 * Manages route state, waypoints, search, and interactions with repositories
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val routingRepository: RoutingRepository,
    private val placesRepository: PlacesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MapState())
    val state: StateFlow<MapState> = _state.asStateFlow()

    // Separate flow for search query to enable debouncing
    private val searchQueryFlow = MutableStateFlow("")

    init {
        // Location updates will start when permission is granted
        setupSearchDebouncing()
    }

    /**
     * Set up debounced search - waits 300ms after user stops typing
     */
    private fun setupSearchDebouncing() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(300) // Wait 300ms after last keystroke
                .distinctUntilChanged() // Only search if query actually changed
                .filter { it.length >= 2 } // Minimum 2 characters
                .mapLatest { query ->
                    performSearch(query)
                }
                .collect { }
        }
    }

    fun onEvent(event: MapEvent) {
        when (event) {
            is MapEvent.MapTapped -> handleMapTap(event.location)
            is MapEvent.SetDestination -> setDestination(event.location)
            is MapEvent.AddWaypoint -> addWaypoint(event.location)
            is MapEvent.RemoveWaypoint -> removeWaypoint(event.waypointId)
            is MapEvent.UndoRemoveWaypoint -> undoRemoveWaypoint(event.waypoint)
            is MapEvent.ReorderWaypoints -> reorderWaypoints(event.waypoints)
            is MapEvent.ClearRoute -> clearRoute()
            is MapEvent.RequestLocationPermission -> requestLocationPermission()
            is MapEvent.ToggleDestinationInput -> toggleDestinationInput()
            is MapEvent.DismissError -> dismissError()
            is MapEvent.AnimateToLocation -> animateToLocation(event.location)
            is MapEvent.ResetCompass -> resetCompass(event.location, event.zoom)
            // Search events
            is MapEvent.SearchQueryChanged -> handleSearchQueryChanged(event.query)
            is MapEvent.SearchResultSelected -> handleSearchResultSelected(event.placeId)
            is MapEvent.ExpandSearchBar -> expandSearchBar()
            is MapEvent.CollapseSearchBar -> collapseSearchBar()
            is MapEvent.ClearSearch -> clearSearch()
        }
    }

    private fun handleMapTap(location: LatLng) {
        val currentState = _state.value

        // Don't handle map taps when search bar is expanded
        if (currentState.isSearchBarExpanded) {
            return
        }

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

    private fun undoRemoveWaypoint(waypoint: Waypoint) {
        val currentWaypoints = _state.value.waypoints.toMutableList()

        // Insert waypoint back at its original position (or at the end if position is out of bounds)
        val insertIndex = (waypoint.order - 1).coerceIn(0, currentWaypoints.size)
        currentWaypoints.add(insertIndex, waypoint)

        // Reorder all waypoints
        val reorderedWaypoints = currentWaypoints.mapIndexed { index, wp ->
            wp.copy(order = index + 1)
        }

        _state.update { it.copy(waypoints = reorderedWaypoints) }

        // Recalculate route with restored waypoint
        calculateRoute()
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

    // Search functionality

    private fun handleSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query, searchError = null) }

        if (query.isEmpty()) {
            // Clear predictions if query is empty
            _state.update { it.copy(searchPredictions = emptyList(), isSearching = false) }
        } else {
            // Trigger debounced search
            searchQueryFlow.value = query
            _state.update { it.copy(isSearching = true) }
        }
    }

    private suspend fun performSearch(query: String) {
        try {
            val predictions = placesRepository.searchPlaces(
                query = query,
                userLocation = _state.value.currentLocation
            )
            _state.update { it.copy(
                searchPredictions = predictions,
                isSearching = false,
                searchError = null
            )}
        } catch (e: Exception) {
            Log.e("MapViewModel", "Search failed", e)
            _state.update { it.copy(
                searchPredictions = emptyList(),
                isSearching = false,
                searchError = "Search failed: ${e.message}"
            )}
        }
    }

    private fun handleSearchResultSelected(placeId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true) }

            try {
                val location = placesRepository.getPlaceLocation(placeId)

                // Set as destination
                setDestination(location)

                // Collapse search bar and clear search
                _state.update { it.copy(
                    isSearchBarExpanded = false,
                    searchQuery = "",
                    searchPredictions = emptyList(),
                    isSearching = false,
                    searchError = null
                )}

                // Animate camera to selected location
                animateToLocation(location)

            } catch (e: Exception) {
                Log.e("MapViewModel", "Failed to get place location", e)
                _state.update { it.copy(
                    isSearching = false,
                    searchError = "Failed to select place: ${e.message}"
                )}
            }
        }
    }

    private fun expandSearchBar() {
        _state.update { it.copy(isSearchBarExpanded = true) }
    }

    private fun collapseSearchBar() {
        _state.update { it.copy(
            isSearchBarExpanded = false,
            searchQuery = "",
            searchPredictions = emptyList(),
            searchError = null
        )}
    }

    private fun clearSearch() {
        _state.update { it.copy(
            searchQuery = "",
            searchPredictions = emptyList(),
            searchError = null
        )}
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

    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun animateToLocation(location: LatLng) {
        Log.d("MapsRoutePicker", "📍 Animating to location: $location")

        val cameraUpdate = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(location, 16f)
        _state.update { it.copy(cameraAnimationTarget = cameraUpdate) }

        // Clear the target after animation completes
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // Wait for 800ms animation + buffer
            _state.update { it.copy(cameraAnimationTarget = null) }
        }
    }

    private fun resetCompass(location: LatLng, zoom: Float) {
        Log.d("MapsRoutePicker", "🧭 Resetting compass to north at zoom level: $zoom")

        // Reset bearing to 0 (north) and tilt to 0 (flat), but preserve current zoom and position
        // This matches Google Maps official behavior - only rotate the map, don't zoom or move
        val cameraPosition = com.google.android.gms.maps.model.CameraPosition.Builder()
            .target(location) // Keep current camera target
            .zoom(zoom) // Preserve current zoom level
            .bearing(0f) // Reset to north
            .tilt(0f) // Flatten the view
            .build()

        val cameraUpdate = com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(cameraPosition)
        _state.update { it.copy(cameraAnimationTarget = cameraUpdate) }

        // Clear the target after animation completes
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _state.update { it.copy(cameraAnimationTarget = null) }
        }
    }
}
