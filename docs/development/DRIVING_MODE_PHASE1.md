# Basic Driving Mode – Phase 1 Implementation Plan

> Scope: Distance-to-next-maneuver banner + off-route visual alert (no rerouting, no TTS) using existing Google Directions-derived data models.

---
## 1. Goals
- Display live next maneuver instruction + decreasing distance.
- Advance through steps accurately.
- Detect off-route condition and show clear visual alert (no reroute request).
- Keep CPU/memory overhead minimal; no new dependencies.

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

