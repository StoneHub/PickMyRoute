# Google Directions API

## Overview
The Directions API calculates routes between locations with support for waypoints, traffic data, and multiple travel modes.

**Base URL:** `https://maps.googleapis.com/maps/api/directions/json`

## Key Features for Our App
- ✅ **Waypoint Support** - Force routes through specific points
- ✅ **Via Waypoints** - Pass through without stopping (`via:` prefix)
- ✅ **Turn-by-Turn Instructions** - Detailed navigation steps
- ✅ **Polyline Encoding** - Efficient route geometry
- ✅ **Multiple Routes** - Alternative route suggestions
- ✅ **Travel Time** - Duration estimates
- ✅ **Distance** - Total route distance

## Request Parameters

### Required Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| `origin` | String | Starting point (lat,lng or address) |
| `destination` | String | End point (lat,lng or address) |
| `key` | String | Your API key |

### Important Optional Parameters
| Parameter | Type | Description | Our Usage |
|-----------|------|-------------|-----------|
| `waypoints` | String | Pipe-separated waypoints | **Core feature** - user-selected roads |
| `alternatives` | Boolean | Return alternative routes | Future: suggest alternatives respecting waypoints |
| `avoid` | String | Features to avoid (tolls, highways, ferries) | User preferences |
| `departure_time` | Integer | Unix timestamp for traffic prediction | Real-time routing |
| `traffic_model` | String | Traffic prediction (best_guess, pessimistic, optimistic) | Enhanced routing |
| `units` | String | imperial or metric | User settings |

## Waypoint Syntax

### Standard Waypoint (Stop)
```
waypoints=41.8781,-87.6298|41.8840,-87.6340
```
This treats each point as a stop/destination.

### Via Waypoint (Pass Through) ⭐ **WE USE THIS**
```
waypoints=via:41.8781,-87.6298|via:41.8840,-87.6340
```
The `via:` prefix means "route through this point but don't treat it as a stop."
**Perfect for our "drive through this road" feature!**

### Optimization
```
waypoints=optimize:true|41.8781,-87.6298|41.8840,-87.6340
```
API will reorder waypoints for optimal route. **We won't use this** - we respect user's order.

## Example Request

### Our Use Case: Route with Custom Roads
```
GET https://maps.googleapis.com/maps/api/directions/json?
  origin=34.0522,-118.2437                          # User's current location (LA)
  &destination=34.1016,-118.3288                    # Destination (Sherman Oaks)
  &waypoints=via:34.0689,-118.2449|via:34.0834,-118.2691  # Roads user wants to drive
  &departure_time=now                               # Current traffic
  &traffic_model=best_guess
  &alternatives=false
  &units=imperial
  &key=YOUR_API_KEY
```

## Response Structure

### Top-Level Fields
```json
{
  "status": "OK",
  "routes": [
    {
      "summary": "I-405 N",
      "legs": [...],
      "overview_polyline": {
        "points": "encoded_polyline_string"
      },
      "bounds": {...},
      "waypoint_order": [0, 1]
    }
  ]
}
```

### Route Legs (Segments Between Waypoints)
```json
"legs": [
  {
    "distance": {
      "text": "5.2 mi",
      "value": 8369  // meters
    },
    "duration": {
      "text": "15 mins",
      "value": 900  // seconds
    },
    "steps": [
      {
        "html_instructions": "Turn <b>right</b> onto <b>Main St</b>",
        "distance": {...},
        "duration": {...},
        "start_location": {"lat": 34.0522, "lng": -118.2437},
        "end_location": {"lat": 34.0530, "lng": -118.2440},
        "polyline": {"points": "encoded_string"},
        "maneuver": "turn-right"
      }
    ]
  }
]
```

### Navigation Steps (Turn-by-Turn)
Each step includes:
- `html_instructions` - Human-readable directions (includes HTML tags)
- `maneuver` - Type of maneuver (turn-left, turn-right, merge, etc.)
- `distance` - Distance for this step
- `duration` - Time for this step
- `polyline` - Encoded path for this step
- `start_location` / `end_location` - Coordinates

