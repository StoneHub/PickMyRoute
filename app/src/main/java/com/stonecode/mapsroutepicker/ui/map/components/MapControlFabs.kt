package com.stonecode.mapsroutepicker.ui.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Floating Action Buttons stacked in bottom-right corner
 * Contains Compass, My Location button, and Close (X) button
 */
@Composable
fun MapControlFabs(
    onMyLocationClick: () -> Unit,
    onCloseClick: () -> Unit,
    showCloseButton: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Close button (X) - only show when route exists
        if (showCloseButton) {
            FloatingActionButton(
                onClick = onCloseClick,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear route",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Compass/Navigation button
        FloatingActionButton(
            onClick = { /* TODO: Re-center and rotate to north */ },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Compass",
                modifier = Modifier.size(28.dp)
            )
        }

        // My Location button
        FloatingActionButton(
            onClick = onMyLocationClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = "My location",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
