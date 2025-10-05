package com.stonecode.mapsroutepicker.domain.model

/**
 * Represents a single turn-by-turn navigation instruction
 */
data class NavigationStep(
    val instruction: String,        // e.g., "Turn right onto Main St"
    val htmlInstruction: String,    // HTML formatted instruction from API
    val distanceMeters: Int,
    val durationSeconds: Int,
    val polyline: String,           // Encoded polyline for this step
    val startLocation: com.google.android.gms.maps.model.LatLng,
    val endLocation: com.google.android.gms.maps.model.LatLng,
    val maneuver: ManeuverType
)

/**
 * Types of navigation maneuvers
 */
enum class ManeuverType {
    TURN_LEFT,
    TURN_RIGHT,
    TURN_SLIGHT_LEFT,
    TURN_SLIGHT_RIGHT,
    TURN_SHARP_LEFT,
    TURN_SHARP_RIGHT,
    UTURN_LEFT,
    UTURN_RIGHT,
    MERGE,
    FORK_LEFT,
    FORK_RIGHT,
    ROUNDABOUT_LEFT,
    ROUNDABOUT_RIGHT,
    RAMP_LEFT,
    RAMP_RIGHT,
    KEEP_LEFT,
    KEEP_RIGHT,
    STRAIGHT,
    FERRY,
    FERRY_TRAIN,
    DESTINATION,
    UNKNOWN;

    companion object {
        /**
         * Converts Google Directions API maneuver string to enum
         */
        fun fromApiString(maneuver: String?): ManeuverType {
            return when (maneuver?.lowercase()) {
                "turn-left" -> TURN_LEFT
                "turn-right" -> TURN_RIGHT
                "turn-slight-left" -> TURN_SLIGHT_LEFT
                "turn-slight-right" -> TURN_SLIGHT_RIGHT
                "turn-sharp-left" -> TURN_SHARP_LEFT
                "turn-sharp-right" -> TURN_SHARP_RIGHT
                "uturn-left" -> UTURN_LEFT
                "uturn-right" -> UTURN_RIGHT
                "merge" -> MERGE
                "fork-left" -> FORK_LEFT
                "fork-right" -> FORK_RIGHT
                "roundabout-left" -> ROUNDABOUT_LEFT
                "roundabout-right" -> ROUNDABOUT_RIGHT
                "ramp-left" -> RAMP_LEFT
                "ramp-right" -> RAMP_RIGHT
                "keep-left" -> KEEP_LEFT
                "keep-right" -> KEEP_RIGHT
                "straight" -> STRAIGHT
                "ferry" -> FERRY
                "ferry-train" -> FERRY_TRAIN
                else -> UNKNOWN
            }
        }
    }
}