## Maneuver Types
We'll map these to icons/voice commands:
- `turn-left`, `turn-right`
- `turn-slight-left`, `turn-slight-right`
- `turn-sharp-left`, `turn-sharp-right`
- `uturn-left`, `uturn-right`
- `merge`
- `ramp-left`, `ramp-right`
- `fork-left`, `fork-right`
- `roundabout-left`, `roundabout-right`
- `straight`
- `ferry`

## Status Codes
| Code | Meaning | Our Handling |
|------|---------|--------------|
| `OK` | Success | Display route |
| `NOT_FOUND` | Location not found | Show error, let user adjust |
| `ZERO_RESULTS` | No route possible | Alert user, suggest removing waypoints |
| `MAX_WAYPOINTS_EXCEEDED` | Too many waypoints (limit: 25) | Warn user before adding 26th |
| `INVALID_REQUEST` | Bad parameters | Log error, show generic message |
| `OVER_QUERY_LIMIT` | API quota exceeded | Cache routes, reduce requests |
| `REQUEST_DENIED` | API key issue | Alert developer |
| `UNKNOWN_ERROR` | Server error | Retry with exponential backoff |

## Polyline Encoding
Routes are encoded to save bandwidth. Use Google's polyline utility:
```kotlin
// Decode polyline
val path = PolyUtil.decode(encodedPolyline)
// Returns List<LatLng>
```

## Best Practices for Our App

### 1. **Minimize API Calls**
- Only recalculate when waypoints change
- Cache the current route
- Don't recalculate on every map pan/zoom

### 2. **Waypoint Ordering**
```kotlin
// Build waypoints string from user's ordered list
fun buildWaypointsParam(waypoints: List<Waypoint>): String {
    return waypoints
        .sortedBy { it.order }
        .joinToString("|") { "via:${it.location.latitude},${it.location.longitude}" }
}
```

### 3. **Handle Rerouting**
When user goes off-route:
```kotlin
// Remove waypoints already passed
val remainingWaypoints = allWaypoints.filter { it.order > lastPassedWaypointOrder }
// Recalculate from current location
recalculateRoute(
    origin = currentLocation,
    waypoints = remainingWaypoints,
    destination = finalDestination
)
```

### 4. **Avoid Backtracking**
The API generally avoids backtracking, but we can help:
- Order waypoints geographically along the route
- Detect if new waypoint would cause major backtracking
- Warn user: "This will add 20 minutes to your route"

## Rate Limits & Quotas

### Free Tier
- **40,000 requests/month FREE**
- Then $5 per 1,000 requests

### Request Optimization
Typical usage for one route:
- Initial route: 1 request
- Add waypoint 1: 1 request (recalculate)
- Add waypoint 2: 1 request (recalculate)
- Reorder waypoints: 1 request
- Go off-route (reroute): 1 request
- **Total: ~5 requests per trip**

For personal use, easily within free tier!

## Code Implementation

### Retrofit Interface
```kotlin
interface DirectionsApi {
    @GET("json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String? = null,
        @Query("departure_time") departureTime: String = "now",
        @Query("traffic_model") trafficModel: String = "best_guess",
        @Query("alternatives") alternatives: Boolean = false,
        @Query("units") units: String = "imperial",
        @Query("key") apiKey: String
    ): DirectionsResponse
}
```

### Usage Example
```kotlin
val response = directionsApi.getDirections(
    origin = "${currentLat},${currentLng}",
    destination = "${destLat},${destLng}",
    waypoints = "via:34.0689,-118.2449|via:34.0834,-118.2691",
    apiKey = BuildConfig.MAPS_API_KEY
)

if (response.status == "OK") {
    val route = response.routes.first()
    val polyline = route.overviewPolyline.points
    val decodedPath = PolyUtil.decode(polyline)
    // Draw on map
}
```

## Links
- [Official Documentation](https://developers.google.com/maps/documentation/directions)
- [Waypoints Documentation](https://developers.google.com/maps/documentation/directions/get-directions#Waypoints)
- [Polyline Encoding](https://developers.google.com/maps/documentation/utilities/polylinealgorithm)

