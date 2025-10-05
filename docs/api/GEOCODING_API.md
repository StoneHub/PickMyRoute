# Google Geocoding API

## Overview
The Geocoding API converts between addresses and geographic coordinates (lat/lng).

**Base URL:** `https://maps.googleapis.com/maps/api/geocode/json`

## Key Features for Our App
- ✅ **Address to Coordinates** - User enters destination address, we get lat/lng
- ✅ **Coordinates to Address** - Show human-readable location names
- ✅ **Road Names** - Get the name of a road from coordinates
- ✅ **Place IDs** - Convert place IDs to coordinates

## Use Cases in Our App

### 1. Destination Input
User types "123 Main St, Los Angeles" → Convert to coordinates for routing

### 2. Road Name Display
User taps road → We have coordinates → Get road name for timeline display

### 3. Waypoint Labels
Show "Main Street" instead of "34.0522, -118.2437" in the UI

### 4. Current Location Display
Show "You are on Highway 101" instead of raw coordinates

## Endpoints

### Forward Geocoding (Address → Coordinates)
```
GET https://maps.googleapis.com/maps/api/geocode/json
  ?address=1600+Amphitheatre+Parkway,+Mountain+View,+CA
  &key=YOUR_API_KEY
```

**Response:**
```json
{
  "results": [
    {
      "formatted_address": "1600 Amphitheatre Parkway, Mountain View, CA 94043, USA",
      "geometry": {
        "location": {
          "lat": 37.4224764,
          "lng": -122.0842499
        },
        "location_type": "ROOFTOP"
      },
      "place_id": "ChIJ2eUgeAK6j4ARbn5u_wAGqWA",
      "types": ["street_address"]
    }
  ],
  "status": "OK"
}
```

### Reverse Geocoding (Coordinates → Address) ⭐ **PRIMARY USE**
```
GET https://maps.googleapis.com/maps/api/geocode/json
  ?latlng=37.4224764,-122.0842499
  &result_type=route
  &key=YOUR_API_KEY
```

**Key Parameter:** `result_type=route` - Returns road names (not full addresses)

**Response:**
```json
{
  "results": [
    {
      "formatted_address": "Amphitheatre Parkway, Mountain View, CA",
      "address_components": [
        {
          "long_name": "Amphitheatre Parkway",
          "short_name": "Amphitheatre Pkwy",
          "types": ["route"]
        }
      ],
      "place_id": "ChIJ...",
      "types": ["route"]
    }
  ],
  "status": "OK"
}
```

### Place ID Lookup
```
GET https://maps.googleapis.com/maps/api/geocode/json
  ?place_id=ChIJ2eUgeAK6j4ARbn5u_wAGqWA
  &key=YOUR_API_KEY
```

## Result Types

Filter results to get what you need:

| Result Type | Description | Our Usage |
|-------------|-------------|-----------|
| `route` | Street/road name | **Waypoint labels** |
| `street_address` | Specific address | Destination input |
| `intersection` | Street intersection | Alternative waypoint label |
| `locality` | City | Not needed |
| `administrative_area` | State/province | Not needed |

## Location Types (Accuracy)

| Type | Meaning | Quality |
|------|---------|---------|
| `ROOFTOP` | Exact address | Excellent |
| `RANGE_INTERPOLATED` | Between two points | Good |
| `GEOMETRIC_CENTER` | Center of area (like city) | Poor for routing |
| `APPROXIMATE` | Rough location | Poor for routing |

**For routing:** Prefer `ROOFTOP` or `RANGE_INTERPOLATED`

## Implementation for Our App

### Get Road Name for Waypoint
```kotlin
suspend fun getRoadName(latLng: LatLng): String? {
    val response = geocodingApi.reverseGeocode(
        latlng = "${latLng.latitude},${latLng.longitude}",
        resultType = "route",
        apiKey = BuildConfig.MAPS_API_KEY
    )
    
    if (response.status == "OK" && response.results.isNotEmpty()) {
        val routeComponent = response.results.first()
            .addressComponents
            .firstOrNull { it.types.contains("route") }
        
        return routeComponent?.shortName ?: routeComponent?.longName
    }
    
    return null
}
```

### Destination Search (Simple)
```kotlin
suspend fun searchDestination(query: String): List<SearchResult> {
    val response = geocodingApi.geocode(
        address = query,
        apiKey = BuildConfig.MAPS_API_KEY
    )
    
    return response.results.map { result ->
        SearchResult(
            address = result.formattedAddress,
            location = LatLng(
                result.geometry.location.lat,
                result.geometry.location.lng
            ),
            placeId = result.placeId
        )
    }
}
```

