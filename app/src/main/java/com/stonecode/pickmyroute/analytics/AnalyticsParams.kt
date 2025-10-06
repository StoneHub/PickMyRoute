package com.stonecode.pickmyroute.analytics

/**
 * Centralized Firebase Analytics parameter names
 * Use these constants for event parameters
 */
object AnalyticsParams {
    // Route parameters
    const val ROUTE_DISTANCE_METERS = "route_distance_meters"
    const val ROUTE_DURATION_SECONDS = "route_duration_seconds"
    const val WAYPOINT_COUNT = "waypoint_count"

    // Search parameters
    const val SEARCH_QUERY = "search_query"
    const val PLACE_ID = "place_id"

    // Error parameters
    const val ERROR_MESSAGE = "error_message"
    const val ERROR_TYPE = "error_type"

    // Navigation parameters
    const val NAVIGATION_DURATION_SECONDS = "navigation_duration_seconds"
}

