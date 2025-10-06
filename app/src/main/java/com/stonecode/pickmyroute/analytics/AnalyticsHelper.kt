package com.stonecode.pickmyroute.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
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
        firebaseAnalytics.logEvent(AnalyticsEvents.ROUTE_CALCULATED) {
            param(AnalyticsParams.ROUTE_DISTANCE_METERS, distanceMeters.toLong())
            param(AnalyticsParams.ROUTE_DURATION_SECONDS, durationSeconds.toLong())
            param(AnalyticsParams.WAYPOINT_COUNT, waypointCount.toLong())
        }
    }

    /**
     * Log a waypoint added event
     */
    fun logWaypointAdded(waypointCount: Int) {
        firebaseAnalytics.logEvent(AnalyticsEvents.WAYPOINT_ADDED) {
            param(AnalyticsParams.WAYPOINT_COUNT, waypointCount.toLong())
        }
    }

    /**
     * Log a waypoint removed event
     */
    fun logWaypointRemoved(waypointCount: Int) {
        firebaseAnalytics.logEvent(AnalyticsEvents.WAYPOINT_REMOVED) {
            param(AnalyticsParams.WAYPOINT_COUNT, waypointCount.toLong())
        }
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
        firebaseAnalytics.logEvent(AnalyticsEvents.NAVIGATION_STARTED) {
            param(AnalyticsParams.ROUTE_DISTANCE_METERS, distanceMeters.toLong())
            param(AnalyticsParams.ROUTE_DURATION_SECONDS, durationSeconds.toLong())
        }
    }

    /**
     * Log navigation stopped
     */
    fun logNavigationStopped(navigationDurationSeconds: Long) {
        firebaseAnalytics.logEvent(AnalyticsEvents.NAVIGATION_STOPPED) {
            param(AnalyticsParams.NAVIGATION_DURATION_SECONDS, navigationDurationSeconds)
        }
    }

    /**
     * Log place search
     */
    fun logPlaceSearched(query: String, resultCount: Int) {
        firebaseAnalytics.logEvent(AnalyticsEvents.PLACE_SEARCHED) {
            param(AnalyticsParams.SEARCH_QUERY, query)
            param("result_count", resultCount.toLong())
        }
    }

    /**
     * Log place selection from search results
     */
    fun logPlaceSelected(placeId: String) {
        firebaseAnalytics.logEvent(AnalyticsEvents.PLACE_SELECTED) {
            param(AnalyticsParams.PLACE_ID, placeId)
        }
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
        firebaseAnalytics.logEvent(AnalyticsEvents.ROUTE_ERROR) {
            param(AnalyticsParams.ERROR_MESSAGE, errorMessage)
            param(AnalyticsParams.ERROR_TYPE, "route_calculation")
        }
    }

    /**
     * Log search error
     */
    fun logSearchError(errorMessage: String) {
        firebaseAnalytics.logEvent(AnalyticsEvents.SEARCH_ERROR) {
            param(AnalyticsParams.ERROR_MESSAGE, errorMessage)
            param(AnalyticsParams.ERROR_TYPE, "place_search")
        }
    }

    /**
     * Set user property (e.g., preferred measurement system)
     */
    fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }
}

