package com.stonecode.mapsroutepicker.domain.repository

import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow

/**
 * Repository for accessing device location
 */
interface LocationRepository {
    /**
     * Get continuous location updates as a Flow
     * Emits new location whenever device location changes
     */
    fun getLocationUpdates(): Flow<LatLng>

    /**
     * Get current location once
     * Returns null if location is unavailable
     */
    suspend fun getCurrentLocation(): LatLng?

    /**
     * Check if location services are enabled
     */
    fun isLocationEnabled(): Boolean
}
