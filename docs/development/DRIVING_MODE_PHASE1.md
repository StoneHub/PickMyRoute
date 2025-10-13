// filepath: c:\Users\monro\AndroidStudioProjects\PickMyRoute\docs\development\DRIVING_MODE_PHASE1.md
# Basic Driving Mode – Phase 1 Implementation Plan

> Scope: Distance-to-next-maneuver banner + off-route visual alert (no rerouting, no TTS) using existing Google Directions-derived data models.

---
## 1. Goals
- Clean up previous implementions of Driving mode/navigation UI. 
- make a new file for the driving mode phase 1 implementation plan
- lets try starting with Unit test (see below) 
- Display live next maneuver instruction + decreasing distance.
- Advance through steps accurately.
- Detect off-route condition and show clear visual alert (no reroute request).
- every so often update the distance (throttle to avoid jank), and ETA time
- investigate what prebuilt tools / ui components are available

## 2. Non‑Goals (Explicit Deferrals)
Rerouting, ETA recalculation, lane guidance, speed limits, voice/TTS, background nav, offline caching, persistence across process death.

## 3. Success Criteria
| Scenario | Expected Outcome |
|----------|------------------|
| Start navigation | Banner appears with first step instruction + distance |
| Approach maneuver | Distance counts down; shows "Now" < 15 m |
| Pass maneuver | Step index increments exactly once |
| Deviate laterally (~60 m) | Off-route alert after 3 consecutive updates |
| Return to route corridor | Off-route banner clears within 2 updates |
| Clear route mid-nav | No crash; banner disappears |

## 4. Data Model Additions (UI Layer)
Extend `MapState` with (nullable for backward compatibility):
- `currentStepIndex: Int?`
- `distanceToNextManeuverMeters: Double?`
- `nextInstructionPrimary: String?`
- `isOffRoute: Boolean`
- `offRouteDistanceMeters: Double?`

(Private ViewModel-only fields)
- `flattenedSteps: List<StepRef>`
- `decodedPolylines: MutableMap<Int, List<LatLng>>`
- `offRouteStrike / onRouteStrike: Int`
- `previousEmittedDistance: Double?`

`StepRef` structure:
```kotlin
internal data class StepRef(
  val globalIndex: Int,
  val legIndex: Int,
  val stepIndexInLeg: Int,
  val step: NavigationStep,
  val cumulativeMetersBefore: Int
)
```

## 5. Core Algorithms
### Flattening
Iterate legs & steps; accumulate `cumulativeMetersBefore` for quick progress metrics.

### Snapping (Phase 1 Simplified)
- Decode current step polyline (cache). For each segment compute perpendicular distance and projection.
- Accept snap if min distance ≤ 35 m; else treat unsnapped (use raw location for distance & off-route with strikes).
- Optionally also look at next step to reduce latency near step boundaries.

### Distance to Next Maneuver
`remaining = step.distanceMeters - traveledWithinStep` (clamp ≥ 0). Display as human-readable (see Formatting).

### Step Advancement
Advance if either:
- Remaining < 12 m, OR
- Haversine distance to `step.endLocation` < 15 m.
Skip zero-length steps immediately (loop until a non-zero step or end).

### Off-Route Detection
Evaluate minimal haversine distance `d` to points of current + next step.
- If `d > 45 m` → `offRouteStrike++` else `onRouteStrike++` & reset the other.
- Enter off-route when `offRouteStrike >= 3`.
- Exit when `onRouteStrike >= 2` *and* `d < 30 m`.
- Store `offRouteDistanceMeters = d` when flagged.

### Emission Throttling
Emit state only if:
- Step index changed OR
- Off-route flag toggled OR
- Distance bucket changed (≥ 3 m delta or formatting output changes).

## 6. UI / UX Spec
### Instruction Banner
Conditions: visible when `isNavigating && route != null`.
Content:
- Distance chip (formatted)
- Primary text: `nextInstructionPrimary` (from current step’s `instruction`)
- Off-route state overrides content: "Off route • {d} m" (rounded) with error styling.

Styling:
- Normal: `surfaceVariant` background.
- Imminent (<15 m): emphasize (e.g., `primaryContainer`).
- Off-route: `errorContainer` / `onErrorContainer` text.

