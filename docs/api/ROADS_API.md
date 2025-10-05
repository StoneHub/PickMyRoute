# Google Roads API

## Overview
The Roads API provides services to snap GPS points to roads, find nearest roads, and get speed limits.

**Base URL:** `https://roads.googleapis.com/v1`

## Key Features for Our App
- ✅ **Snap to Roads** - Convert tap location to actual road coordinates
- ✅ **Nearest Roads** - Find roads near a tapped point
- ✅ **Road Names** - Get the name of the road
- ⚠️ **Speed Limits** - Available but requires separate billing (not MVP)

## Why We Need This
When user taps the map to select a road:
1. User taps near a road (might be slightly off)
2. Roads API snaps the point to the actual road centerline
3. We get the road's precise coordinates and name
4. Creates a better waypoint for routing

**Without Roads API:** User might tap slightly off the road, creating invalid waypoint
**With Roads API:** We guarantee the waypoint is on an actual road

## Endpoints

### 1. Snap to Roads ⭐ **PRIMARY USE**
Snaps GPS coordinates to the most likely road.

**Endpoint:** `/snapToRoads`

**Use Case:** User taps map → We snap to nearest road

```
GET https://roads.googleapis.com/v1/snapToRoads
  ?path=34.0522,-118.2437
  &interpolate=false
  &key=YOUR_API_KEY
```

**Parameters:**
| Parameter | Required | Description |
|-----------|----------|-------------|
| `path` | Yes | Latitude,longitude (can be multiple points) |
| `interpolate` | No | Fill in gaps between points (false for single tap) |
| `key` | Yes | API key |

**Response:**
```json
{
  "snappedPoints": [
    {
      "location": {
        "latitude": 34.05229,
        "longitude": -118.24371
      },
      "originalIndex": 0,
      "placeId": "ChIJN1t_tDeuEmsRUsoyG83frY4"  // Unique road identifier
    }
  ]
}
```

### 2. Nearest Roads
Find roads near a point (alternative to snap to roads).

**Endpoint:** `/nearestRoads`

**Use Case:** Show user a list of nearby roads to choose from

```
GET https://roads.googleapis.com/v1/nearestRoads
  ?points=34.0522,-118.2437
  &key=YOUR_API_KEY
```

**Response:**
```json
{
  "snappedPoints": [
    {
      "location": {...},
      "placeId": "ChIJ..."
    }
  ]
}
```

### 3. Speed Limits (Future Enhancement)
Get speed limit data for roads.

**Note:** Requires separate billing/approval from Google. **Skip for MVP.**

## Implementation Strategy

### Option A: Snap Every Tap (Recommended)
**Flow:**
1. User taps map at (34.0522, -118.2437)
2. Call Roads API to snap to road
3. Add snapped coordinate as waypoint
4. Use placeId to get road name (via Geocoding API)
5. Display road name in waypoint timeline

**Pros:**
- Most accurate waypoints
- Better user experience
- Prevents invalid routes

**Cons:**
- Extra API call per waypoint
- Slightly slower (network latency)

### Option B: Direct Tap (Faster, Less Accurate)
**Flow:**
1. User taps map at (34.0522, -118.2437)
2. Immediately add as waypoint (no API call)
3. Directions API will try to route through that point

**Pros:**
- Instant feedback
- No extra API calls
- Simpler implementation

**Cons:**
- May create waypoints slightly off road
- Could produce weird routes

### Recommendation: **Hybrid Approach**
1. Add waypoint immediately (instant feedback)
2. Snap to road in background
3. Update waypoint with snapped location
4. If snap fails, keep original point

```kotlin
// User taps map
onMapClick { latLng ->
    // Add waypoint immediately (optimistic)
    val tempWaypoint = Waypoint(
        id = UUID.randomUUID().toString(),
        location = latLng,
        roadName = "Loading...",
        order = currentWaypoints.size
    )
    addWaypoint(tempWaypoint)
    
    // Snap in background
    launch {
        val snapped = roadsApi.snapToRoads(latLng)
        if (snapped != null) {
            updateWaypoint(tempWaypoint.copy(
                location = snapped.location,
                placeId = snapped.placeId
            ))
            // Optionally fetch road name
            val roadName = geocodingApi.getRoadName(snapped.placeId)
            updateWaypoint(tempWaypoint.copy(roadName = roadName))
        }
    }
}
```

## Getting Road Names

Roads API returns `placeId` but not the road name. To get the name:

### Method 1: Geocoding API (Reverse Geocode)
```
GET https://maps.googleapis.com/maps/api/geocode/json
  ?latlng=34.0522,-118.2437
  &result_type=route
  &key=YOUR_API_KEY
```

Response includes:
```json
{
  "results": [{
    "formatted_address": "Main Street, Los Angeles, CA",
    "address_components": [{
      "long_name": "Main Street",
      "types": ["route"]
    }]
  }]
}
```

### Method 2: Places API (Place Details)
```
GET https://maps.googleapis.com/maps/api/place/details/json
  ?place_id=ChIJN1t_tDeuEmsRUsoyG83frY4
  &fields=name
  &key=YOUR_API_KEY
```

**Recommendation:** Use **Geocoding API** - simpler and cheaper.

## Rate Limits & Costs

### Free Tier
Roads API is bundled with other pricing. Check current Google Maps Platform pricing.

### Cost Optimization
- Only snap waypoints, not every GPS update
- Cache snapped results
- Consider direct tap for MVP, add snapping later

## Code Implementation

### Retrofit Interface
```kotlin
interface RoadsApi {
    @GET("snapToRoads")
    suspend fun snapToRoads(
        @Query("path") path: String,  // "lat,lng"
        @Query("interpolate") interpolate: Boolean = false,
        @Query("key") apiKey: String
    ): SnapToRoadsResponse
    
    @GET("nearestRoads")
    suspend fun nearestRoads(
        @Query("points") points: String,
        @Query("key") apiKey: String
    ): NearestRoadsResponse
}
```

### Data Models
```kotlin
data class SnapToRoadsResponse(
    val snappedPoints: List<SnappedPoint>
)

data class SnappedPoint(
    val location: Location,
    val originalIndex: Int?,
    val placeId: String
)

data class Location(
    val latitude: Double,
    val longitude: Double
)
```

### Usage Example
```kotlin
suspend fun snapToNearestRoad(latLng: LatLng): SnappedPoint? {
    return try {
        val response = roadsApi.snapToRoads(
            path = "${latLng.latitude},${latLng.longitude}",
            apiKey = BuildConfig.MAPS_API_KEY
        )
        response.snappedPoints.firstOrNull()
    } catch (e: Exception) {
        Log.e("RoadsApi", "Failed to snap to road", e)
        null
    }
}
```

## UI Feedback

### Visual Feedback
When snapping to road:
1. Show temporary marker at tap location
2. Animate marker moving to snapped location
3. Slight haptic feedback when snapped

### Timeline Display
```
Before snap: [📍 Waypoint 1] (Loading...)
After snap:  [📍 Main Street]
```

## Decision for MVP

**Recommendation:** Start **without** Roads API for MVP
- Users tap roads directly
- Faster implementation
- Still functional
- Add Roads API in Phase 2 for polish

**If user taps off-road:** Directions API will fail gracefully with "ZERO_RESULTS"
- We can detect this and show: "Try tapping on a road"

## Links
- [Official Documentation](https://developers.google.com/maps/documentation/roads)
- [Snap to Roads](https://developers.google.com/maps/documentation/roads/snap)
- [Nearest Roads](https://developers.google.com/maps/documentation/roads/nearest)

