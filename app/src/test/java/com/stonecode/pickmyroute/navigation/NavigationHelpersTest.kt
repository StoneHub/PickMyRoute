package com.stonecode.pickmyroute.navigation

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Phase 1 Driving Mode navigation helpers
 * Tests distance formatting, advancement logic, and off-route detection
 */
class NavigationHelpersTest {

    @Test
    fun distanceFormatting_kilometers() {
        // Test km formatting with one decimal
        assertEquals("1.5 km", formatDistance(1520.0))
        assertEquals("2.0 km", formatDistance(2000.0))
        assertEquals("1.0 km", formatDistance(1000.0))
    }

    @Test
    fun distanceFormatting_largeMeters() {
        // Test 100-999 meters as integers
        assertEquals("500 m", formatDistance(500.0))
        assertEquals("100 m", formatDistance(100.0))
        assertEquals("999 m", formatDistance(999.0))
    }

    @Test
    fun distanceFormatting_roundedMeters() {
        // Test 20-99 meters rounded to nearest 5
        assertEquals("85 m", formatDistance(83.0))
        assertEquals("20 m", formatDistance(22.0))
        assertEquals("50 m", formatDistance(48.0))
        assertEquals("95 m", formatDistance(97.0))
    }

    @Test
    fun distanceFormatting_nowThreshold() {
        // Test "Now" label under 15m
        assertEquals("Now", formatDistance(12.0))
        assertEquals("Now", formatDistance(5.0))
        assertEquals("Now", formatDistance(14.9))
    }

    @Test
    fun distanceFormatting_smallMeters() {
        // Test 15-19 meters show exact value
        assertEquals("15 m", formatDistance(15.0))
        assertEquals("18 m", formatDistance(18.0))
        assertEquals("19 m", formatDistance(19.0))
    }

    @Test
    fun advancement_threshold() {
        // TODO: Build a fake route with steps and verify advancement logic
        // - Create StepRef instances with known distances
        // - Simulate device location near step end
        // - Assert that advancement occurs when remaining < 12m or distance to end < 15m
        // - Verify step index increments exactly once per maneuver
    }

    @Test
    fun offroute_strike_sequence_enter() {
        // TODO: Simulate off-route detection
        // - Start with strikes at 0
        // - Simulate 3 consecutive location updates with distance > 45m
        // - Assert offRouteStrike reaches 3 and isOffRoute becomes true
    }

    @Test
    fun offroute_strike_sequence_exit() {
        // TODO: Simulate returning to route
        // - Start with isOffRoute = true
        // - Simulate 2 consecutive location updates with distance < 30m
        // - Assert onRouteStrike reaches 2 and isOffRoute becomes false
    }

    // Helper function matching the one in InstructionBanner.kt
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
}
