package com.stonecode.pickmyroute.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.stonecode.pickmyroute.data.location.LocationRepositoryImpl
import com.stonecode.pickmyroute.domain.repository.LocationRepository
import com.stonecode.pickmyroute.domain.repository.PlacesRepository
import com.stonecode.pickmyroute.domain.repository.RoutingRepository
import com.stonecode.pickmyroute.data.repository.PlacesRepositoryImpl
import com.stonecode.pickmyroute.data.repository.RoutingRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing location-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideFusedLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(
        @ApplicationContext context: Context,
        fusedLocationClient: FusedLocationProviderClient
    ): LocationRepository {
        return LocationRepositoryImpl(context, fusedLocationClient)
    }
}
