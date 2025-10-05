package com.stonecode.mapsroutepicker.domain.model

/**
 * Represents a place search prediction from Google Places API
 * Used for autocomplete suggestions in the search bar
 */
data class PlacePrediction(
    val placeId: String,
    val mainText: String,        // e.g., "Starbucks"
    val secondaryText: String,   // e.g., "123 Main St, San Francisco, CA"
    val fullText: String         // Full combined description
)

