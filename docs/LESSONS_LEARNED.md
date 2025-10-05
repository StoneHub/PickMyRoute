# Lessons Learned - Maps Route Picker

## Development Insights (2025-10-05)

### What Worked Well ✅

#### 1. Clean Slate Approach
**Decision:** Remove all template code before building features
**Result:** Zero confusion about what's active vs legacy code
**Lesson:** Always clean up scaffolding before building. Template code creates cognitive overhead.

#### 2. Domain Models First
**Decision:** Build Route/Waypoint/NavigationStep models before API integration
**Result:** No rework needed when API responses came in - DTOs mapped cleanly to domain
**Lesson:** Define your domain model based on business requirements, not API shape. Let the data layer adapt.

#### 3. Vertical Slices
**Decision:** Build complete features end-to-end (UI → ViewModel → Repository → API)
**Result:** Integration issues surfaced early
**Lesson:** Horizontal layers (all models, then all repos, then all UI) hide problems until the end.

#### 4. Minimal Dependencies
**Decision:** Implement polyline decoder vs adding a library
**Result:** 47 lines, zero external dependency, complete control
**Lesson:** Don't reach for a library when the algorithm is simple. You own it and understand it.

#### 5. Flow-Based Location Updates
**Decision:** Use Kotlin Flow instead of callbacks for location
**Result:** Clean, cancellable, testable streams
**Lesson:** Flow is the right abstraction for continuous data streams. No callback hell.

---

## What We'd Do Differently 🔄

### Critical Issues (Pre-Launch)

#### 1. ⚠️ Test Build Earlier
**Mistake:** Wrote all code without compiling
**Risk:**
- Gradle dependency conflicts
- Hilt annotation processing failures
- BuildConfig.MAPS_API_KEY visibility issues
- Maps Compose version mismatches

**Fix:** After every major component (domain models, location, routing), run `./gradlew build`

**Lesson:** "It compiles in my head" is not the same as "it compiles in Gradle."

#### 2. ⚠️ Camera Doesn't Follow Route
**Current:** User manually zooms to see route after calculation
**Should:** Auto-zoom camera to fit route bounds

**Implementation:**
```kotlin
LaunchedEffect(state.route) {
    state.route?.let { route ->
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngBounds(route.bounds, 100)
        )
    }
}
```

**Lesson:** Users expect the camera to "do the right thing." Manual pan/zoom is a UX failure.

#### 3. ⚠️ No User Guidance
**Current:** Nothing tells user what to tap or when
**Should:** Progressive hints
- On first load: "Tap map to set destination"
- After destination: "Tap roads to add waypoints"
- Long-press: "Hold to add multiple waypoints"

**Lesson:** Gestural interfaces need on-screen hints. Not everyone reads docs.

#### 4. ⚠️ Waypoint Markers All Look the Same
**Current:** All markers are red pins
**Should:** Visual hierarchy
- Blue numbered circles for waypoints (1, 2, 3...)
- Green pin for destination
- Start flag for origin

**Lesson:** Visual distinction isn't decoration - it's information architecture.

#### 5. ⚠️ Error Messages Are Generic
**Current:** `catch (e: Exception)` → "Failed to calculate route"
**Should:** Specific, actionable messages
- `UnknownHostException` → "No internet connection. Check WiFi/data."
- `SocketTimeoutException` → "Server not responding. Try again."
- `HTTP 403` → "Invalid API key. Check configuration."
- `HTTP 429` → "API quota exceeded. Try again in 1 minute."

**Implementation:**
```kotlin
catch (e: Exception) {
    val message = when (e) {
        is UnknownHostException -> "No internet connection"
        is SocketTimeoutException -> "Request timed out"
        is HttpException -> when (e.code()) {
            403 -> "Invalid API key"
            429 -> "API quota exceeded"
            else -> "Server error: ${e.code()}"
        }
        else -> "Error: ${e.message}"
    }
    _state.update { it.copy(error = message, isLoading = false) }
}
```

**Lesson:** Generic error messages frustrate users. Be specific and actionable.

---

### UX Polish (Post-Launch)

#### 6. Loading Indicator Too Subtle
**Current:** Small spinner over map, easy to miss
**Better:** Modal overlay with card

```kotlin
if (state.isLoading) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Card {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Calculating route...")
            }
        }
    }
}
```

**Lesson:** Loading states should demand attention, not blend into the background.

#### 7. Error Snackbar Auto-Dismisses
**Current:** Error shows briefly, then disappears
**Better:** Persistent card with dismiss button and retry

