package com.stonecode.mapsroutepicker.ui.map.components

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stonecode.mapsroutepicker.domain.model.Route
import com.stonecode.mapsroutepicker.domain.model.Waypoint

/**
 * Waypoint timeline showing route progression with distances
 * Tap a waypoint bubble to remove it
 * Horizontally scrollable when there are many waypoints
 */
@Composable
fun WaypointTimeline(
    waypoints: List<Waypoint>,
    onRemoveWaypoint: (String) -> Unit,
    modifier: Modifier = Modifier,
    isNavigating: Boolean = false,
    route: Route? = null
) {
    val sortedWaypoints = waypoints.sortedBy { it.order }
    val scrollState = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start indicator
            Text("🏁", style = MaterialTheme.typography.bodyLarge)

            // Waypoint bubbles with colored arrows and distances
            sortedWaypoints.forEachIndexed { index, waypoint ->
                // Arrow with distance below, colored to match the waypoint it's going TO
                ArrowWithDistance(
                    color = getWaypointColor(index),
                    distanceMeters = route?.legs?.getOrNull(index)?.distanceMeters
                )

                WaypointBubble(
                    label = ('A' + index).toString(),
                    color = getWaypointColor(index),
                    onClick = { onRemoveWaypoint(waypoint.id) },
                    isNavigating = isNavigating
                )
            }

            // Final arrow to destination (red) with distance
            val lastLegIndex = sortedWaypoints.size
            ArrowWithDistance(
                color = Color(0xFFE53935),  // Red to match destination
                distanceMeters = route?.legs?.getOrNull(lastLegIndex)?.distanceMeters
            )
            Text("🎯", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * Simple waypoint bubble that can be tapped to remove
 */
@Composable
private fun WaypointBubble(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNavigating: Boolean = false
) {
    val context = LocalContext.current

    FilledTonalButton(
        onClick = {
            if (isNavigating) {
                // Show toast telling user to exit navigation mode first
                Toast.makeText(
                    context,
                    "❌ Exit navigation mode to edit waypoints",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                onClick()
            }
        },
        modifier = modifier.size(48.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = color,
            contentColor = Color.White
        ),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Arrow with distance label below it
 */
@Composable
private fun ArrowWithDistance(
    color: Color,
    distanceMeters: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        // Larger arrow
        Text(
            text = "→",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 28.sp,
            color = color
        )

        // Distance label below arrow
        if (distanceMeters != null) {
            Text(
                text = formatDistance(distanceMeters),
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Format distance in meters to human-readable string
 */
private fun formatDistance(meters: Int, useMetric: Boolean = false): String {
    return if (useMetric) {
        when {
            meters < 1000 -> "${meters}m"
            else -> String.format("%.1fkm", meters / 1000.0)
        }
    } else {
        val miles = meters * 0.000621371
        when {
            miles < 0.1 -> "${(meters * 3.28084).toInt()}ft"
            miles < 10 -> String.format("%.1fmi", miles)
            else -> String.format("%.0fmi", miles)
        }
    }
}

/**
 * Get color for waypoint by index
 */
fun getWaypointColor(index: Int): Color {
    val colors = listOf(
        Color(0xFFE53935), // Red
        Color(0xFF1E88E5), // Blue
        Color(0xFF43A047), // Green
        Color(0xFFFFB300), // Amber
        Color(0xFF8E24AA), // Purple
        Color(0xFFFF6F00), // Orange
        Color(0xFF00ACC1), // Cyan
        Color(0xFFC62828), // Dark Red
        Color(0xFF5E35B1), // Deep Purple
        Color(0xFF00897B), // Teal
    )
    return colors[index % colors.size]
}
