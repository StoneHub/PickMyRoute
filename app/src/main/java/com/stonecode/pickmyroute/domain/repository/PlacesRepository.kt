package com.stonecode.pickmyroute.domain.repository

import com.google.android.gms.maps.model.LatLng
import com.stonecode.pickmyroute.domain.model.PlacePrediction

/**
 * Repository for searching places using Google Places API
 */
interface PlacesRepository {

    /**
     * Search for places based on user query
     * Returns autocomplete predictions
     *
     * @param query User's search text
     * @param userLocation Optional location to bias results near user
     * @return List of place predictions
     * @throws Exception if search fails
     */
    suspend fun searchPlaces(
        query: String,
        userLocation: LatLng? = null
    ): List<PlacePrediction>

    /**
     * Get the exact coordinates for a selected place
     *
     * @param placeId The place ID from a prediction
     * @return Coordinates of the place
     * @throws Exception if place details cannot be fetched
     */
    suspend fun getPlaceLocation(placeId: String): LatLng
}
