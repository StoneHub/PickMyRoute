package com.stonecode.pickmyroute.analytics

/**
 * Centralized Firebase Analytics event names
 * Use these constants to ensure consistency across the app
 */
object AnalyticsEvents {
    // Route Planning Events
    const val ROUTE_CALCULATED = "route_calculated"
    const val WAYPOINT_ADDED = "waypoint_added"
    const val WAYPOINT_REMOVED = "waypoint_removed"
    const val ROUTE_CLEARED = "route_cleared"

    // Navigation Events
    const val NAVIGATION_STARTED = "navigation_started"
    const val NAVIGATION_STOPPED = "navigation_stopped"

    // Search Events
    const val PLACE_SEARCHED = "place_searched"
    const val PLACE_SELECTED = "place_selected"

    // Map Interaction Events
    const val MAP_TAPPED = "map_tapped"
    const val MY_LOCATION_CLICKED = "my_location_clicked"
    const val COMPASS_RESET = "compass_reset"

    // Error Events
    const val ROUTE_ERROR = "route_error"
    const val SEARCH_ERROR = "search_error"
}
