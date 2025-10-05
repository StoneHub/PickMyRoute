package com.stonecode.mapsroutepicker.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            // Show main map content
            MapContent(state = state, viewModel = viewModel)
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

        // Top controls and waypoint timeline
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Destination input button
            if (!state.showDestinationInput) {
                Button(
                    onClick = { viewModel.onEvent(MapEvent.ToggleDestinationInput) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.destination == null) "Set Destination" else "Change Destination")
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

        // Loading indicator
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Error message
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(error)
            }
        }

        // Route info card (bottom)
        state.route?.let { route ->
            RouteInfoCard(
                route = route,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            )
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
            onMapTapped(location)
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
