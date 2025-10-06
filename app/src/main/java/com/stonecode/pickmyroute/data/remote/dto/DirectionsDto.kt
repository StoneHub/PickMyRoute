package com.stonecode.pickmyroute.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs matching Google Directions API response format
 * https://developers.google.com/maps/documentation/directions/get-directions
 */

data class DirectionsResponse(
    @SerializedName("routes") val routes: List<RouteDto>,
    @SerializedName("status") val status: String,
    @SerializedName("error_message") val errorMessage: String? = null
)

data class RouteDto(
    @SerializedName("summary") val summary: String?,
    @SerializedName("legs") val legs: List<RouteLegDto>,
    @SerializedName("overview_polyline") val overviewPolyline: PolylineDto,
    @SerializedName("bounds") val bounds: BoundsDto,
    @SerializedName("warnings") val warnings: List<String>?,
    @SerializedName("copyrights") val copyrights: String?
)

data class RouteLegDto(
    @SerializedName("distance") val distance: DistanceDto,
    @SerializedName("duration") val duration: DurationDto,
    @SerializedName("start_location") val startLocation: LocationDto,
    @SerializedName("end_location") val endLocation: LocationDto,
    @SerializedName("start_address") val startAddress: String?,
    @SerializedName("end_address") val endAddress: String?,
    @SerializedName("steps") val steps: List<StepDto>
)

data class StepDto(
    @SerializedName("html_instructions") val htmlInstructions: String,
    @SerializedName("distance") val distance: DistanceDto,
    @SerializedName("duration") val duration: DurationDto,
    @SerializedName("start_location") val startLocation: LocationDto,
    @SerializedName("end_location") val endLocation: LocationDto,
    @SerializedName("polyline") val polyline: PolylineDto,
    @SerializedName("maneuver") val maneuver: String?
)

data class DistanceDto(
    @SerializedName("value") val value: Int, // meters
    @SerializedName("text") val text: String
)

data class DurationDto(
    @SerializedName("value") val value: Int, // seconds
    @SerializedName("text") val text: String
)

data class LocationDto(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class PolylineDto(
    @SerializedName("points") val points: String // Encoded polyline
)

data class BoundsDto(
    @SerializedName("northeast") val northeast: LocationDto,
    @SerializedName("southwest") val southwest: LocationDto
)
