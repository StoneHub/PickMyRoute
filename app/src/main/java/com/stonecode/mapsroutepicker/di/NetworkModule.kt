package com.stonecode.mapsroutepicker.di

import com.stonecode.mapsroutepicker.data.remote.DirectionsApi
import com.stonecode.mapsroutepicker.data.remote.PlacesApi
import com.stonecode.mapsroutepicker.data.repository.PlacesRepositoryImpl
import com.stonecode.mapsroutepicker.data.repository.RoutingRepositoryImpl
import com.stonecode.mapsroutepicker.domain.repository.PlacesRepository
import com.stonecode.mapsroutepicker.domain.repository.RoutingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing network-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val MAPS_API_BASE_URL = "https://maps.googleapis.com/maps/api/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(MAPS_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDirectionsApi(retrofit: Retrofit): DirectionsApi {
        return retrofit.create(DirectionsApi::class.java)
    }

    @Provides
    @Singleton
    fun providePlacesApi(retrofit: Retrofit): PlacesApi {
        return retrofit.create(PlacesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRoutingRepository(
        directionsApi: DirectionsApi
    ): RoutingRepository {
        return RoutingRepositoryImpl(directionsApi)
    }

    @Provides
    @Singleton
    fun providePlacesRepository(
        placesApi: PlacesApi
    ): PlacesRepository {
        return PlacesRepositoryImpl(placesApi)
    }
}
