package com.stonecode.mapsroutepicker.ui.map.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.stonecode.mapsroutepicker.domain.model.Waypoint
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Waypoint timeline with drag-to-reorder functionality
 * Features: Long-press to drag, spring animations, wiggle effect, haptic feedback
 */
@Composable
fun WaypointTimeline(
    waypoints: List<Waypoint>,
    onRemoveWaypoint: (String) -> Unit,
    onReorderWaypoints: ((List<Waypoint>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sortedWaypoints = remember(waypoints) { waypoints.sortedBy { it.order } }
    var dismissedHint by remember { mutableStateOf(false) }

    // Drag state
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var waypointPositions by remember { mutableStateOf<List<Float>>(emptyList()) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    Column(modifier = modifier) {
        // Dismissible hint card
        if (sortedWaypoints.isNotEmpty() && !dismissedHint) {
            DismissibleHintCard(
                text = "🛣️ Long-press and drag waypoints to reorder • Tap to remove",
                onDismiss = { dismissedHint = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        // Waypoint bubbles
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .onGloballyPositioned { coordinates ->
                        // Track positions for reordering logic
                        waypointPositions = sortedWaypoints.indices.map { index ->
                            (index * 64).toFloat() // Approximate spacing
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Start indicator
                Text("🏁", style = MaterialTheme.typography.bodyMedium)

                // Waypoint bubbles with drag support
                sortedWaypoints.forEachIndexed { index, waypoint ->
                    Text("→", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

                    DraggableWaypointBubble(
                        label = ('A' + index).toString(),
                        color = getWaypointColor(index),
                        isDragging = draggedIndex == index,
                        isBeingDragged = draggedIndex != null,
                        currentIndex = index,
                        draggedIndex = draggedIndex,
                        dragOffset = if (draggedIndex == index) dragOffset else Offset.Zero,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRemoveWaypoint(waypoint.id)
                        },
                        onDragStart = {
                            draggedIndex = index
                            dragOffset = Offset.Zero
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, offset ->
                            if (draggedIndex == index) {
                                dragOffset += offset
                                change.consume()

                                // Calculate new position based on drag
                                val bubbleWidth = with(density) { 64.dp.toPx() }
                                val newIndex = ((dragOffset.x + index * bubbleWidth) / bubbleWidth)
                                    .roundToInt()
                                    .coerceIn(0, sortedWaypoints.lastIndex)

                                if (newIndex != index && newIndex != draggedIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        onDragEnd = {
                            if (draggedIndex != null) {
                                val bubbleWidth = with(density) { 64.dp.toPx() }
                                val newIndex = ((dragOffset.x + draggedIndex!! * bubbleWidth) / bubbleWidth)
                                    .roundToInt()
                                    .coerceIn(0, sortedWaypoints.lastIndex)

                                if (newIndex != draggedIndex) {
                                    // Reorder waypoints
                                    val reordered = sortedWaypoints.toMutableList()
                                    val item = reordered.removeAt(draggedIndex!!)
                                    reordered.add(newIndex, item)
                                    onReorderWaypoints?.invoke(reordered)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }

                                draggedIndex = null
                                dragOffset = Offset.Zero
                            }
                        }
                    )
                }

                Text("→", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                Text("🎯", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Individual waypoint bubble with drag-to-reorder support
 */
@Composable
private fun DraggableWaypointBubble(
    label: String,
    color: Color,
    isDragging: Boolean,
    isBeingDragged: Boolean,
    currentIndex: Int,
    draggedIndex: Int?,
    dragOffset: Offset,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange, offset: Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Scale animation when dragging
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ),
        label = "bubble_scale"
    )

    // Wiggle animation when being dragged
    val wiggleRotation by rememberInfiniteTransition(label = "wiggle").animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle_rotation"
    )

    // Spring animation for other bubbles making room
    val offsetAnimation by animateFloatAsState(
        targetValue = if (!isDragging && isBeingDragged && draggedIndex != null) {
            if (draggedIndex < currentIndex && dragOffset.x > 0) {
                -20f // Squeeze left
            } else if (draggedIndex > currentIndex && dragOffset.x < 0) {
                20f // Squeeze right
            } else {
                0f
            }
        } else {
            0f
        },
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 400f
        ),
        label = "bubble_offset"
    )

    FilledTonalButton(
        onClick = { if (!isDragging) onClick() },
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .offset { IntOffset(offsetAnimation.roundToInt(), 0) }
            .graphicsLayer {
                rotationZ = if (isDragging) wiggleRotation else 0f
                shadowElevation = if (isDragging) 8f else 0f
                alpha = if (isDragging) 0.9f else 1f
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = onDrag,
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            },
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
 * Dismissible hint card
 */
@Composable
private fun DismissibleHintCard(
    text: String,
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Text("×", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
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
