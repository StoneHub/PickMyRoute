package com.stonecode.pickmyroute.ui.map.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stonecode.pickmyroute.ui.map.MapState

@Composable
fun InstructionBanner(
    state: MapState,
    onStopNavigation: () -> Unit
) {
    if (!state.isNavigating || state.route == null) return

    val distanceText = state.distanceToNextManeuverMeters?.let { formatDistance(it) } ?: ""

    val bg = when {
        state.isOffRoute -> MaterialTheme.colorScheme.errorContainer
        (state.distanceToNextManeuverMeters ?: Double.MAX_VALUE) <= 15.0 -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        color = bg,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Text(text = distanceText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text(text = if (state.isOffRoute) "Off route • ${state.offRouteDistanceMeters?.toInt() ?: ""} m" else state.nextInstructionPrimary ?: "", maxLines = 1)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onStopNavigation) {
                Icon(Icons.Default.Close, contentDescription = "Stop")
            }
        }
    }
}

// Simple formatter matching the plan's rules
private fun formatDistance(meters: Double): String {
    return when {
        meters >= 1000 -> String.format("%.1f km", meters / 1000.0)
        meters >= 100 -> "${meters.toInt()} m"
        meters >= 20 -> {
            // Round to nearest 5: add 2.5 before dividing to get proper rounding
            val rounded = ((meters + 2.5) / 5.0).toInt() * 5
            "$rounded m"
        }
        meters < 15 -> "Now"
        else -> "${meters.toInt()} m"
    }
}
