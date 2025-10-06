package com.stonecode.mapsroutepicker.ui.map

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.stonecode.mapsroutepicker.domain.model.Waypoint
import com.stonecode.mapsroutepicker.ui.permissions.LocationPermissionHandler
import com.stonecode.mapsroutepicker.ui.map.components.WaypointTimeline
import com.stonecode.mapsroutepicker.ui.map.components.MapControlFabs
import com.stonecode.mapsroutepicker.ui.map.components.SwipeableRouteInfoCard
import com.stonecode.mapsroutepicker.ui.map.components.PlaceSearchBar
import com.stonecode.mapsroutepicker.ui.map.components.rememberDeviceBearing
import com.stonecode.mapsroutepicker.ui.map.components.getWaypointColor
import com.stonecode.mapsroutepicker.util.PolylineDecoder
import kotlinx.coroutines.launch

/**
 * Main map screen - displays Google Map with route, waypoints, and controls
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Track removed waypoint for undo
    var removedWaypoint by remember { mutableStateOf<Waypoint?>(null) }
    var showInitialToast by remember { mutableStateOf(true) }

    // Debug: Log API key at runtime
    LaunchedEffect(Unit) {
        val apiKey = com.stonecode.mapsroutepicker.BuildConfig.MAPS_API_KEY
        Log.d("MapsRoutePicker", "🔑 API Key in app: ${apiKey.take(10)}...")
        Log.d("MapsRoutePicker", "📍 Current location: ${state.currentLocation}")
    }

    // Show initial toast when destination is null
    LaunchedEffect(state.destination, showInitialToast) {
        if (state.destination == null && showInitialToast && state.waypoints.isEmpty()) {
            Toast.makeText(context, "👆 Tap anywhere on the map to set your destination", Toast.LENGTH_LONG).show()
            showInitialToast = false
        }
    }

    // Show toast when waypoints are added
    LaunchedEffect(state.waypoints.size) {
        if (state.waypoints.isNotEmpty() && state.waypoints.size > (removedWaypoint?.let { 0 } ?: 0)) {
            Toast.makeText(context, "🛣️ Tap waypoints to remove • Tap map to add more", Toast.LENGTH_SHORT).show()
        }
    }

    LocationPermissionHandler(
        onPermissionGranted = {
            viewModel.onEvent(MapEvent.RequestLocationPermission)
        },
        onPermissionDenied = {
            // Permission not granted, map will still work but without location
        }
    ) { permissionState ->
        if (!permissionState.allPermissionsGranted) {
            // Show permission rationale
            PermissionRationaleContent(
                onRequestPermission = { permissionState.launchMultiplePermissionRequest() }
            )
        } else {
            // Show main map content with snackbar host
            Box(modifier = Modifier.fillMaxSize()) {
                MapContent(
                    state = state,
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onWaypointRemoved = { waypoint ->
                        removedWaypoint = waypoint
                    },
                    onUndoRemoval = { waypoint ->
                        viewModel.onEvent(MapEvent.UndoRemoveWaypoint(waypoint))
                        removedWaypoint = null
                    }
                )

                // Snackbar for undo
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionRationaleContent(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Location Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "This app needs location access to show your current position and provide navigation.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun MapContent(
    state: MapState,
    viewModel: MapViewModel,
    snackbarHostState: SnackbarHostState,
    onWaypointRemoved: (Waypoint) -> Unit,
    onUndoRemoval: (Waypoint) -> Unit
) {
    var cameraPositionState: CameraPositionState? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Track route card height dynamically for better waypoint timeline spacing
    var routeCardHeight by remember { mutableStateOf(0) }
    var searchBarHeight by remember { mutableStateOf(0) }

    // Track device bearing when in navigation mode
    val deviceBearing = rememberDeviceBearing(
        isEnabled = state.isNavigating,
        onBearingChanged = { bearing ->
            viewModel.onEvent(MapEvent.UpdateDeviceBearing(bearing))
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Google Map
        GoogleMapView(
            state = state,
            onMapTapped = { location ->
                if (state.isNavigating) {
                    // Show toast when trying to edit during navigation
                    Toast.makeText(
                        context,
                        "❌ Exit navigation mode to edit route",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                viewModel.onEvent(MapEvent.MapTapped(location))
            },
            onCameraStateReady = { cameraState ->
                cameraPositionState = cameraState
            }
        )

        // Search bar - ONLY show when no destination is set
        if (state.destination == null) {
            PlaceSearchBar(
                searchQuery = state.searchQuery,
                predictions = state.searchPredictions,
                isSearching = state.isSearching,
                isExpanded = state.isSearchBarExpanded,
                onQueryChange = { query ->
                    viewModel.onEvent(MapEvent.SearchQueryChanged(query))
                    // Expand when user starts typing
                    if (query.isNotEmpty() && !state.isSearchBarExpanded) {
                        viewModel.onEvent(MapEvent.ExpandSearchBar)
                    }
                },
                onResultSelected = { placeId ->
                    viewModel.onEvent(MapEvent.SearchResultSelected(placeId))
                },
                onExpandChange = { expanded ->
                    if (expanded) {
                        viewModel.onEvent(MapEvent.ExpandSearchBar)
                    } else {
                        viewModel.onEvent(MapEvent.CollapseSearchBar)
                    }
                },
                onClearSearch = {
                    viewModel.onEvent(MapEvent.ClearSearch)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        searchBarHeight = size.height
                    }
            )
        }

        // Route info card - position based on whether search is visible
        if (state.error == null) {
            state.route?.let { route ->
                val topPadding = if (state.destination == null) 88.dp else 16.dp

                SwipeableRouteInfoCard(
                    route = route,
                    onClose = {
                        viewModel.onEvent(MapEvent.ClearRoute)
                    },
                    onStartNavigation = {
                        viewModel.onEvent(MapEvent.StartNavigation)
                    },
                    onStopNavigation = {
                        viewModel.onEvent(MapEvent.StopNavigation)
                    },
                    isNavigating = state.isNavigating,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(start = 16.dp, top = topPadding, end = 16.dp)
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            routeCardHeight = size.height
                        }
                )
            }
        }

        // Waypoint timeline - programmatically calculate position to avoid overlap
        if (state.waypoints.isNotEmpty()) {
            // Calculate top padding based on actual measured heights
            val topPadding = when {
                state.destination == null && state.route != null -> {
                    // Below search + route card: use measured heights + margins
                    with(LocalDensity.current) {
                        (searchBarHeight + routeCardHeight).toDp() + 24.dp  // 24dp for spacing between cards
                    }
                }
                state.destination == null -> {
                    // Below search bar only
                    with(LocalDensity.current) {
                        searchBarHeight.toDp() + 16.dp
                    }
                }
                state.route != null -> {
                    // Below route card only (no search)
                    with(LocalDensity.current) {
                        routeCardHeight.toDp() + 24.dp  // 24dp spacing between route card and waypoint timeline
                    }
                }
                else -> 16.dp  // Just top padding
            }

            WaypointTimeline(
                waypoints = state.waypoints,
                onRemoveWaypoint = { waypointId ->
                    // Find the waypoint being removed
                    val waypoint = state.waypoints.find { it.id == waypointId }
                    waypoint?.let {
                        onWaypointRemoved(it)
                        viewModel.onEvent(MapEvent.RemoveWaypoint(waypointId))

                        // Show snackbar with undo
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Waypoint removed",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                onUndoRemoval(it)
                            }
                        }
                    }
                },
                isNavigating = state.isNavigating,
                route = state.route,  // Pass route data for distance display
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = topPadding
                    )
            )
        }

        // FAB stack (NO X BUTTON) - only show when camera is ready
        cameraPositionState?.let { cameraState ->
            MapControlFabs(
                cameraPositionState = cameraState,
                onMyLocationClick = {
                    state.currentLocation?.let { location ->
                        viewModel.onEvent(MapEvent.AnimateToLocation(location))
                    }
                },
                onCompassClick = {
                    viewModel.onEvent(MapEvent.ResetCompass(
                        location = cameraState.position.target,
                        zoom = cameraState.position.zoom
                    ))
                },
                isNavigating = state.isNavigating,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp)
            )
        }

        // Loading indicator
        if (state.isLoading) {
            LoadingOverlay()
        }

        // Error message
        state.error?.let { error ->
            ErrorCard(
                error = error,
                onDismiss = { viewModel.onEvent(MapEvent.DismissError) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GoogleMapView(
    state: MapState,
    onMapTapped: (LatLng) -> Unit,
    onCameraStateReady: (CameraPositionState) -> Unit = {}
) {
    // Default camera position (San Francisco)
    val defaultLocation = LatLng(37.7749, -122.4194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            state.currentLocation ?: defaultLocation,
            12f
        )
    }

    // Track if we've done initial zoom to user location
    var hasInitiallyZoomed by remember { mutableStateOf(false) }

    // Debug: Log when map is being composed
    LaunchedEffect(Unit) {
        Log.d("MapsRoutePicker", "🗺️ GoogleMap composable being rendered")
        onCameraStateReady(cameraPositionState)
    }

    // Only animate to current location ONCE on initial load
    LaunchedEffect(state.currentLocation) {
        if (!hasInitiallyZoomed && state.currentLocation != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(state.currentLocation!!, 15f),
                durationMs = 1000
            )
            hasInitiallyZoomed = true
        }
    }

    // Listen for camera animation requests from ViewModel
    LaunchedEffect(state.cameraAnimationTarget, state.isNavigating) {
        state.cameraAnimationTarget?.let { target ->
            if (state.isNavigating) {
                // In navigation mode, use 300ms animation instead of instant move
                cameraPositionState.animate(
                    update = target,
                    durationMs = 300
                )
            } else {
                // In planning mode, animate smoothly
                cameraPositionState.animate(
                    update = target,
                    durationMs = 800
                )
            }
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = state.hasLocationPermission
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = false, // Use custom FAB instead
            zoomControlsEnabled = false,
            compassEnabled = false // Moved to custom controls
        ),
        onMapClick = { location ->
            Log.d("MapsRoutePicker", "🎯 Map tapped at: $location")
            onMapTapped(location)
        },
        onMapLoaded = {
            Log.d("MapsRoutePicker", "✅ Map loaded successfully!")
        }
    ) {
        // Destination marker
        state.destination?.let { destination ->
            Marker(
                state = MarkerState(position = destination),
                title = "Destination",
                snippet = "Your destination",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
        }

        // Waypoint markers with labels (A, B, C...)
        state.waypoints.sortedBy { it.order }.forEachIndexed { index, waypoint ->
            Marker(
                state = MarkerState(position = waypoint.location),
                title = "Waypoint ${('A' + index)}",
                snippet = "Tap to remove",
                icon = BitmapDescriptorFactory.defaultMarker(getMarkerHue(index))
            )
        }

        // Color-coded route polylines for each segment
        state.route?.let { route ->
            if (state.waypoints.isEmpty()) {
                // Simple single-color route when no waypoints
                val polylinePoints = PolylineDecoder.decode(route.overviewPolyline)
                Polyline(
                    points = polylinePoints,
                    color = Color(0xFF4285F4), // Google Blue
                    width = 12f
                )
            } else {
                // Multi-colored route segments matching destination waypoint
                // Leg 0: current location → waypoint A (use waypoint A's color)
                // Leg 1: waypoint A → waypoint B (use waypoint B's color)
                // Last leg: last waypoint → destination (use destination red color)
                route.legs.forEachIndexed { legIndex, leg ->
                    leg.steps.forEach { step ->
                        val segmentPoints = PolylineDecoder.decode(step.polyline)

                        // Color matches the destination of this leg
                        val segmentColor = if (legIndex < state.waypoints.size) {
                            // This leg goes TO a waypoint, use that waypoint's color
                            getWaypointColor(legIndex)
                        } else {
                            // Final leg goes to destination, use red
                            Color(0xFFE53935)
                        }

                        Polyline(
                            points = segmentPoints,
                            color = segmentColor,
                            width = 12f
                        )
                    }
                }
            }
        }
    }
}

/**
 * Convert waypoint color to Google Maps marker hue (0-360)
 */
private fun getMarkerHue(index: Int): Float {
    val hues = listOf(
        0f,    // Red
        210f,  // Blue
        120f,  // Green
        45f,   // Orange/Amber
        270f,  // Purple
        30f,   // Orange
        180f,  // Cyan
        0f,    // Dark Red
        270f,  // Deep Purple
        180f   // Teal
    )
    return hues[index % hues.size]
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Calculating route...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "⚠️ Error",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                // Add helpful hints based on error type
                if (error.contains("API key", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Fix: Run tools/fix_api_key.sh in WSL to create a valid API key",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss error",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}