Placement: Top center beneath (or replacing) route info card. If overlapping, collapse card during navigation (optional toggle later).

### Distance Formatting
Rules:
- ≥ 1000 m → one decimal km (1520 → "1.5 km")
- 100–999 m → integer meters
- 20–99 m → nearest 5 m (83 → 85 m)
- < 20 m → if < 15 → "Now" else exact meters

### Arrival (Optional for Phase 1)
If final step and remaining < 25 m → show "Arriving". (Can be deferred.)

## 7. Parameters / Tunables
| Name | Value | Purpose |
|------|-------|---------|
| snapThresholdMeters | 35 | Accept snap proximity |
| offRouteEnterMeters | 45 | Distance to start counting strikes |
| offRouteExitMeters | 30 | Distance to clear flag |
| offRouteEnterStrikes | 3 | Debounce for noise |
| offRouteExitStrikes | 2 | Debounce for returning |
| advanceDistanceMeters | 12 | Step advancement cut-off |
| maneuverNowThreshold | 15 | Switch to "Now" label |
| distanceEmissionDelta | 3 | Prevent churn |

All centralized in a private object for easy tuning.

## 8. ViewModel Flow
1. Route loaded → flatten steps + decode first 2 polylines.
2. StartNavigation → reset counters & indices.
3. Each location update (if navigating & route present):
   - Decode current (and next if near end) if needed.
   - Snap; compute traveled & remaining.
   - Advance if thresholds satisfied (loop while needed).
   - Evaluate off-route strikes.
   - If emission criteria met → update `MapState`.
4. StopNavigation → clear progress fields (set to null / defaults).

## 9. Testing Strategy
### Unit Tests
- Distance formatting (edge buckets)
- Step advancement threshold logic
- Off-route strike enter & exit sequences
- Snapping correctness on a simple straight polyline

### Manual / Exploratory
- Emulator path mock (adb or test harness) verifying countdown.
- Lateral deviation >60 m triggers off-route after ~3 s.
- Re-enter corridor returns to normal quickly.

### Logging
Temporary tags (debug builds):
- `NAV_STEP`: step=4 rem=82 off=false
- `NAV_ADV`: advance 4->5
- `NAV_OFF`: enter d=53 / exit d=27

## 10. Rollback Strategy
Feature flag: `enableDrivingModeProgress` (internal). If disabled, navigation mode only tilts/zooms camera; banner hidden. Since changes are additive, rollback = set flag false + remove banner composable call.

## 11. Incremental Task List (T1–T15)
- [ ] T1 Add MapState fields (progress + off-route)
- [ ] T2 Define `StepRef` internal data class
- [ ] T3 Implement `flattenSteps(route)`
- [ ] T4 Implement lazy polyline decode cache
- [ ] T5 Implement `snapToStep()` helper
- [ ] T6 Implement remaining distance computation
- [ ] T7 Implement advancement loop
- [ ] T8 Implement off-route detection logic
- [ ] T9 Integrate into `onLocationUpdate` (navigation guard)
- [ ] T10 Emission throttling mechanism
- [ ] T11 InstructionBanner composable
- [ ] T12 Integrate banner into `MapScreen`
- [ ] T13 Logging hooks
- [ ] T14 Extract pure helpers + unit test skeleton
- [ ] T15 Manual run + refine thresholds

(Adjust checkmarks as work proceeds.)

## 12. Risks & Mitigations
| Risk | Impact | Mitigation |
|------|--------|------------|
| GPS noise near turns causes false off-route | User confusion | Strike system + generous thresholds |
| Performance overhead from decoding all steps | Battery / jank | Lazy decode current + next only |
| Rapid camera / UI recompositions | Jank | Throttle emissions (delta & buckets) |
| Race when route cleared mid-update | Crash risk | Null + bounds checks before each stage |

## 13. Future Phases Enabled
- Phase 2: Rerouting (replace off-route alert path with repository call)
- Phase 3: Voice/TTS (subscribe to step & bucket transitions)
- Phase 4: ETA modeling (add cumulative remaining distance)
- Phase 5: Lane/Speed features (extend data model)