```kotlin
state.error?.let { error ->
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null)
            Spacer(Modifier.width(8.dp))
            Text(error, Modifier.weight(1f))
            TextButton(onClick = { /* retry */ }) { Text("Retry") }
            IconButton(onClick = { viewModel.onEvent(MapEvent.DismissError) }) {
                Icon(Icons.Default.Close, "Dismiss")
            }
        }
    }
}
```

**Lesson:** Errors should be dismissible by the user, not by a timer.

#### 8. No "Clear Route" Button
**Current:** Can change destination but not clear it
**Should:** Floating action button or menu option

**Lesson:** Every action needs an undo/clear option.

#### 9. Waypoint Timeline Has No Interaction
**Current:** Shows waypoints as chips, but can't tap to remove or drag to reorder
**Should:**
- Tap chip → remove waypoint
- Long-press + drag → reorder
- Swipe chip → delete

**Lesson:** If it looks interactive, it should be interactive.

---

## Architecture Decisions

### What's Right ✅

1. **Repository Pattern** - Clean boundary between data sources and domain
2. **Flow for Streams** - Location updates are elegant and cancellable
3. **Sealed Classes for Events** - Type-safe, exhaustive handling
4. **Hilt DI** - No manual wiring, everything scoped correctly
5. **DTO → Domain Mapping** - API changes don't break domain logic

### What Could Be Better 🤔

1. **No UseCase Layer** - ViewModels call repositories directly
   - **Pro:** Less boilerplate for simple operations
   - **Con:** Business logic could get mixed with presentation
   - **Verdict:** Fine for MVP, refactor when ViewModels hit 300+ lines

2. **No Local Caching** - Every route request hits the network
   - **Pro:** Always fresh data
   - **Con:** Wastes API quota on repeated requests
   - **Future:** Add Room database for route caching

3. **Single MapState** - Everything in one data class
   - **Pro:** Easy to reason about
   - **Con:** Could grow unwieldy with more features
   - **Future:** Split into PermissionState, LocationState, RouteState

---

## Testing Strategy (What's Missing)

### Unit Tests We Should Write
```kotlin
// Domain models
RouteTest - verify getFormattedDistance/Duration
WaypointTest - verify toDirectionsApiFormat()

// Repositories
RoutingRepositoryTest - mock DirectionsApi, verify DTO mapping
LocationRepositoryTest - mock FusedLocationClient

// ViewModels
MapViewModelTest - verify state transitions
```

### Integration Tests
```kotlin
// API integration
DirectionsApiIntegrationTest - real network calls (CI only)

// UI tests
MapScreenTest - Compose UI testing with fake ViewModel
```

**Lesson:** Write tests as you build, not at the end. Test-after is test-never.

---

## Performance Considerations

### What Could Be Slow

1. **Polyline Decoding on Main Thread**
   - 1000+ points could block UI
   - **Fix:** `withContext(Dispatchers.Default)` for decoding

2. **No Debouncing on Waypoint Changes**
   - Every tap triggers new API call
   - **Fix:** Debounce waypoint additions by 500ms

3. **Location Updates Every 5 Seconds**
   - Battery drain during navigation
   - **Fix:** Lower frequency when not navigating actively

---

## Security & Best Practices

### What's Good ✅
- API key in `local.properties` (gitignored)
- Permissions requested with rationale
- HTTPS for all network calls

### What's Missing ⚠️
- No ProGuard/R8 rules for production
- No certificate pinning (overkill for MVP)
- API key visible in BuildConfig (consider backend proxy for production)

---

## Key Takeaways

### For Future Projects

1. **Build → Test → Polish** cycle should be hours, not days
2. **Show the user what's happening** at every step (loading, error, success)
3. **Fail gracefully** with specific, actionable error messages
4. **Visual hierarchy matters** - make important things look important
5. **Architecture is for humans** - if it's hard to explain, it's probably over-engineered

### Specific to This Project

- ✅ Clean domain models make everything downstream easier
- ✅ Minimal dependencies = less maintenance burden
- ⚠️ Need to test on real device before declaring "done"
- ⚠️ UX polish is not optional - it's part of the feature

---

## Next Steps

Before calling it "MVP Complete":
1. ✅ Build and deploy to device
2. ✅ Test location permission flow
3. ✅ Test route calculation with real GPS
4. ⚠️ Add camera animation for routes
5. ⚠️ Add better error handling UI
6. ⚠️ Add user guidance hints

**Remember:** MVP = Minimum **Viable** Product, not Minimum **Broken** Product.
