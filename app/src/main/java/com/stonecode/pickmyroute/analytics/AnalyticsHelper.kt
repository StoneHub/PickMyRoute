package com.stonecode.pickmyroute.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for logging Firebase Analytics events
 * Wraps Firebase Analytics to provide type-safe event logging
 */
@Singleton
class AnalyticsHelper @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    /**
     * Log a route calculation event
     */
    fun logRouteCalculated(
        distanceMeters: Int,
        durationSeconds: Int,
        waypointCount: Int
    ) {
        val bundle = Bundle().apply {
            putLong(AnalyticsParams.ROUTE_DISTANCE_METERS, distanceMeters.toLong())
            putLong(AnalyticsParams.ROUTE_DURATION_SECONDS, durationSeconds.toLong())
            putLong(AnalyticsParams.WAYPOINT_COUNT, waypointCount.toLong())
        }
        firebaseAnalytics.logEvent(AnalyticsEvents.ROUTE_CALCULATED, bundle)
    }

    /**
     * Log a waypoint added event
     */
    fun logWaypointAdded(waypointCount: Int) {
        val bundle = Bundle().apply {
            putLong(AnalyticsParams.WAYPOINT_COUNT, waypointCount.toLong())
        }
        firebaseAnalytics.logEvent(AnalyticsEvents.WAYPOINT_ADDED, bundle)
    }

    /**
     * Log a waypoint removed event
     */
    fun logWaypointRemoved(waypointCount: Int) {
        val bundle = Bundle().apply {
            putLong(AnalyticsParams.WAYPOINT_COUNT, waypointCount.toLong())
        }
        firebaseAnalytics.logEvent(AnalyticsEvents.WAYPOINT_REMOVED, bundle)
    }

    /**
     * Log when route is cleared
     */
    fun logRouteCleared() {
        firebaseAnalytics.logEvent(AnalyticsEvents.ROUTE_CLEARED, null)
    }

    /**
     * Log navigation started
     */
    fun logNavigationStarted(
        distanceMeters: Int,
        durationSeconds: Int
    ) {
        val bundle = Bundle().apply {
            putLong(AnalyticsParams.ROUTE_DISTANCE_METERS, distanceMeters.toLong())
            putLong(AnalyticsParams.ROUTE_DURATION_SECONDS, durationSeconds.toLong())
        }
        firebaseAnalytics.logEvent(AnalyticsEvents.NAVIGATION_STARTED, bundle)
    }

    /**
     * Log navigation stopped
     */
    fun logNavigationStopped(navigationDurationSeconds: Long) {
        val bundle = Bundle().apply {
            putLong(AnalyticsParams.NAVIGATION_DURATION_SECONDS, navigationDurationSeconds)
        }
        firebaseAnalytics.logEvent(AnalyticsEvents.NAVIGATION_STOPPED, bundle)
    }

    /**
     * Log place search
     */
    fun logPlaceSearched(query: String, resultCount: Int) {
        val bundle = Bundle().apply {
            putString(AnalyticsParams.SEARCH_QUERY, query)
            putLong("result_count", resultCount.toLong())
        }
        firebaseAnalytics.logEvent(AnalyticsEvents.PLACE_SEARCHED, bundle)
    }

    /**
     * Log place selection from search results
     */
    fun logPlaceSelected(placeId: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsParams.PLACE_ID, placeId)
        }
        firebaseAnalytics.logEvent(AnalyticsEvents.PLACE_SELECTED, bundle)
    }

    /**
     * Log map tap
     */
    fun logMapTapped() {
        firebaseAnalytics.logEvent(AnalyticsEvents.MAP_TAPPED, null)
    }

    /**
     * Log my location button click
     */
    fun logMyLocationClicked() {
        firebaseAnalytics.logEvent(AnalyticsEvents.MY_LOCATION_CLICKED, null)
    }

    /**
     * Log compass reset
     */
    fun logCompassReset() {
        firebaseAnalytics.logEvent(AnalyticsEvents.COMPASS_RESET, null)
    }

    /**
     * Log route calculation error
     */
    fun logRouteError(errorMessage: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsParams.ERROR_MESSAGE, errorMessage)
            putString(AnalyticsParams.ERROR_TYPE, "route_calculation")
        }
        firebaseAnalytics.logEvent(AnalyticsEvents.ROUTE_ERROR, bundle)
    }

    /**
     * Log search error
     */
    fun logSearchError(errorMessage: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsParams.ERROR_MESSAGE, errorMessage)
            putString(AnalyticsParams.ERROR_TYPE, "place_search")
        }
        firebaseAnalytics.logEvent(AnalyticsEvents.SEARCH_ERROR, bundle)
    }

    /**
     * Set user property (e.g., preferred measurement system)
     */
    fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }
}
