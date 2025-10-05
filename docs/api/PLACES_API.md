# Google Places API

## Overview
The Places API provides rich information about places, including businesses, landmarks, and geographic locations.

**Base URLs:**
- Place Autocomplete: `https://maps.googleapis.com/maps/api/place/autocomplete/json`
- Place Details: `https://maps.googleapis.com/maps/api/place/details/json`
- Nearby Search: `https://maps.googleapis.com/maps/api/place/nearbysearch/json`

## Key Features for Our App
- ⭐ **Autocomplete** - Smart destination search (like Google Maps)
- ✅ **Place Details** - Get coordinates from place selection
- ✅ **Photos** - Show place images (optional)
- ⚠️ **Nearby Search** - Find POIs near route (future enhancement)

## Status in Our MVP
**Phase 1:** ❌ Not needed - use simple coordinate input
**Phase 2:** ❌ Not needed - basic geocoding sufficient
**Phase 3+:** ✅ Add for enhanced destination search

## Why We'll Eventually Want This

### Current (Without Places API)
User destination input: Basic text field → Geocoding API
- Must type exact address
- No suggestions
- No POI search (e.g., "Starbucks")

### Future (With Places API)
User destination input: Autocomplete dropdown → Places API
- Type "starb" → See Starbucks suggestions
- Type "123 mai" → See "123 Main St" suggestions
- Select from dropdown → Get exact location
- **Much better UX** (exactly like Google Maps app)

## Places Autocomplete ⭐ **BEST FOR SEARCH**

### How It Works
As user types, get real-time suggestions:

```
GET https://maps.googleapis.com/maps/api/place/autocomplete/json
  ?input=123+main
  &location=34.0522,-118.2437
  &radius=50000
  &key=YOUR_API_KEY
```

**Response:**
```json
{
  "predictions": [
    {
      "description": "123 Main Street, Los Angeles, CA, USA",
      "place_id": "ChIJN1t_tDeuEmsRUsoyG83frY4",
      "structured_formatting": {
        "main_text": "123 Main Street",
        "secondary_text": "Los Angeles, CA, USA"
      },
      "types": ["street_address"]
    },
    {
      "description": "123 Main St, Santa Monica, CA, USA",
      "place_id": "ChIJ...",
      ...
    }
  ],
  "status": "OK"
}
```

### Key Parameters
| Parameter | Description | Our Usage |
|-----------|-------------|-----------|
| `input` | Search query | User's typed text |
| `location` | Bias results near this point | User's current location |
| `radius` | Search radius (meters) | 50km around user |
| `types` | Filter by type | "address" or "establishment" |
| `components` | Country restriction | "country:us" (optional) |
| `sessiontoken` | Group queries for billing | Cost optimization |

## Session Tokens (Important for Cost!)

Places Autocomplete charges per **session**, not per request:
- **Without session tokens:** Each keystroke = 1 API call = separate charge
- **With session tokens:** All keystrokes in one search = 1 session = 1 charge

```kotlin
class PlacesSearchSession {
    private var sessionToken: String = UUID.randomUUID().toString()
    
    suspend fun search(query: String): List<Prediction> {
        return placesApi.autocomplete(
            input = query,
            sessionToken = sessionToken,
            apiKey = apiKey
        ).predictions
    }
    
    fun selectPlace(placeId: String) {
        // Get place details with same token
        val details = placesApi.placeDetails(
            placeId = placeId,
            sessionToken = sessionToken,
            apiKey = apiKey
        )
        
        // End session (generate new token for next search)
        sessionToken = UUID.randomUUID().toString()
        
        return details
    }
}
```

## Place Details (Get Coordinates)

After user selects from autocomplete, get full details:

```
GET https://maps.googleapis.com/maps/api/place/details/json
  ?place_id=ChIJN1t_tDeuEmsRUsoyG83frY4
  &fields=geometry,name,formatted_address
  &key=YOUR_API_KEY
```

**Response:**
```json
{
  "result": {
    "geometry": {
      "location": {
        "lat": 34.0522,
        "lng": -118.2437
      }
    },
    "name": "123 Main Street",
    "formatted_address": "123 Main St, Los Angeles, CA 90012, USA"
  },
  "status": "OK"
}
```

### Field Masks (Cost Optimization)
Only request fields you need! Pricing varies by field category:

**Basic** (cheaper): name, formatted_address, geometry
**Contact** (medium): phone, website, opening_hours
**Atmosphere** (expensive): photos, reviews, ratings

For routing, we only need: `fields=geometry,name,formatted_address`

## Implementation Example

### Complete Destination Search Flow