## Response Status Codes

| Status | Meaning | Our Handling |
|--------|---------|--------------|
| `OK` | Success | Use results |
| `ZERO_RESULTS` | No results found | Show "Location not found" |
| `OVER_QUERY_LIMIT` | Quota exceeded | Cache results, rate limit |
| `REQUEST_DENIED` | API key problem | Alert developer |
| `INVALID_REQUEST` | Missing parameters | Log error |
| `UNKNOWN_ERROR` | Server error | Retry |

## Caching Strategy

**Important:** Cache geocoding results to save API calls!

```kotlin
class GeocodingCache {
    private val cache = LruCache<LatLng, String>(100)
    
    suspend fun getRoadName(latLng: LatLng): String? {
        // Round to ~10m precision to improve cache hits
        val key = latLng.round(4)
        
        return cache[key] ?: run {
            val name = geocodingApi.getRoadName(latLng)
            name?.let { cache.put(key, it) }
            name
        }
    }
}

fun LatLng.round(decimals: Int): LatLng {
    val factor = 10.0.pow(decimals)
    return LatLng(
        (latitude * factor).roundToInt() / factor,
        (longitude * factor).roundToInt() / factor
    )
}
```

## Rate Limits & Costs

### Pricing
- **Geocoding:** $5 per 1,000 requests (after free tier)
- **Free Tier:** Check current Google Maps Platform limits

### Optimization Tips
1. **Cache aggressively** - Road names don't change
2. **Batch requests** - Get multiple road names at once (not directly supported, but cache helps)
3. **Debounce** - Don't geocode every map movement
4. **Optional feature** - Make road names optional if quota is reached

## Code Implementation

### Retrofit Interface
```kotlin
interface GeocodingApi {
    @GET("json")
    suspend fun geocode(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): GeocodingResponse
    
    @GET("json")
    suspend fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("result_type") resultType: String? = null,
        @Query("key") apiKey: String
    ): GeocodingResponse
    
    @GET("json")
    suspend fun placeIdLookup(
        @Query("place_id") placeId: String,
        @Query("key") apiKey: String
    ): GeocodingResponse
}
```

### Data Models
```kotlin
data class GeocodingResponse(
    val results: List<GeocodingResult>,
    val status: String
)

data class GeocodingResult(
    val formattedAddress: String,
    val addressComponents: List<AddressComponent>,
    val geometry: Geometry,
    val placeId: String,
    val types: List<String>
)

data class AddressComponent(
    val longName: String,
    val shortName: String,
    val types: List<String>
)

data class Geometry(
    val location: LatLngDto,
    val locationType: String
)

data class LatLngDto(
    val lat: Double,
    val lng: Double
)
```

## UI Integration

### Waypoint Timeline Display
```kotlin
// Before geocoding
[📍 Waypoint 1]

// After geocoding
[📍 Main St]

// If geocoding fails
[📍 34.05°, -118.24°]
```

### Loading States
```kotlin
@Composable
fun WaypointChip(waypoint: Waypoint) {
    val roadName by viewModel.getRoadName(waypoint.location)
        .collectAsState(initial = null)
    
    FilterChip(
        selected = true,
        onClick = { viewModel.removeWaypoint(waypoint) },
        label = {
            Text(roadName ?: "Loading...")
        }
    )
}
```

## MVP Recommendations

### Phase 1: Skip It
- Show coordinates instead of road names
- Focus on core routing functionality
- Example: "Waypoint at 34.05°, -118.24°"

### Phase 2: Add Road Names
- Geocode waypoints in background
- Cache results
- Enhance UI with readable names

### Phase 3: Destination Search
- Add search bar with autocomplete
- Use Places API Autocomplete (better than Geocoding for search)

## Alternative: Places API

For destination search, **Places API Autocomplete** is better:
- Real-time suggestions as user types
- Better UX than basic geocoding
- More expensive but worth it for search

**Decision:** Use **Geocoding** for waypoint labels, **Places API** for destination search (future)

## Links
- [Official Documentation](https://developers.google.com/maps/documentation/geocoding)
- [Reverse Geocoding](https://developers.google.com/maps/documentation/geocoding/requests-reverse-geocoding)
- [Address Component Types](https://developers.google.com/maps/documentation/geocoding/requests-geocoding#Types)

