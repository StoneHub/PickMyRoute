package com.stonecode.mapsroutepicker.data.repository

import com.google.android.gms.maps.model.LatLng
import com.stonecode.mapsroutepicker.BuildConfig
import com.stonecode.mapsroutepicker.data.remote.PlacesApi
import com.stonecode.mapsroutepicker.domain.model.PlacePrediction
import com.stonecode.mapsroutepicker.domain.repository.PlacesRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PlacesRepository using Google Places API
 * Handles search predictions and place details fetching
 */
@Singleton
class PlacesRepositoryImpl @Inject constructor(
    private val placesApi: PlacesApi
) : PlacesRepository {

    // Session token for billing optimization - groups autocomplete + details as one request
    private var currentSessionToken: String = generateSessionToken()

    override suspend fun searchPlaces(
        query: String,
        userLocation: LatLng?
    ): List<PlacePrediction> {
        // Don't search for empty or very short queries
        if (query.length < 2) {
            return emptyList()
        }

        val locationBias = userLocation?.let { "${it.latitude},${it.longitude}" }

        val response = placesApi.getAutocompletePredictions(
            input = query,
            location = locationBias,
            radius = 50000, // 50km
            sessionToken = currentSessionToken,
            apiKey = BuildConfig.MAPS_API_KEY
        )

        // Check if request was successful
        if (response.status != "OK" && response.status != "ZERO_RESULTS") {
            throw Exception("Places API error: ${response.status}")
        }

        // Convert DTOs to domain models
        return response.predictions.map { prediction ->
            PlacePrediction(
                placeId = prediction.placeId,
                mainText = prediction.structuredFormatting.mainText,
                secondaryText = prediction.structuredFormatting.secondaryText ?: "",
                fullText = prediction.description
            )
        }
    }

    override suspend fun getPlaceLocation(placeId: String): LatLng {
        val response = placesApi.getPlaceDetails(
            placeId = placeId,
            fields = "geometry,name,formatted_address",
            sessionToken = currentSessionToken,
            apiKey = BuildConfig.MAPS_API_KEY
        )

        // Reset session token after completing a search session
        currentSessionToken = generateSessionToken()

        if (response.status != "OK") {
            throw Exception("Failed to get place details: ${response.status}")
        }

        val location = response.result.geometry.location
        return LatLng(location.lat, location.lng)
    }

    /**
     * Generate a unique session token for Places API billing optimization
     * Groups autocomplete requests with the final place details request
     */
    private fun generateSessionToken(): String {
        return UUID.randomUUID().toString()
    }
}