```kotlin
@Composable
fun DestinationSearchBar(
    onDestinationSelected: (LatLng, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Prediction>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    val session = remember { PlacesSearchSession() }
    
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { newQuery ->
                query = newQuery
                if (newQuery.isNotEmpty()) {
                    scope.launch {
                        suggestions = session.search(newQuery)
                        expanded = true
                    }
                }
            },
            placeholder = { Text("Where to?") }
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { prediction ->
                DropdownMenuItem(
                    onClick = {
                        scope.launch {
                            val details = session.selectPlace(prediction.placeId)
                            onDestinationSelected(
                                details.geometry.location,
                                prediction.description
                            )
                            expanded = false
                        }
                    },
                    text = {
                        Column {
                            Text(
                                prediction.structuredFormatting.mainText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                prediction.structuredFormatting.secondaryText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }
    }
}
```

## Nearby Search (Future Enhancement)

Find points of interest along the route:

```
GET https://maps.googleapis.com/maps/api/place/nearbysearch/json
  ?location=34.0522,-118.2437
  &radius=1000
  &type=gas_station
  &key=YOUR_API_KEY
```

**Use Cases:**
- "Show gas stations along my route"
- "Find coffee shops near waypoint"
- "Add Starbucks as waypoint"

## Rate Limits & Costs

### Pricing (as of 2025)
**Autocomplete - Per Session:**
- Basic: $2.83 per 1,000 sessions
- Advanced: $8.50 per 1,000 sessions (if using "establishment" types)

**Place Details:**
- Basic: $17 per 1,000 requests (geometry, name, address)
- Contact: $3 per 1,000 (additional)
- Atmosphere: $5 per 1,000 (additional)

### Free Tier
- $200 monthly credit (covers ~7,000 autocomplete sessions)

### Cost Optimization
1. **Use session tokens** - Critical!
2. **Debounce input** - Wait 300ms after last keystroke
3. **Minimum characters** - Only search after 3+ characters
4. **Request minimal fields** - Only geometry for routing
5. **Cache results** - Store recent searches

## MVP Decision

### Phase 1-2: Skip Places API
**Use instead:**
- Simple text field for coordinates: "34.0522, -118.2437"
- Or basic Geocoding API for address: "123 Main St"

**Pros:**
- Faster development
- Lower cost
- Simpler implementation
- Still functional

**Cons:**
- Less user-friendly
- Must know exact address

### Phase 3+: Add Places Autocomplete
**When to add:**
- After core routing works
- When you want to polish UX
- To match Google Maps experience

## Code Implementation

### Retrofit Interface
```kotlin
interface PlacesApi {
    @GET("autocomplete/json")
    suspend fun autocomplete(
        @Query("input") input: String,
        @Query("sessiontoken") sessionToken: String,
        @Query("location") location: String? = null,
        @Query("radius") radius: Int? = null,
        @Query("types") types: String? = null,
        @Query("key") apiKey: String
    ): AutocompleteResponse
    
    @GET("details/json")
    suspend fun placeDetails(
        @Query("place_id") placeId: String,
        @Query("fields") fields: String = "geometry,name,formatted_address",
        @Query("sessiontoken") sessionToken: String,
        @Query("key") apiKey: String
    ): PlaceDetailsResponse
}
```

### Data Models
```kotlin
data class AutocompleteResponse(
    val predictions: List<Prediction>,
    val status: String
)

data class Prediction(
    val description: String,
    val placeId: String,
    val structuredFormatting: StructuredFormatting,
    val types: List<String>
)

data class StructuredFormatting(
    val mainText: String,
    val secondaryText: String
)

data class PlaceDetailsResponse(
    val result: PlaceDetails,
    val status: String
)

data class PlaceDetails(
    val geometry: Geometry,
    val name: String,
    val formattedAddress: String
)
```

## Comparison: Places vs Geocoding

| Feature | Places API | Geocoding API |
|---------|-----------|---------------|
| **Search suggestions** | ✅ Yes (autocomplete) | ❌ No |
| **POI search** | ✅ Yes ("Starbucks") | ❌ No |
| **Address search** | ✅ Yes | ✅ Yes |
| **Cost** | $$$ Higher | $ Lower |
| **UX** | ⭐⭐⭐⭐⭐ Excellent | ⭐⭐ Basic |
| **MVP Priority** | Low | Medium |

## Recommendation

**For MVP:** Start without Places API
```kotlin
// Simple destination input
@Composable
fun DestinationInput() {
    var destination by remember { mutableStateOf("") }
    
    OutlinedTextField(
        value = destination,
        onValueChange = { destination = it },
        label = { Text("Destination (address or coordinates)") },
        trailingIcon = {
            IconButton(onClick = {
                // Geocode the input
                viewModel.setDestination(destination)
            }) {
                Icon(Icons.Default.Search, "Search")
            }
        }
    )
}
```

**Phase 3:** Upgrade to Places Autocomplete for polished experience

## Links
- [Places API Overview](https://developers.google.com/maps/documentation/places/web-service/overview)
- [Autocomplete](https://developers.google.com/maps/documentation/places/web-service/autocomplete)
- [Place Details](https://developers.google.com/maps/documentation/places/web-service/details)
- [Session Tokens](https://developers.google.com/maps/documentation/places/web-service/session-tokens)
- [Pricing](https://developers.google.com/maps/billing-and-pricing/pricing#places)

