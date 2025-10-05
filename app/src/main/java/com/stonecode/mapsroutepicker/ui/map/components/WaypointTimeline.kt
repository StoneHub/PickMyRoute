package com.stonecode.mapsroutepicker.ui.map.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stonecode.mapsroutepicker.domain.model.Waypoint

/**
 * Waypoint timeline showing route progression
 * Tap a waypoint bubble to remove it
 * Horizontally scrollable when there are many waypoints
 */
@Composable
fun WaypointTimeline(
    waypoints: List<Waypoint>,
    onRemoveWaypoint: (String) -> Unit,
    modifier: Modifier = Modifier
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
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start indicator
            Text("🏁", style = MaterialTheme.typography.bodyMedium)

            // Waypoint bubbles
            sortedWaypoints.forEachIndexed { index, waypoint ->
                Text("→", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                WaypointBubble(
                    label = ('A' + index).toString(),
                    color = getWaypointColor(index),
                    onClick = { onRemoveWaypoint(waypoint.id) }
                )
            }

            Text("→", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text("🎯", style = MaterialTheme.typography.bodyMedium)
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
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
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
