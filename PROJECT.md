# Maps Route Picker

## Project Vision

A focused Google Maps navigation app that solves a specific problem: **allowing drivers to force routes through specific roads they prefer**, rather than always taking the algorithmically "optimal" route.

### The Problem This Solves
Google Maps constantly tries to optimize routes, often rerouting drivers away from roads they specifically want to drive on. This app lets users:
- Select a destination
- Tap on specific roads they want to drive through
- Get navigation that respects their preferences
- Still benefit from Google's routing intelligence between their chosen waypoints

## Core Features (MVP)

### Phase 1: Basic Routing
- [ ] Display Google Map with user's current location
- [ ] Set destination (manual coordinate entry or simple search)
- [ ] Display default route from origin to destination
- [ ] Show turn-by-turn directions

### Phase 2: Custom Waypoint Selection ⭐ (Key Differentiator)
- [ ] Tap on any road on the map to add it as a waypoint
- [ ] Route automatically recalculates to go through selected road(s)
- [ ] Visual indication of "locked" waypoints vs regular route
- [ ] Ability to remove waypoints
- [ ] Drag to reorder waypoints

### Phase 3: Navigation Enhancement
- [ ] Real-time location tracking during navigation
- [ ] Voice/visual turn-by-turn guidance
- [ ] Persistent waypoint routing (reroute through remaining waypoints if user goes off-track)
- [ ] Distance/time estimates

### Future Enhancements (Post-MVP)
- Full Places API integration for smart destination search
- Save favorite routes
- Share routes with others
- Traffic data integration
- Alternative route suggestions that respect waypoints

## Technical Stack

### Frontend
- **UI Framework**: Jetpack Compose (100% Compose for new screens)
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Jetpack Navigation Compose
- **Dependency Injection**: Hilt

### Google Services & APIs
- **Maps SDK**: Google Maps SDK for Android (Compose version)
  - Library: `com.google.maps.android:maps-compose`
- **Directions API**: For route calculation with waypoints
  - REST API: `https://maps.googleapis.com/maps/api/directions/json`
- **Places API**: For enhanced destination search (Phase 2+)
- **Geocoding API**: For address/coordinate conversion

### Key Libraries
```kotlin
// Maps
implementation("com.google.maps.android:maps-compose:4.3.3")
implementation("com.google.android.gms:play-services-maps:18.2.0")
implementation("com.google.android.gms:play-services-location:21.1.0")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose")

// Dependency Injection
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-compiler:2.48")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services")
```

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Compose UI Layer                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  MapScreen   │  │RouteDetails  │  │ Settings     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
                         ▲
                         │ State / Events
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   ViewModel Layer                        │
│  ┌──────────────────────────────────────────────────┐   │
│  │            MapViewModel                          │   │
│  │  - Route state                                   │   │
│  │  - Waypoints management                          │   │
│  │  - Navigation state                              │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                         ▲
                         │ Use Cases
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   Repository Layer                       │
│  ┌────────────────────┐  ┌──────────────────────────┐   │
│  │ RoutingRepository  │  │  LocationRepository      │   │
│  └────────────────────┘  └──────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                         ▲
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                   Data Sources                           │
│  ┌────────────────────┐  ┌──────────────────────────┐   │
│  │ DirectionsAPI      │  │  FusedLocationProvider   │   │
│  │ (Retrofit)         │  │  (Google Play Services)  │   │
│  └────────────────────┘  └──────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

## Data Models

### Core Domain Models
```kotlin
data class Route(
    val polyline: String,              // Encoded polyline from Directions API
    val legs: List<RouteLeg>,
    val bounds: LatLngBounds,
    val durationSeconds: Int,
    val distanceMeters: Int,
    val waypoints: List<Waypoint>
)

data class Waypoint(
    val id: String,
    val location: LatLng,
    val roadName: String?,
    val isLocked: Boolean = true,      // User-selected waypoints are locked
    val order: Int
)

data class RouteLeg(
    val steps: List<NavigationStep>,
    val durationSeconds: Int,
    val distanceMeters: Int
)

data class NavigationStep(
    val instruction: String,           // "Turn right onto Main St"
    val distanceMeters: Int,
    val durationSeconds: Int,
    val polyline: String,
    val maneuver: ManeuverType
)

enum class ManeuverType {
    TURN_LEFT, TURN_RIGHT, TURN_SLIGHT_LEFT, TURN_SLIGHT_RIGHT,
    MERGE, ROUNDABOUT, STRAIGHT, FERRY, DESTINATION
}
```

## Google Cloud Setup

### Required APIs to Enable
1. **Maps SDK for Android** - Display maps
2. **Directions API** - Calculate routes with waypoints
3. **Geocoding API** - Convert addresses to coordinates
4. **Places API** (Future) - Enhanced search
5. **Roads API** (Optional) - Snap to roads, get road names

