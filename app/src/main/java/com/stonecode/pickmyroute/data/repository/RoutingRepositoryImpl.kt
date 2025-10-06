package com.stonecode.pickmyroute.data.repository

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.stonecode.pickmyroute.BuildConfig
import com.stonecode.pickmyroute.data.remote.DirectionsApi
import com.stonecode.pickmyroute.data.remote.dto.RouteDto
import com.stonecode.pickmyroute.domain.model.*
import com.stonecode.pickmyroute.domain.repository.RoutingRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of RoutingRepository using Google Directions API
 */
@Singleton
class RoutingRepositoryImpl @Inject constructor(
    private val directionsApi: DirectionsApi
) : RoutingRepository {

    override suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        waypoints: List<Waypoint>
    ): Route {
        val originStr = "${origin.latitude},${origin.longitude}"
        val destinationStr = "${destination.latitude},${destination.longitude}"
        val waypointsStr = if (waypoints.isNotEmpty()) {
            // Add "optimize:true|" prefix to enable Google's waypoint optimization
            "optimize:true|" + waypoints
                .sortedBy { it.order }
                .joinToString("|") { "${it.location.latitude},${it.location.longitude}" }
        } else null

        val response = directionsApi.getDirections(
            origin = originStr,
            destination = destinationStr,
            waypoints = waypointsStr,
            apiKey = BuildConfig.MAPS_API_KEY
        )

        if (response.status != "OK") {
            throw Exception(response.errorMessage ?: "Failed to get directions: ${response.status}")
        }

        val routeDto = response.routes.firstOrNull()
            ?: throw Exception("No routes found")

        return routeDto.toDomain(waypoints)
    }

    private fun RouteDto.toDomain(waypoints: List<Waypoint>): Route {
        val legs = this.legs.map { legDto ->
            RouteLeg(
                steps = legDto.steps.map { stepDto ->
                    NavigationStep(
                        instruction = stepDto.htmlInstructions.replace(Regex("<[^>]*>"), ""), // Strip HTML
                        htmlInstruction = stepDto.htmlInstructions,
                        distanceMeters = stepDto.distance.value,
                        durationSeconds = stepDto.duration.value,
                        polyline = stepDto.polyline.points,
                        startLocation = LatLng(stepDto.startLocation.lat, stepDto.startLocation.lng),
                        endLocation = LatLng(stepDto.endLocation.lat, stepDto.endLocation.lng),
                        maneuver = ManeuverType.fromApiString(stepDto.maneuver)
                    )
                },
                durationSeconds = legDto.duration.value,
                distanceMeters = legDto.distance.value,
                startLocation = LatLng(legDto.startLocation.lat, legDto.startLocation.lng),
                endLocation = LatLng(legDto.endLocation.lat, legDto.endLocation.lng),
                startAddress = legDto.startAddress,
                endAddress = legDto.endAddress
            )
        }

        val bounds = LatLngBounds(
            LatLng(this.bounds.southwest.lat, this.bounds.southwest.lng),
            LatLng(this.bounds.northeast.lat, this.bounds.northeast.lng)
        )

        val totalDistance = legs.sumOf { it.distanceMeters }
        val totalDuration = legs.sumOf { it.durationSeconds }

        return Route(
            overviewPolyline = this.overviewPolyline.points,
            legs = legs,
            bounds = bounds,
            totalDurationSeconds = totalDuration,
            totalDistanceMeters = totalDistance,
            waypoints = waypoints,
            summary = this.summary,
            warnings = this.warnings ?: emptyList(),
            copyrights = this.copyrights
        )
    }
}
