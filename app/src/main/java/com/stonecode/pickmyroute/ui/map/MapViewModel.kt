package com.stonecode.pickmyroute.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.stonecode.pickmyroute.domain.model.Waypoint
import com.stonecode.pickmyroute.domain.model.NavigationStep
import com.stonecode.pickmyroute.domain.repository.LocationRepository
import com.stonecode.pickmyroute.domain.repository.PlacesRepository
import com.stonecode.pickmyroute.domain.repository.RoutingRepository
import com.stonecode.pickmyroute.util.PolylineDecoder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.math.abs
import kotlin.math.min

/**
 * ViewModel for the map screen
 * Manages route state, waypoints, search, and interactions with repositories
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
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

    // === Phase 1 Driving Mode: Internal navigation state ===

    /** Flattened list of all steps across all legs for quick indexing */
    private var flattenedSteps: List<StepRef> = emptyList()

    /** Cache of decoded polylines keyed by global step index */
    private val decodedPolylines = mutableMapOf<Int, List<LatLng>>()

    /** Off-route detection strike counters */
    private var offRouteStrike = 0
    private var onRouteStrike = 0

    /** Last emitted distance for throttling */
    private var previousEmittedDistance: Double? = null

    /** Navigation tunables - centralized for easy adjustment */
    private object NavParams {
        const val snapThresholdMeters = 35.0
        const val offRouteEnterMeters = 45.0
        const val offRouteExitMeters = 30.0
        const val offRouteEnterStrikes = 3
        const val offRouteExitStrikes = 2
        const val advanceDistanceMeters = 12.0
        const val maneuverNowThreshold = 15.0
        const val distanceEmissionDelta = 3.0
    }

    /** Internal step reference with cumulative distance tracking */
    private data class StepRef(
        val globalIndex: Int,
        val legIndex: Int,
        val stepIndexInLeg: Int,
        val step: NavigationStep,
        val cumulativeMetersBefore: Int
    )

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
            // Navigation events
            is MapEvent.StartNavigation -> startNavigation()
            is MapEvent.StopNavigation -> stopNavigation()
            is MapEvent.UpdateDeviceBearing -> updateDeviceBearing(event.bearing)
        }
    }

    private fun handleMapTap(location: LatLng) {
        val currentState = _state.value

        // Don't handle map taps when search bar is expanded
        if (currentState.isSearchBarExpanded) {
            return
        }

        // Don't allow route editing during navigation mode
        if (currentState.isNavigating) {
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

                // Flatten steps for navigation (Phase 1)
                flattenedSteps = flattenSteps(route)
                decodedPolylines.clear()

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

        // Phase 1 Driving Mode: Update navigation progress
        if (_state.value.isNavigating && _state.value.route != null && flattenedSteps.isNotEmpty()) {
            updateNavigationProgress(location)
        }

        // Don't automatically move camera in navigation mode - let user pan freely
        // Camera will only recenter when My Location button is explicitly tapped
    }

    private fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun animateToLocation(location: LatLng) {
        Log.d("MapsRoutePicker", "📍 Animating to location: $location")

        // In navigation mode, use a fast 300ms animation (not instant, not slow)
        if (_state.value.isNavigating) {
            val cameraPosition = com.google.android.gms.maps.model.CameraPosition.Builder()
                .target(location)
                .zoom(18f)
                .bearing(_state.value.deviceBearing)
                .tilt(_state.value.cameraTilt)
                .build()

            val cameraUpdate = com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(cameraPosition)
            _state.update { it.copy(cameraAnimationTarget = cameraUpdate) }

            // Clear after a short animation
            viewModelScope.launch {
                kotlinx.coroutines.delay(400)
                _state.update { it.copy(cameraAnimationTarget = null) }
            }
            return
        }

        // In planning mode, animate smoothly
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

    // === Phase 1 Driving Mode: Navigation Helpers ===

    /**
     * Flatten route into a linear list of steps with cumulative distance tracking
     */
    private fun flattenSteps(route: com.stonecode.pickmyroute.domain.model.Route): List<StepRef> {
        val refs = mutableListOf<StepRef>()
        var globalIndex = 0
        var cumulative = 0

        route.legs.forEachIndexed { legIdx, leg ->
            leg.steps.forEachIndexed { stepIdx, step ->
                refs += StepRef(
                    globalIndex = globalIndex++,
                    legIndex = legIdx,
                    stepIndexInLeg = stepIdx,
                    step = step,
                    cumulativeMetersBefore = cumulative
                )
                cumulative += step.distanceMeters
            }
        }

        return refs
    }

    /**
     * Lazy decode and cache polyline for a step
     */
    private fun getDecodedPolyline(globalIndex: Int, step: NavigationStep): List<LatLng> {
        return decodedPolylines.getOrPut(globalIndex) {
            PolylineDecoder.decode(step.polyline)
        }
    }

    /**
     * Calculate haversine distance between two points in meters
     */
    private fun distanceMeters(a: LatLng, b: LatLng): Double {
        val result = FloatArray(1)
        android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result[0].toDouble()
    }

    /**
     * Project point P onto line segment AB
     * Returns: Pair(projectedPoint, distanceFromPtoSegmentMeters)
     */
    private fun projectOntoSegment(p: LatLng, a: LatLng, b: LatLng): Pair<LatLng, Double> {
        // Use lat/lng as approximate x/y (good for short distances)
        val ax = a.latitude
        val ay = a.longitude
        val bx = b.latitude
        val by = b.longitude
        val px = p.latitude
        val py = p.longitude

        val vx = bx - ax
        val vy = by - ay
        val wx = px - ax
        val wy = py - ay

        val c1 = vx * wx + vy * wy
        val c2 = vx * vx + vy * vy
        val t = when {
            c2 == 0.0 -> 0.0
            else -> (c1 / c2).coerceIn(0.0, 1.0)
        }

        val projLat = ax + t * vx
        val projLng = ay + t * vy
        val projected = LatLng(projLat, projLng)
        val dist = distanceMeters(p, projected)
        return projected to dist
    }

    /**
     * Snap device location to a step's polyline
     * Returns: Pair(snappedPoint or null if too far, distance to polyline)
     */
    private fun snapToStep(globalIndex: Int, step: NavigationStep, deviceLocation: LatLng): Pair<LatLng?, Double> {
        val poly = getDecodedPolyline(globalIndex, step)
        if (poly.size < 2) return deviceLocation to Double.MAX_VALUE

        var bestPoint: LatLng? = null
        var bestDistance = Double.MAX_VALUE

        for (i in 0 until poly.lastIndex) {
            val a = poly[i]
            val b = poly[i + 1]
            val (proj, d) = projectOntoSegment(deviceLocation, a, b)
            if (d < bestDistance) {
                bestDistance = d
                bestPoint = proj
            }
        }

        return if (bestDistance <= NavParams.snapThresholdMeters) {
            bestPoint to bestDistance
        } else {
            null to bestDistance
        }
    }

    /**
     * Calculate remaining distance from projected point to end of step
     */
    private fun remainingDistanceForStep(globalIndex: Int, stepRef: StepRef, projectedPoint: LatLng?): Double {
        if (projectedPoint == null) return stepRef.step.distanceMeters.toDouble()

        // Simple approximation: distance from projected point to step end location
        val distFromProjToEnd = distanceMeters(projectedPoint, stepRef.step.endLocation)
        return distFromProjToEnd.coerceAtLeast(0.0)
    }

    /**
     * Check if should emit state update (throttling logic)
     */
    private fun shouldEmit(newStepIdx: Int?, newOffRoute: Boolean, newDistance: Double?): Boolean {
        if (newStepIdx != _state.value.currentStepIndex) return true
        if (newOffRoute != _state.value.isOffRoute) return true
        val prev = previousEmittedDistance
        if (prev == null && newDistance != null) return true
        if (prev != null && newDistance != null && abs(prev - newDistance) >= NavParams.distanceEmissionDelta) return true
        return false
    }

    /**
     * Main navigation progress update - called on each location update during navigation
     */
    private fun updateNavigationProgress(deviceLocation: LatLng) {
        val currentIdx = _state.value.currentStepIndex ?: 0
        if (currentIdx >= flattenedSteps.size) return

        var idx = currentIdx

        // Advancement loop: skip through short/completed steps
        while (idx < flattenedSteps.size) {
            val stepRef = flattenedSteps[idx]
            val (snapped, _) = snapToStep(stepRef.globalIndex, stepRef.step, deviceLocation)
            val remaining = remainingDistanceForStep(stepRef.globalIndex, stepRef, snapped)

            // Check if we should advance to next step
            val distToEnd = distanceMeters(deviceLocation, stepRef.step.endLocation)
            if (remaining < NavParams.advanceDistanceMeters || distToEnd < 15.0) {
                Log.d("NAV_ADV", "Advancing from step $idx to ${idx + 1}")
                idx += 1
                continue
            }
            break
        }

        // Clamp to valid range
        idx = idx.coerceIn(0, flattenedSteps.size - 1)

        // Compute current step metrics
        val currentStepRef = flattenedSteps[idx]
        val (snappedPoint, distToPolyline) = snapToStep(currentStepRef.globalIndex, currentStepRef.step, deviceLocation)
        val remainingDist = remainingDistanceForStep(currentStepRef.globalIndex, currentStepRef, snappedPoint)

        // Off-route detection: check distance to current and next step
        val distToCurrent = distToPolyline
        val distToNext = if (idx + 1 < flattenedSteps.size) {
            val nextStepRef = flattenedSteps[idx + 1]
            snapToStep(nextStepRef.globalIndex, nextStepRef.step, deviceLocation).second
        } else {
            Double.MAX_VALUE
        }

        val minDist = min(distToCurrent, distToNext)

        // Update strike counters
        if (minDist > NavParams.offRouteEnterMeters) {
            offRouteStrike += 1
            onRouteStrike = 0
        } else {
            onRouteStrike += 1
            offRouteStrike = 0
        }

        // Determine off-route state
        val isOffRoute = offRouteStrike >= NavParams.offRouteEnterStrikes
        val shouldClearOffRoute = onRouteStrike >= NavParams.offRouteExitStrikes && minDist < NavParams.offRouteExitMeters

        val finalOffRoute = if (shouldClearOffRoute) false else isOffRoute

        if (finalOffRoute) {
            Log.d("NAV_OFF", "Off route detected: distance=$minDist strikes=$offRouteStrike")
        }

        // Get instruction text
        val instruction = currentStepRef.step.instruction

        // Throttled emission
        if (shouldEmit(idx, finalOffRoute, remainingDist)) {
            Log.d("NAV_STEP", "step=$idx rem=${remainingDist.toInt()} off=$finalOffRoute")

            _state.update { it.copy(
                currentStepIndex = idx,
                distanceToNextManeuverMeters = remainingDist,
                nextInstructionPrimary = instruction,
                isOffRoute = finalOffRoute,
                offRouteDistanceMeters = if (finalOffRoute) minDist else null
            )}

            previousEmittedDistance = remainingDist
        }
    }

    private fun startNavigation() {
        Log.d("MapViewModel", "🚗 Starting navigation mode")

        // Reset navigation state
        offRouteStrike = 0
        onRouteStrike = 0
        previousEmittedDistance = null

        // Enter navigation mode with tilted camera (45 degrees for 3D view)
        _state.update { it.copy(
            isNavigating = true,
            cameraTilt = 45f,
            currentStepIndex = 0,
            distanceToNextManeuverMeters = null,
            nextInstructionPrimary = null,
            isOffRoute = false,
            offRouteDistanceMeters = null
        )}

        // Immediately update camera to navigation view
        _state.value.currentLocation?.let { location ->
            updateNavigationCamera(location, _state.value.deviceBearing)
        }
    }

    private fun stopNavigation() {
        Log.d("MapViewModel", "🛑 Stopping navigation mode")

        // Clear navigation state
        offRouteStrike = 0
        onRouteStrike = 0
        previousEmittedDistance = null

        // Exit navigation mode, reset to flat top-down view
        _state.update { it.copy(
            isNavigating = false,
            cameraTilt = 0f,
            deviceBearing = 0f,
            currentStepIndex = null,
            distanceToNextManeuverMeters = null,
            nextInstructionPrimary = null,
            isOffRoute = false,
            offRouteDistanceMeters = null
        )}

        // Reset camera to north-up flat view
        _state.value.currentLocation?.let { location ->
            val cameraPosition = com.google.android.gms.maps.model.CameraPosition.Builder()
                .target(location)
                .zoom(15f)
                .bearing(0f)
                .tilt(0f)
                .build()

            val cameraUpdate = com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(cameraPosition)
            _state.update { it.copy(cameraAnimationTarget = cameraUpdate) }
        }
    }

    private fun updateDeviceBearing(bearing: Float) {
        // Only update bearing if in navigation mode
        if (!_state.value.isNavigating) return

        // Just update the bearing state, DON'T move the camera
        // Camera will only move when user explicitly taps My Location button
        _state.update { it.copy(deviceBearing = bearing) }
    }

    /**
     * Update camera to follow user location with bearing and tilt for navigation mode
     */
    private fun updateNavigationCamera(location: LatLng, bearing: Float) {
        val cameraPosition = com.google.android.gms.maps.model.CameraPosition.Builder()
            .target(location)
            .zoom(18f) // Closer zoom for navigation
            .bearing(bearing) // Rotate map to match phone orientation
            .tilt(_state.value.cameraTilt) // 45 degree angle for 3D view
            .build()

        val cameraUpdate = com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(cameraPosition)
        _state.update { it.copy(cameraAnimationTarget = cameraUpdate) }
    }
}