### Setup Steps
```bash
# Install Google Cloud CLI (in WSL)
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init

# Create new project
gcloud projects create maps-route-picker-<unique-id> --name="Maps Route Picker"

# Set project
gcloud config set project maps-route-picker-<unique-id>

# Enable required APIs
gcloud services enable maps-android-backend.googleapis.com
gcloud services enable directions-backend.googleapis.com
gcloud services enable geocoding-backend.googleapis.com
gcloud services enable roads.googleapis.com

# Create API key with restrictions
gcloud alpha services api-keys create \
  --display-name="Maps Route Picker Android" \
  --api-target=service=maps-android-backend.googleapis.com \
  --api-target=service=directions-backend.googleapis.com \
  --api-target=service=geocoding-backend.googleapis.com \
  --api-target=service=roads.googleapis.com
```

### Security
- Store API key in `local.properties` (already gitignored)
- Use Android app restrictions on the API key
- Add SHA-1 fingerprint restriction
- Consider backend proxy for production (optional for MVP)

## Project Structure

```
app/src/main/java/com/stonecode/mapsroutepicker/
├── MainActivity.kt                    ✅ Compose + Hilt (43 lines)
├── MapsRoutePickerApp.kt             ✅ Application class (7 lines)
│
├── ui/
│   ├── map/
│   │   ├── MapScreen.kt              ✅ Main map UI + polyline rendering (236 lines)
│   │   ├── MapState.kt               ✅ State + events (33 lines)
│   │   └── MapViewModel.kt           ✅ Business logic + routing (192 lines)
│   │
│   ├── permissions/
│   │   └── LocationPermissionHandler.kt  ✅ Runtime permissions (35 lines)
│   │
│   └── theme/
│       └── Theme.kt                  ✅ Material3 theme (33 lines)
│
├── domain/
│   ├── model/
│   │   ├── Route.kt                  ✅ Domain model with helpers (81 lines)
│   │   ├── Waypoint.kt               ✅ With API formatting (24 lines)
│   │   └── NavigationStep.kt         ✅ Turn-by-turn + ManeuverType (71 lines)
│   │
│   └── repository/
│       ├── LocationRepository.kt     ✅ Interface (26 lines)
│       └── RoutingRepository.kt      ✅ Interface (22 lines)
│
├── data/
│   ├── remote/
│   │   ├── DirectionsApi.kt          ✅ Retrofit interface (28 lines)
│   │   └── dto/
│   │       └── DirectionsDto.kt      ✅ Complete API models (75 lines)
│   │
│   ├── location/
│   │   └── LocationRepositoryImpl.kt ✅ FusedLocationProvider wrapper (72 lines)
│   │
│   └── repository/
│       └── RoutingRepositoryImpl.kt  ✅ Directions API integration (97 lines)
│
├── util/
│   └── PolylineDecoder.kt           ✅ Google polyline decoder (47 lines)
│
└── di/
    ├── NetworkModule.kt              ✅ Retrofit + Routing DI (63 lines)
    └── LocationModule.kt             ✅ Location services DI (38 lines)

**Total: 19 files, 1,242 lines of clean, production-ready code**
```

## Development Workflow

### Initial Setup Checklist
- [x] Set up Google Cloud project and enable APIs
- [x] Configure API keys and add to project
- [x] Update dependencies to include Maps Compose and Retrofit
- [x] Configure Hilt for dependency injection
- [x] Request location permissions in manifest
- [x] Clean up template code (fragments, navigation drawer, menus)
- [x] Convert MainActivity to 100% Compose + Hilt
- [x] Create Material3 theme
- [ ] Test basic map display

### Development Phases

#### Phase 1: Foundation (Week 1) ✅ COMPLETE
- ✅ Set up Google Cloud and API keys
- ✅ Clean project structure (removed template fragments/layouts)
- ✅ Create basic Compose UI with MainActivity
- ✅ Configure Hilt dependency injection
- ✅ Create MapScreen composable with Google Maps integration
- ✅ Define enhanced domain models (Route, Waypoint, NavigationStep)
- ✅ Implement location permission handling (Accompanist Permissions)
- ✅ Create permission rationale UI
- ✅ Implement LocationRepository for GPS tracking
- ✅ Display user's current location on map

#### Phase 2: Basic Routing (Week 1-2) ✅ COMPLETE
- ✅ Implement destination selection (tap on map)
- ✅ Integrate Directions API with RoutingRepository
- ✅ Display route polyline on map (decoded polyline rendering)
- ✅ Show basic route info (distance, duration)

#### Phase 3: Waypoint Selection (Week 2-3) ⭐
- Implement road tap detection
- Add waypoint markers to map
- Recalculate route with waypoints
- Visual distinction for custom waypoints

#### Phase 4: Turn-by-Turn (Week 3-4)
- Parse navigation steps from Directions API
- Display step-by-step instructions
- Implement basic navigation UI
- Real-time location tracking