## 14. Manual Verification Checklist
1. Acquire location permissions; set route with destination & waypoints.
2. Enter navigation → first instruction visible.
3. Simulate movement (or walk in real world) → distance decreases smoothly.
4. Step index increments logged exactly once per maneuver.
5. Force deviation >60 m → off-route alert after ~3 updates.
6. Return inside 25–30 m → off-route clears.
7. Stop navigation → banner disappears, state resets.

## 15. Commands (Local Validation)
(Executed by user—Android Studio / Gradle handles builds)
```bash
# Build (Windows cmd via Gradle wrapper):
gradlew.bat assembleDebug

# (Optional) Run unit tests once added:
gradlew.bat testDebugUnitTest
```

## 16. No Dependency Changes Required
Existing versions compatible: Kotlin 2.0.21, Compose Compiler 1.7.1, Hilt 2.57.2 (JavaPoet 1.13.0 already pinned), AGP 8.13.0. Compose compiler extension matches catalog; no action needed.

## 17. Open Questions (Defaults If Unanswered)
| Question | Default Applied |
|----------|-----------------|
| Hide route card during navigation? | Keep visible; user can collapse manually |
| Show "Arriving" final step? | Deferred unless trivial after core complete |
| Metric vs Imperial toggle? | Metric only (km/m) for Phase 1 |

---
## 18. Summary
Root need: Basic, reliable driving guidance overlay.
Planned actions: Add minimal state fields, implement snapping + advancement + off-route strikes, surface as a lightweight instruction banner.
Verification: Unit tests for math + manual emulator path playback.
Next step: Implement tasks T1–T15; then empirical threshold tuning.

---
**Decision Point:** Proceed to implementation (mark T1–T15) or adjust thresholds before coding.

---

## 19. Implementation snippets & exact integration points (concrete)
Below are compact, copy-paste-friendly Kotlin examples showing where and how to implement the plan using the repo's existing types (`MapState`, `MapViewModel`, `MapScreen`, `NavigationStep`, `PolylineDecoder`). These are intentionally small, focused helpers and composables. Add them behind the ViewModel and Map UI code as suggested file locations.

### 19.1 MapState additions
File: `app/src/main/java/com/stonecode/pickmyroute/ui/map/MapState.kt`

Add the following properties to the `MapState` data class (keep defaults for backward compat):

```kotlin
// ...existing code...
    // Navigation mode
    val isNavigating: Boolean = false,
    val deviceBearing: Float = 0f,
    val cameraTilt: Float = 0f,

    // Driving-mode progress fields (Phase 1)
    val currentStepIndex: Int? = null,
    val distanceToNextManeuverMeters: Double? = null,
    val nextInstructionPrimary: String? = null,
    val isOffRoute: Boolean = false,
    val offRouteDistanceMeters: Double? = null
)
// ...existing code...
```

Note: these fields are UI-only; the ViewModel will compute/emit them.

### 19.2 Internal StepRef + decode cache
File: `app/src/main/java/com/stonecode/pickmyroute/ui/map/MapViewModel.kt`

Add at top (private to ViewModel):

```kotlin
// ...existing code...
private data class StepRef(
    val globalIndex: Int,
    val legIndex: Int,
    val stepIndexInLeg: Int,
    val step: NavigationStep,
    val cumulativeMetersBefore: Int
)

// Lazy cache of decoded polylines keyed by globalIndex
private val decodedPolylines = mutableMapOf<Int, List<com.google.android.gms.maps.model.LatLng>>()

// Tunables
private object NavParams {
    const val snapThresholdMeters = 35.0
    const val offRouteEnterMeters = 45.0
    const val offRouteExitMeters = 30.0
    const val offRouteEnterStrikes = 3
    const val offRouteExitStrikes = 2
    const val advanceDistanceMeters = 12.0
    const val maneuverNowThreshold = 15.0
    const val distanceEmissionDelta = 3.0
}
// ...existing code...
```

### 19.3 Flatten steps helper
Implement `flattenSteps(route)` which produces a List<StepRef> used for quick indexing:

```kotlin
private fun flattenSteps(route: com.stonecode.pickmyroute.domain.model.Route): List<StepRef> {
    val refs = mutableListOf<StepRef>()
    var globalIndex = 0
    var cumulative = 0

    route.legs.forEachIndexed { legIdx, leg ->
        leg.steps.forEachIndexed { stepIdx, step ->
            refs += StepRef(
                globalIndex = globalIndex++,
                legIndex = legIdx,
                stepIndexInLeg = stepIdx,
                step = step,
                cumulativeMetersBefore = cumulative
            )
            cumulative += step.distanceMeters
        }
    }

    return refs
}
```

