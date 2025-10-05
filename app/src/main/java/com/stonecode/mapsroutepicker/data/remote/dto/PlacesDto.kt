package com.stonecode.mapsroutepicker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for Google Places API (New)
 * Reference: https://developers.google.com/maps/documentation/places/web-service/autocomplete
 */

// Autocomplete Response
data class PlacesAutocompleteResponse(
    @SerializedName("predictions")
    val predictions: List<AutocompletePrediction>,
    @SerializedName("status")
    val status: String
)

data class AutocompletePrediction(
    @SerializedName("place_id")
    val placeId: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("structured_formatting")
    val structuredFormatting: StructuredFormatting
)

data class StructuredFormatting(
    @SerializedName("main_text")
    val mainText: String,           // "Starbucks"
    @SerializedName("secondary_text")
    val secondaryText: String?      // "123 Main St, San Francisco, CA"
)

// Place Details Response
data class PlaceDetailsResponse(
    @SerializedName("result")
    val result: PlaceResult,
    @SerializedName("status")
    val status: String
)

data class PlaceResult(
    @SerializedName("place_id")
    val placeId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("formatted_address")
    val formattedAddress: String,
    @SerializedName("geometry")
    val geometry: PlaceGeometry
)

data class PlaceGeometry(
    @SerializedName("location")
    val location: PlaceLocation
)

data class PlaceLocation(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("lng")
    val lng: Double
)

