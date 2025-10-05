package com.stonecode.mapsroutepicker.data.remote

import com.stonecode.mapsroutepicker.data.remote.dto.PlaceDetailsResponse
import com.stonecode.mapsroutepicker.data.remote.dto.PlacesAutocompleteResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Google Places API (New)
 * Base URL: https://maps.googleapis.com/maps/api/
 */
interface PlacesApi {

    /**
     * Get autocomplete predictions as user types
     *
     * @param input User's search query
     * @param location Bias results near this location (format: "lat,lng")
     * @param radius Search radius in meters
     * @param sessionToken Session token for billing optimization (optional)
     * @param apiKey Google Maps API key
     */
    @GET("place/autocomplete/json")
    suspend fun getAutocompletePredictions(
        @Query("input") input: String,
        @Query("location") location: String? = null,
        @Query("radius") radius: Int = 50000, // 50km radius
        @Query("sessiontoken") sessionToken: String? = null,
        @Query("key") apiKey: String
    ): PlacesAutocompleteResponse

    /**
     * Get detailed information about a place including coordinates
     *
     * @param placeId The place ID from autocomplete prediction
     * @param fields Comma-separated fields to retrieve (keep minimal for cost)
     * @param sessionToken Session token to tie with autocomplete (for billing)
     * @param apiKey Google Maps API key
     */
    @GET("place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "geometry,name,formatted_address",
        @Query("sessiontoken") sessionToken: String? = null,
        @Query("key") apiKey: String
    ): PlaceDetailsResponse
}