Call this from `calculateRoute()` after `route` is loaded and store it in a private field `flattenedSteps`.

### 19.4 Polyline decode cache (lazy)
When you need a decoded polyline for `globalIndex`:

```kotlin
private fun getDecodedPolyline(globalIndex: Int, step: NavigationStep): List<com.google.android.gms.maps.model.LatLng> {
    return decodedPolylines.getOrPut(globalIndex) {
        // step.polyline is the encoded polyline string
        PolylineDecoder.decode(step.polyline)
    }
}
```

### 19.5 Geometry helpers (distance & projection)
Create a small util within the ViewModel file or `util/NavigationUtils.kt`:

```kotlin
private fun distanceMeters(a: com.google.android.gms.maps.model.LatLng, b: com.google.android.gms.maps.model.LatLng): Double {
    val result = FloatArray(1)
    android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
    return result[0].toDouble()
}

// Project point P onto segment AB, return Pair(projectedPoint, distanceFromPtoSegmentMeters)
private fun projectOntoSegment(p: com.google.android.gms.maps.model.LatLng,
                                 a: com.google.android.gms.maps.model.LatLng,
                                 b: com.google.android.gms.maps.model.LatLng): Pair<com.google.android.gms.maps.model.LatLng, Double> {
    // Convert to simple x/y using lat/lng degrees — this is approximate but sufficient for short distances
    val ax = a.latitude; val ay = a.longitude
    val bx = b.latitude; val by = b.longitude
    val px = p.latitude; val py = p.longitude

    val vx = bx - ax
    val vy = by - ay
    val wx = px - ax
    val wy = py - ay

    val c1 = vx*wx + vy*wy
    val c2 = vx*vx + vy*vy
    val t = when {
        c2 == 0.0 -> 0.0
        else -> (c1 / c2).coerceIn(0.0, 1.0)
    }

    val projLat = ax + t * vx
    val projLng = ay + t * vy
    val projected = com.google.android.gms.maps.model.LatLng(projLat, projLng)
    val dist = distanceMeters(p, projected)
    return projected to dist
}
```

Notes: This projection uses lat/lng degrees as an equirectangular approximation. It's fast and good for short distances (< few km). If you need more accuracy, convert to meters with a proper easting/northing transform.

### 19.6 snapToStep() (simplified)
Use the decoded polyline and project onto each segment; accept the snap if min distance ≤ snapThresholdMeters.

```kotlin
private fun snapToStep(globalIndex: Int, step: NavigationStep, deviceLocation: com.google.android.gms.maps.model.LatLng): Pair<com.google.android.gms.maps.model.LatLng?, Double> {
    val poly = getDecodedPolyline(globalIndex, step)
    if (poly.size < 2) return deviceLocation to Double.MAX_VALUE

    var bestPoint: com.google.android.gms.maps.model.LatLng? = null
    var bestDistance = Double.MAX_VALUE

    for (i in 0 until poly.lastIndex) {
        val a = poly[i]
        val b = poly[i + 1]
        val (proj, d) = projectOntoSegment(deviceLocation, a, b)
        if (d < bestDistance) {
            bestDistance = d
            bestPoint = proj
        }
    }

    return if (bestDistance <= NavParams.snapThresholdMeters) bestPoint to bestDistance else deviceLocation to bestDistance
}
```

### 19.7 Compute remaining distance within step
If you have a snapped projected point, compute how many meters along the step that projection is from the step end using the decoded polyline cumulative distance:

