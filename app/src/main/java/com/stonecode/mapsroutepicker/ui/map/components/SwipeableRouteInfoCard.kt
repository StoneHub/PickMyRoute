package com.stonecode.mapsroutepicker.ui.map.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.stonecode.mapsroutepicker.domain.model.Route
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Route info card with swipe-to-reveal close button
 * Features: 200dp swipe distance, spring physics, velocity tracking, reduced haptic feedback
 */
@Composable
fun SwipeableRouteInfoCard(
    route: Route,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffsetX by remember { mutableStateOf(0f) }
    var isRevealed by remember { mutableStateOf(false) }
    var wasRevealed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val velocityTracker = remember { VelocityTracker() }

    // Spring animation only when not dragging
    val animatedOffset by animateFloatAsState(
        targetValue = if (isRevealed && dragOffsetX == 0f) -200f else dragOffsetX,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "card_swipe"
    )

    Box(
        modifier = modifier
    ) {
        // Background close button (shows as you drag or when revealed)
        if (isRevealed || dragOffsetX < -50f) {
            val buttonAlpha = if (dragOffsetX < 0f) {
                (abs(dragOffsetX) / 200f).coerceIn(0f, 1f)
            } else if (isRevealed) {
                1f
            } else {
                0f
            }

            FilledTonalButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClose()
                    isRevealed = false
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(80.dp)
                    .padding(8.dp)
                    .alpha(buttonAlpha),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close route",
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Main card content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                            wasRevealed = isRevealed
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().x

                            // Fling detection: fast swipe triggers reveal
                            val shouldReveal = if (abs(velocity) > 1000f) {
                                velocity < -500f // Swiping left fast
                            } else {
                                dragOffsetX < -100f // Dragged past halfway
                            }

                            // Only haptic feedback when state changes
                            if (shouldReveal != wasRevealed) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }

                            isRevealed = shouldReveal
                            dragOffsetX = 0f
                        },
                        onDragCancel = {
                            isRevealed = wasRevealed
                            dragOffsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )

                            dragOffsetX += dragAmount
                            // Only allow left swipe, constrain to 0 to -220
                            dragOffsetX = dragOffsetX.coerceIn(-220f, 0f)
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            onClick = {
                // Tap to hide close button if revealed
                if (isRevealed) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isRevealed = false
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.summary ?: "Route",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "📍 ${route.getFormattedDistance()}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "⏱️ ${route.getFormattedDuration()}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Swipe hint indicator - pulses to show it's swipeable
                Text(
                    text = if (isRevealed) "◄" else "◄◄",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isRevealed) 0.3f else 0.5f),
                    modifier = Modifier.alpha(if (isRevealed) 0.3f else 0.7f)
                )
            }
        }
    }
}
