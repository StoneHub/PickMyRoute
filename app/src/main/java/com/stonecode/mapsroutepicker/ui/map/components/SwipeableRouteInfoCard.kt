package com.stonecode.mapsroutepicker.ui.map.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.stonecode.mapsroutepicker.domain.model.Route
import com.stonecode.mapsroutepicker.domain.model.RouteLeg
import com.stonecode.mapsroutepicker.ui.theme.MapsRoutePickerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Route info card with swipe-to-reveal close button
 * Features: 200dp swipe distance, spring physics, velocity tracking, bounce hint animation
 */
@Composable
fun SwipeableRouteInfoCard(
    route: Route,
    onClose: () -> Unit,
    onStartNavigation: () -> Unit = {},
    onStopNavigation: () -> Unit = {},
    isNavigating: Boolean = false,
    modifier: Modifier = Modifier
) {
    var dragOffsetX by remember { mutableStateOf(0f) }
    var isRevealed by remember { mutableStateOf(false) }
    var wasRevealed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val velocityTracker = remember { VelocityTracker() }
    val scope = rememberCoroutineScope()

    // Bounce animation to hint at swipe-to-dismiss on first appearance
    val bounceOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Wait longer so user notices the card first, then show the swipe hint
        delay(2500)
        bounceOffset.animateTo(
            targetValue = -60f,
            animationSpec = tween(durationMillis = 300)
        )
        delay(200)
        bounceOffset.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // Spring animation only when not dragging
    val animatedOffset by animateFloatAsState(
        targetValue = if (isRevealed && dragOffsetX == 0f) -200f else dragOffsetX,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "card_swipe"
    )

    // Combine bounce hint with drag/reveal offset
    val finalOffset = animatedOffset + bounceOffset.value

    Box(
        modifier = modifier
    ) {
        // Background button (shows as you drag or when revealed)
        // Shows "Exit Nav" in nav mode, "Cancel Trip" in planning mode
        if (isRevealed || dragOffsetX < -50f || bounceOffset.value < -10f) {
            val buttonAlpha = when {
                dragOffsetX < 0f -> (abs(dragOffsetX) / 200f).coerceIn(0f, 1f)
                bounceOffset.value < 0f -> (abs(bounceOffset.value) / 60f).coerceIn(0f, 0.8f)
                isRevealed -> 1f
                else -> 0f
            }

            FilledTonalButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (isNavigating) {
                        // Exit navigation mode and return to planning
                        onStopNavigation()
                    } else {
                        // Cancel the entire trip
                        onClose()
                    }
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
                    contentDescription = if (isNavigating) "Exit navigation" else "Cancel trip",
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Main card content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(finalOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                            wasRevealed = isRevealed
                            // Cancel bounce animation if user starts dragging
                            scope.launch {
                                bounceOffset.snapTo(0f)
                            }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Standardized swipe handle at the top
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                    // Show Start Navigation button or navigation indicator based on state
                    if (isNavigating) {
                        // Show animated chevrons when navigating
                        SwipeRippleIndicator()
                    } else {
                        // Show Start Navigation button with animated right chevron
                        FilledTonalButton(
                            onClick = onStartNavigation,
                            modifier = Modifier.height(40.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("Start", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(6.dp))
                            StartNavigationChevron()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated right-pointing chevron indicator for Start Navigation button
 * Mirrors the left-pointing swipe indicator but points right
 */
@Composable
private fun StartNavigationChevron() {
    val rippleProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            rippleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500)
            )
            rippleProgress.snapTo(0f)
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.onPrimaryContainer

    Canvas(
        modifier = Modifier
            .width(32.dp)
            .height(18.dp)
    ) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f

        // Chevron dimensions
        val chevronWidth = w * 0.18f
        val chevronHeight = h * 0.7f
        val spacing = w * 0.15f
        val strokeWidth = h * 0.14f

        // Draw 3 chevrons (>>>) from left to right
        repeat(3) { chevronIndex ->
            val chevronX = w * 0.2f + (chevronIndex * spacing)

            // Calculate ripple brightness for this chevron
            val rippleX = w * rippleProgress.value
            val distanceToRipple = abs(chevronX - rippleX)
            val rippleRadius = w * 0.25f
            val brightness = (1f - (distanceToRipple / rippleRadius).coerceIn(0f, 1f))

            // Base alpha for chevron (increases from left to right)
            val baseAlpha = 0.5f + (chevronIndex * 0.15f)

            // Combine base alpha with ripple brightness
            val finalAlpha = (baseAlpha + brightness * 0.4f).coerceIn(0f, 1f)

            // Draw left line of chevron: >
            drawLine(
                color = surfaceColor.copy(alpha = finalAlpha),
                start = Offset(chevronX, centerY - chevronHeight / 2f),
                end = Offset(chevronX + chevronWidth, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Draw right line of chevron: >
            drawLine(
                color = surfaceColor.copy(alpha = finalAlpha),
                start = Offset(chevronX + chevronWidth, centerY),
                end = Offset(chevronX, centerY + chevronHeight / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Animated ripple indicator showing swipe direction (right to left)
 * Three chevron arrows with pulses flowing through them - shown during navigation
 */
@Composable
private fun SwipeRippleIndicator() {
    val rippleProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            rippleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500)
            )
            rippleProgress.snapTo(0f)
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = Modifier
            .width(48.dp)
            .height(24.dp)
            .alpha(0.6f)
    ) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f

        // Chevron dimensions
        val chevronWidth = w * 0.15f
        val chevronHeight = h * 0.7f
        val spacing = w * 0.12f
        val strokeWidth = h * 0.12f

        // Draw 3 chevrons (<<<) from right to left
        repeat(3) { chevronIndex ->
            val chevronX = w * 0.75f - (chevronIndex * spacing)

            // Calculate ripple brightness for this chevron
            // Ripples sweep from right to left across all chevrons
            val rippleX = w * (1f - rippleProgress.value)
            val distanceToRipple = abs(chevronX - rippleX)
            val rippleRadius = w * 0.25f
            val brightness = (1f - (distanceToRipple / rippleRadius).coerceIn(0f, 1f))

            // Base alpha for chevron (decreases from right to left)
            val baseAlpha = 0.3f + (chevronIndex * 0.15f)

            // Combine base alpha with ripple brightness
            val finalAlpha = (baseAlpha + brightness * 0.5f).coerceIn(0f, 1f)

            // Draw left line of chevron: <
            drawLine(
                color = surfaceColor.copy(alpha = finalAlpha),
                start = Offset(chevronX, centerY - chevronHeight / 2f),
                end = Offset(chevronX - chevronWidth, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Draw right line of chevron: <
            drawLine(
                color = surfaceColor.copy(alpha = finalAlpha),
                start = Offset(chevronX - chevronWidth, centerY),
                end = Offset(chevronX, centerY + chevronHeight / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

// Preview functions
@Preview(name = "Route Card - Light", showBackground = true)
@Composable
private fun SwipeableRouteInfoCardPreview() {
    MapsRoutePickerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SwipeableRouteInfoCard(
                route = createSampleRoute(),
                onClose = {}
            )
        }
    }
}

@Preview(name = "Route Card - Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SwipeableRouteInfoCardPreviewDark() {
    MapsRoutePickerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SwipeableRouteInfoCard(
                route = createSampleRoute(),
                onClose = {}
            )
        }
    }
}

@Preview(name = "Long Route", showBackground = true)
@Composable
private fun SwipeableRouteInfoCardPreviewLong() {
    MapsRoutePickerTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            SwipeableRouteInfoCard(
                route = createSampleRoute(
                    summary = "I-80 E and I-90 E via Chicago",
                    distanceMeters = 485000,
                    durationSeconds = 28800
                ),
                onClose = {}
            )
        }
    }
}

/**
 * Create a sample route for preview purposes
 */
private fun createSampleRoute(
    summary: String = "I-280 N and US-101 N",
    distanceMeters: Int = 42500,
    durationSeconds: Int = 1980
): Route {
    val startLocation = LatLng(37.7749, -122.4194) // San Francisco
    val endLocation = LatLng(37.4419, -122.1430)   // Palo Alto

    return Route(
        summary = summary,
        overviewPolyline = "",
        legs = listOf(
            RouteLeg(
                steps = emptyList(),
                durationSeconds = durationSeconds,
                distanceMeters = distanceMeters,
                startLocation = startLocation,
                endLocation = endLocation,
                startAddress = "San Francisco, CA",
                endAddress = "Palo Alto, CA"
            )
        ),
        bounds = LatLngBounds.builder()
            .include(startLocation)
            .include(endLocation)
            .build(),
        totalDurationSeconds = durationSeconds,
        totalDistanceMeters = distanceMeters,
        waypoints = emptyList(),
        warnings = emptyList()
    )
}
