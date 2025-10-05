package com.stonecode.mapsroutepicker.ui.permissions

import android.Manifest
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.*

/**
 * Handles location permission requests for the app.
 * Requests both FINE and COARSE location permissions.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable (permissionState: MultiplePermissionsState) -> Unit
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    content(permissionsState)
}
