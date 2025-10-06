package com.stonecode.pickmyroute.data.remote

import com.stonecode.pickmyroute.data.remote.dto.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Google Directions API
 */
interface DirectionsApi {

    /**
     * Get directions between origin and destination with optional waypoints
     *
     * @param origin Starting point as "lat,lng"
     * @param destination End point as "lat,lng"
     * @param waypoints Optional waypoints in format "via:lat,lng|via:lat,lng"
     * @param key Google Maps API key
     */
    @GET("directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String? = null,
        @Query("mode") mode: String = "driving",
        @Query("optimize") optimize: String = "true",
        @Query("key") apiKey: String
    ): DirectionsResponse
}