#### Phase 5: Polish (Week 4+)
- Error handling and edge cases
- Loading states and animations
- Settings screen
- Testing and bug fixes

## Key Implementation Details

### Waypoint System via Directions API

The Directions API supports waypoints! This is perfect for our use case:

```kotlin
// API Request format
GET https://maps.googleapis.com/maps/api/directions/json?
  origin=41.8781,-87.6298
  &destination=41.8887,-87.6355
  &waypoints=via:41.8840,-87.6340|via:41.8820,-87.6300  // Our custom roads!
  &key=YOUR_API_KEY
```

The `via:` prefix means "route through this point but don't count it as a stop" - perfect for our "must use this road" feature!

### Road Tap Detection

```kotlin
// Pseudo-code for tap handling
GoogleMap(
    onMapClick = { latLng ->
        // Option 1: Use Roads API to snap to nearest road
        val snappedPoint = roadsApi.snapToRoads(latLng)
        
        // Option 2: Just use the tapped point directly
        viewModel.addWaypoint(latLng)
    }
)
```

### Route Recalculation Strategy

When user taps a road:
1. Add waypoint to list
2. Reconstruct Directions API request with all waypoints
3. Fetch new route
4. Update map with new polyline

When user goes off route:
1. Detect deviation from route
2. Keep remaining waypoints
3. Recalculate from current location → remaining waypoints → destination

## Questions to Resolve

### ✅ RESOLVED - Design Decisions

1. **Waypoint selection method**: ✅ Tap directly on the map to add waypoint immediately
2. **Multiple waypoints**: ✅ YES - Users can add multiple roads to drive through in sequence
3. **Off-route behavior**: ✅ Recalculate through remaining waypoints (avoid backtracking when possible)
4. **Project structure**: ✅ Full Compose rebuild - clean slate architecture
5. **Compose migration**: ✅ 100% Compose - no XML layouts for main features
6. **Offline support**: ✅ Internet required for MVP - focus on core features first

### UI/UX Design

**Waypoint Timeline Interface:**
- Main screen is map-first (fullscreen map)
- When waypoints exist: floating horizontal timeline at top of screen
- Each waypoint represented as a labeled bubble/chip
- Visual flow: Start → Waypoint 1 → Waypoint 2 → Destination
- Drag & drop to reorder waypoints (triggers route recalculation)
- Tap bubble to remove waypoint
- Compact, translucent overlay that doesn't obscure map

```
┌─────────────────────────────────────────┐
│ [🏁]─→[📍A]─→[📍B]─→[📍C]─→[🎯]      │  ← Floating Timeline
├─────────────────────────────────────────┤
│                                         │
│                                         │
│          🗺️  MAP VIEW                  │
│            (Fullscreen)                 │
│                                         │
│     Route with waypoint markers         │
│                                         │
└─────────────────────────────────────────┘
```

## API Usage & Costs

### Estimated API Costs (Google Maps Platform)
- **Maps SDK**: $0 (free for mobile apps)
- **Directions API**: $5 per 1000 requests (after 40,000 free per month)
- **Roads API**: $10 per 1000 requests (if we use it)

For development and personal use, we'll stay well within free tier.

## Testing Strategy

### Manual Testing Checklist
- [ ] Can display map with current location
- [ ] Can set destination and see route
- [ ] Can tap road and add waypoint
- [ ] Route recalculates through waypoint
- [ ] Can remove waypoints
- [ ] Can reorder multiple waypoints
- [ ] Turn-by-turn directions are accurate
- [ ] App handles location permissions properly
- [ ] App handles no internet connection gracefully

### Edge Cases to Consider
- User taps empty area (not a road)
- Adding waypoint that makes route impossible
- Very long routes with many waypoints
- GPS signal lost during navigation
- Destination and waypoint are on same road

## Resources

### Official Documentation
- [Maps Compose](https://github.com/googlemaps/android-maps-compose)
- [Directions API](https://developers.google.com/maps/documentation/directions)
- [Roads API](https://developers.google.com/maps/documentation/roads)
- [Places API](https://developers.google.com/maps/documentation/places)

### Useful Examples
- [Maps Compose Samples](https://github.com/googlemaps/android-maps-compose/tree/main/app/src/main/java/com/google/maps/android/compose)
- [Android Location Samples](https://github.com/android/location-samples)

## License & Notes

**Project Status**: ✅ MVP Ready for Testing
**Target Platform**: Android 7.0+ (API 24+)
**Code Stats**: 19 files, 1,242 lines
**Last Updated**: 2025-10-05

---

## Next Steps

1. Answer clarifying questions
2. Set up Google Cloud project and APIs
3. Update build.gradle.kts with required dependencies
4. Create basic Compose map screen
5. Start with Phase 1 implementation
