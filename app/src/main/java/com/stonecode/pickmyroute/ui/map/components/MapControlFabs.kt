package com.stonecode.pickmyroute.ui.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.CameraPositionState

/**
 * Floating Action Buttons stacked in bottom-right corner
 * Contains Compass and My Location buttons
 */
@Composable
fun MapControlFabs(
    cameraPositionState: CameraPositionState,
    onMyLocationClick: () -> Unit,
    onCompassClick: () -> Unit,
    isNavigating: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Compass/Navigation button - Reset bearing to north (HIDDEN during navigation)
        if (!isNavigating) {
            FloatingActionButton(
                onClick = onCompassClick,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Reset compass to north",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // My Location button - Center on user (always visible)
        FloatingActionButton(
            onClick = onMyLocationClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "Center on my location",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
