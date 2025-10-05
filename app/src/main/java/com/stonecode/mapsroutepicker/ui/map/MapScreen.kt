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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.stonecode.mapsroutepicker.ui.permissions.LocationPermissionHandler
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Google Map
        GoogleMapView(
            state = state,
            onMapTapped = { location ->
                viewModel.onEvent(MapEvent.MapTapped(location))
            }
        )

        // Top controls and waypoint timeline - WITH SYSTEM BAR PADDING
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()  // Push below status bar
                .padding(16.dp)
        ) {
            // Destination button - only show when no destination OR when explicitly toggled
            if (state.destination == null) {
                Button(
                    onClick = { viewModel.onEvent(MapEvent.MapTapped(state.currentLocation ?: LatLng(0.0, 0.0))) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false // Disabled - user should tap map instead
                ) {
                    Text("Tap map to set destination")
                }
            } else {
                // Show "Change Destination" button when destination exists
                OutlinedButton(
                    onClick = { viewModel.onEvent(MapEvent.ClearRoute) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Route & Start Over")
                }
            }

            // Waypoint timeline (if waypoints exist)
            if (state.waypoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                WaypointTimeline(
                    waypoints = state.waypoints,
                    onRemoveWaypoint = { waypointId ->
                        viewModel.onEvent(MapEvent.RemoveWaypoint(waypointId))
                    }
                )
            }
        }

        // User guidance hints - floating at center-top - WITH SYSTEM BAR PADDING
        if (state.error == null) {
            UserGuidanceHint(
                state = state,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()  // Push below status bar
                    .padding(top = 80.dp)
            )
        }

        // Loading indicator with message
        if (state.isLoading) {
            LoadingOverlay()
        }

        // Error message - improved dismissible card - WITH NAVIGATION BAR PADDING
        state.error?.let { error ->
            ErrorCard(
                error = error,
                onDismiss = { viewModel.onEvent(MapEvent.DismissError) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()  // Push above navigation bar
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }

        // Route info card (bottom) - only show when no error - WITH NAVIGATION BAR PADDING
        if (state.error == null) {
            state.route?.let { route ->
                RouteInfoCard(
                    route = route,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()  // Push above navigation bar
                        .padding(16.dp)
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

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = state.hasLocationPermission
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = true,
            zoomControlsEnabled = false,
            compassEnabled = true
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
                snippet = "Your destination"
            )
        }

        // Waypoint markers
        state.waypoints.forEach { waypoint ->
            Marker(
                state = MarkerState(position = waypoint.location),
                title = waypoint.roadName ?: "Waypoint ${waypoint.order}",
                snippet = "Tap to remove"
            )
        }

        // Route polyline
        state.route?.let { route ->
            val polylinePoints = PolylineDecoder.decode(route.overviewPolyline)
            Polyline(
                points = polylinePoints,
                color = Color(0xFF4285F4), // Google Blue
                width = 12f
            )
        }
    }
}

@Composable
fun WaypointTimeline(
    waypoints: List<com.stonecode.mapsroutepicker.domain.model.Waypoint>,
    onRemoveWaypoint: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏁", style = MaterialTheme.typography.bodySmall)

            waypoints.sortedBy { it.order }.forEach { waypoint ->
                Text("→", style = MaterialTheme.typography.bodySmall)
                AssistChip(
                    onClick = { onRemoveWaypoint(waypoint.id) },
                    label = {
                        Text(
                            text = waypoint.roadName ?: "${waypoint.order}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                )
            }

            Text("→", style = MaterialTheme.typography.bodySmall)
            Text("🎯", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RouteInfoCard(
    route: com.stonecode.mapsroutepicker.domain.model.Route,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = route.summary ?: "Route",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "📍 ${route.getFormattedDistance()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "⏱️ ${route.getFormattedDuration()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun UserGuidanceHint(
    state: MapState,
    modifier: Modifier = Modifier
) {
    val hintText = when {
        state.destination == null -> "👆 Tap anywhere on the map to set your destination"
        state.waypoints.isEmpty() -> "✨ Tap a road to add it as a waypoint"
        else -> "🛣️ Keep tapping roads to add more waypoints"
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = hintText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
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