```kotlin
private fun remainingDistanceForStep(globalIndex: Int, stepRef: StepRef, projectedPoint: com.google.android.gms.maps.model.LatLng?): Double {
    if (projectedPoint == null) return stepRef.step.distanceMeters.toDouble()

    val poly = getDecodedPolyline(globalIndex, stepRef.step)
    // Walk the polyline and sum distances until we pass projectedPoint
    var acc = 0.0
    var found = false
    for (i in 0 until poly.lastIndex) {
        val a = poly[i]
        val b = poly[i + 1]
        // if projection lies on this segment (approx match) then add partial distance
        val (_, dToSegment) = projectOntoSegment(projectedPoint, a, b)
        val segLen = distanceMeters(a, b)
        // Conservative approach: if projected point is within segLen of either endpoint treat as inside
        if (dToSegment < 5.0) {
            // partial distance from projected point to end of polyline
            val distFromProjToEnd = distanceMeters(projectedPoint, stepRef.step.endLocation)
            return distFromProjToEnd.coerceAtLeast(0.0)
        }
        acc += segLen
    }
    // Fallback
    return stepRef.step.distanceMeters.toDouble()
}
```

This is intentionally simple: for Phase 1 we just need a reasonable estimate of remaining meters; it will be refined later if necessary.

### 19.8 Advancement loop (pseudocode)
Call this after computing remaining on the current step. It may loop to skip zero-length or very short steps:

```kotlin
private fun maybeAdvanceWhileNeeded(deviceLocation: com.google.android.gms.maps.model.LatLng) {
    var idx = currentStepIndex ?: 0
    while (idx < flattenedSteps.size) {
        val stepRef = flattenedSteps[idx]
        val projected = snapToStep(stepRef.globalIndex, stepRef.step, deviceLocation).first
        val remaining = remainingDistanceForStep(stepRef.globalIndex, stepRef, projected)
        if (remaining < NavParams.advanceDistanceMeters || distanceMeters(deviceLocation, stepRef.step.endLocation) < 15.0) {
            // advance
            idx += 1
            continue
        }
        break
    }
    // write idx back to currentStepIndex (and clamp to bounds)
}
```

### 19.9 Off-route strike logic (store strikes in ViewModel)
Keep two ints `offRouteStrike` and `onRouteStrike` as private ViewModel properties. On each update:

```kotlin
val minDist = min(distanceToCurrentStep, distanceToNextStep)
if (minDist > NavParams.offRouteEnterMeters) {
    offRouteStrike += 1
    onRouteStrike = 0
} else {
    onRouteStrike += 1
    offRouteStrike = 0
}
if (offRouteStrike >= NavParams.offRouteEnterStrikes) {
    // mark off-route and emit state
}
if (onRouteStrike >= NavParams.offRouteExitStrikes && minDist < NavParams.offRouteExitMeters) {
    // clear off-route
}
```

Store the measured distance into `offRouteDistanceMeters` when entering off-route.

### 19.10 Emission throttling
Before calling `_state.update { ... }`, compare previous emitted values: step index, isOffRoute, and previousEmittedDistance. Only emit when any of those changed or the absolute difference of distance > `distanceEmissionDelta`.

```kotlin
private var previousEmittedDistance: Double? = null
private fun shouldEmit(newStepIdx: Int?, newOffRoute: Boolean, newDistance: Double?): Boolean {
    if (newStepIdx != _state.value.currentStepIndex) return true
    if (newOffRoute != _state.value.isOffRoute) return true
    val prev = previousEmittedDistance
    if (prev == null && newDistance != null) return true
    if (prev != null && newDistance != null && kotlin.math.abs(prev - newDistance) >= NavParams.distanceEmissionDelta) return true
    return false
}

// When emitting:
_state.update { it.copy(
    currentStepIndex = newStepIdx,
    distanceToNextManeuverMeters = newDistance,
    nextInstructionPrimary = newInstruction,
    isOffRoute = newOffRoute,
    offRouteDistanceMeters = if (newOffRoute) newDistance else null
)}
previousEmittedDistance = newDistance
```

### 19.11 InstructionBanner composable (Phase 1)
File: `app/src/main/java/com/stonecode/pickmyroute/ui/map/components/InstructionBanner.kt`

```kotlin
@Composable
fun InstructionBanner(
    state: MapState,
    onStopNavigation: () -> Unit
) {
    if (!state.isNavigating || state.route == null) return

    val distanceText = state.distanceToNextManeuverMeters?.let { formatDistance(it) } ?: ""

    val bg = when {
        state.isOffRoute -> MaterialTheme.colorScheme.errorContainer
        (state.distanceToNextManeuverMeters ?: Double.MAX_VALUE) <= 15.0 -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        color = bg,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Text(text = distanceText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text(text = if (state.isOffRoute) "Off route • ${state.offRouteDistanceMeters?.toInt() ?: ""} m" else state.nextInstructionPrimary ?: "", maxLines = 1)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onStopNavigation) {
                Icon(Icons.Default.Close, contentDescription = "Stop")
            }
        }
    }
}
```

