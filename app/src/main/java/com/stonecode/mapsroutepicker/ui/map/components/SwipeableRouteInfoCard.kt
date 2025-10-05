package com.stonecode.mapsroutepicker.ui.map.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.stonecode.mapsroutepicker.domain.model.Route
import kotlin.math.roundToInt

/**
 * Route info card with swipe-to-reveal close button
 * Swipe left to reveal X button, tap X to close, tap elsewhere to hide X
 */
@Composable
fun SwipeableRouteInfoCard(
    route: Route,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isRevealed by remember { mutableStateOf(false) }

    val animatedOffset by animateFloatAsState(
        targetValue = if (isRevealed) -80f else 0f,
        label = "card_swipe"
    )

    Box(
        modifier = modifier
    ) {
        // Background close button (revealed when swiped)
        if (isRevealed) {
            FilledTonalButton(
                onClick = {
                    onClose()
                    isRevealed = false
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(64.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close route",
                    modifier = Modifier.size(32.dp)
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
                        onDragEnd = {
                            // Snap to revealed or hidden based on drag distance
                            isRevealed = offsetX < -40f
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX += dragAmount
                            // Only allow left swipe
                            if (offsetX < 0 && offsetX > -100f) {
                                // Drag in progress
                            } else {
                                offsetX = offsetX.coerceIn(-100f, 0f)
                            }
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            onClick = {
                // Tap to hide X button if revealed
                if (isRevealed) {
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

                // Swipe hint indicator
                Text(
                    text = if (isRevealed) "◄" else "◄◄",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.alpha(if (isRevealed) 0.3f else 0.6f)
                )
            }
        }
    }
}

