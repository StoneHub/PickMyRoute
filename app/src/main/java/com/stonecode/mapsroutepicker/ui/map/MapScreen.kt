package com.stonecode.mapsroutepicker.ui.map

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.stonecode.mapsroutepicker.ui.permissions.LocationPermissionHandler
import com.stonecode.mapsroutepicker.ui.map.components.WaypointTimeline
import com.stonecode.mapsroutepicker.ui.map.components.MapControlFabs
import com.stonecode.mapsroutepicker.ui.map.components.SwipeableRouteInfoCard
import com.stonecode.mapsroutepicker.ui.map.components.getWaypointColor
import com.stonecode.mapsroutepicker.util.PolylineDecoder

/**
 * Main map screen - displays Google Map with route, waypoints, and controls
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Debug: Log API key at runtime
    LaunchedEffect(Unit) {
        val apiKey = com.stonecode.mapsroutepicker.BuildConfig.MAPS_API_KEY
        Log.d("MapsRoutePicker", "🔑 API Key in app: ${apiKey.take(10)}...")
        Log.d("MapsRoutePicker", "📍 Current location: ${state.currentLocation}")
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
            // Show main map content with system bar padding
            Box(modifier = Modifier.fillMaxSize()) {
                MapContent(state = state, viewModel = viewModel)
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
    viewModel: MapViewModel
) {
    var showInitialHint by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Google Map
        GoogleMapView(
            state = state,
            onMapTapped = { location ->
                viewModel.onEvent(MapEvent.MapTapped(location))
                showInitialHint = false // Dismiss hint after first tap
            }
        )

        // Top waypoint timeline - only show when waypoints exist
        if (state.waypoints.isNotEmpty()) {
            WaypointTimeline(
                waypoints = state.waypoints,
                onRemoveWaypoint = { waypointId ->
                    viewModel.onEvent(MapEvent.RemoveWaypoint(waypointId))
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            )
        }

        // Initial hint - dismissible, reappears if context changes
        if (state.error == null && showInitialHint && state.destination == null && state.waypoints.isEmpty()) {
            DismissibleInitialHint(
                onDismiss = { showInitialHint = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }

        // Bottom-right FAB stack (My Location + Compass + Close button)
        MapControlFabs(
            onMyLocationClick = {
                // Animate camera to current location
                state.currentLocation?.let { location ->
                    viewModel.onEvent(MapEvent.AnimateToLocation(location))
                }
            },
            onCloseClick = {
                viewModel.onEvent(MapEvent.ClearRoute)
                showInitialHint = true // Show hint again after clearing
            },
            showCloseButton = state.destination != null || state.waypoints.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp)
                .padding(bottom = if (state.route != null && state.error == null) 80.dp else 0.dp) // Extra padding when route card is visible
        )

        // Loading indicator with message
        if (state.isLoading) {
            LoadingOverlay()
        }

        // Error message - improved dismissible card
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

        // Route info card (bottom) - only show when no error - with swipe-to-reveal close
        if (state.error == null) {
            state.route?.let { route ->
                SwipeableRouteInfoCard(
                    route = route,
                    onClose = {
                        viewModel.onEvent(MapEvent.ClearRoute)
                        showInitialHint = true
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart) // Changed from BottomCenter to BottomStart for swipe
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun GoogleMapView(
    state: MapState,
    onMapTapped: (LatLng) -> Unit
) {
    // Default camera position (San Francisco)
    val defaultLocation = LatLng(37.7749, -122.4194)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            state.currentLocation ?: defaultLocation,
            12f
        )
    }

    // Debug: Log when map is being composed
    LaunchedEffect(Unit) {
        Log.d("MapsRoutePicker", "🗺️ GoogleMap composable being rendered")
    }

    // Animate to current location when it changes and map is loaded
    LaunchedEffect(state.currentLocation) {
        state.currentLocation?.let { location ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(location, 15f),
                durationMs = 1000
            )
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
                // Multi-colored route segments for each waypoint
                route.legs.forEachIndexed { index, leg ->
                    leg.steps.forEach { step ->
                        val segmentPoints = PolylineDecoder.decode(step.polyline)
                        val segmentColor = getWaypointColor(index)
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
private fun DismissibleInitialHint(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "👆 Tap anywhere on the map to set your destination",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Text(
                    text = "×",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
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