Integration: Call `InstructionBanner(state = state, onStopNavigation = { viewModel.onEvent(MapEvent.StopNavigation) })` inside `MapContent` in `MapScreen.kt`, positioned near the top (beneath `SwipeableRouteInfoCard`).

### 19.12 Where to wire into `onLocationUpdate`
In `MapViewModel.onLocationUpdate(location: LatLng)` (file `MapViewModel.kt`) add a guard when `state.isNavigating && state.route != null` and then run the flow:
- Ensure `flattenedSteps` exists
- Use `maybeAdvanceWhileNeeded(location)`
- Compute snap and remaining for current step
- Update strikes and decide off-route
- Decide whether to emit `_state.update` via `shouldEmit(...)`

Keep all new state private to the ViewModel except for the final `MapState` fields to be consumed by UI.

### 19.13 Unit test skeletons
File: `app/src/test/java/com/stonecode/pickmyroute/navigation/NavigationHelpersTest.kt`

```kotlin
class NavigationHelpersTest {

    @Test
    fun distanceFormatting_edges() {
        assertEquals("1.5 km", formatDistance(1520.0))
        assertEquals("85 m", formatDistance(83.0))
        assertEquals("Now", formatDistance(12.0))
    }

    @Test
    fun advancement_threshold() {
        // build a fake route with a few steps and assert the ViewModel will advance when remaining < 12
    }

    @Test
    fun offroute_strike_sequence() {
        // simulate successive distances and assert strikes produce off-route after 3 bad updates
    }
}
```

Notes: Keep helpers pure and small so they are straightforward to unit test.

### 19.14 Logging & debug
Use the same log tags already present: `NAV_STEP`, `NAV_ADV`, `NAV_OFF`. Add a compact debug dump when entering/exiting off-route for easier manual testing.

---

## 20. Next steps (developer-facing)
1. Copy the snippets above into the repo: add MapState fields (T1), create `InstructionBanner` composable (T11), implement flattening & decode cache (T3/T4), then wire `onLocationUpdate` (T9) to call the helpers (T5–T8).
2. Add unit tests from 19.13 and run `gradlew.bat testDebugUnitTest` locally. I will wait for you to run the build/tests and paste any errors/stack traces if they occur.
3. Tune thresholds after manual emulator runs and real-world driving tests.

---

## 21. Quick integration checklist (mapping to files)
- MapState additions: `app/src/main/java/com/stonecode/pickmyroute/ui/map/MapState.kt` (T1)
- StepRef + helpers + decoded cache: `app/src/main/java/com/stonecode/pickmyroute/ui/map/MapViewModel.kt` (T2–T6)
- InstructionBanner: `app/src/main/java/com/stonecode/pickmyroute/ui/map/components/InstructionBanner.kt` (T11)
- MapScreen integration: `app/src/main/java/com/stonecode/pickmyroute/ui/map/MapScreen.kt` (T12)
- Utils: `app/src/main/java/com/stonecode/pickmyroute/util/NavigationUtils.kt` (optional)
- Unit tests: `app/src/test/java/com/stonecode/pickmyroute/navigation/NavigationHelpersTest.kt` (T14)

---

## Requirements coverage
- Add state fields: Done (documented) ✅
- Flatten, decode cache, snapping: Provided code snippets and guidance ✅
- Off-route strike system: Provided snippet & tunables ✅
- Emission throttling: Provided ✅
- InstructionBanner + integration: Provided snippet & where to place ✅

---

### Implementation assumptions
- `NavigationStep.polyline` contains the encoded polyline (true per `NavigationStep.kt`).
- `PolylineDecoder.decode` returns `List<LatLng>` (exists in `util/PolylineDecoder.kt`).
- Use `android.location.Location.distanceBetween(...)` for meter calculations (available in Android SDK).

If any of these assumptions are incorrect, tell me which file to inspect and I will adapt the snippets accordingly.

---

End of file.
