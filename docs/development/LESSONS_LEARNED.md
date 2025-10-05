# Lessons Learned - Maps Route Picker

Internal development notes and insights from building this project.

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
**Better:** Auto-zoom to show entire route with padding

**Implementation needed:**
```kotlin
// After route calculation
val bounds = LatLngBounds.builder()
route.points.forEach { bounds.include(it) }
cameraPositionState.animate(
    CameraUpdateFactory.newLatLngBounds(bounds.build(), 100)
)
```

#### 3. ⚠️ No Unit Tests
**Current:** Zero test coverage
**Risk:** Regression bugs, hard to refactor safely

**Should test:**
- Polyline decoder logic
- Route calculation state machine
- Location permission flows
- ViewModel state updates

---

## Technical Decisions

### Architecture: MVVM + Clean Architecture Lite
**Why:**
- Compose works best with ViewModels for state management
- Repository pattern for data abstraction
- Domain models separate from DTOs
- No use-case layer (overkill for this app size)

**Trade-offs:**
- ✅ Clear separation of concerns
- ✅ Easy to test (if we had tests!)
- ❌ More files/boilerplate than MVI
- ❌ State updates via events can get verbose

### UI: 100% Jetpack Compose
**Why:**
- Modern Android development standard
- Declarative UI is cleaner
- No XML layouts to maintain
- Great for dynamic UI (swipeable cards, animations)

**Challenges:**
- Google Maps Compose wrapper has rough edges
- Camera control more complex than XML
- Custom gestures require low-level APIs

### DI: Hilt
**Why:**
- Standard for Android DI
- Works seamlessly with ViewModels
- Compile-time safety
- Less boilerplate than Dagger

**Trade-offs:**
- ✅ Simple setup for this app
- ❌ Annotation processor adds build time
- ❌ Debugging DI issues can be cryptic

### Networking: Retrofit + OkHttp
**Why:**
- Industry standard
- Type-safe API definitions
- Easy to add interceptors (logging, auth)
- Works great with coroutines

**Trade-offs:**
- ✅ Well-documented
- ✅ Handles retries, timeouts
- ❌ Slightly heavy for simple HTTP

---

## UX Decisions

### Swipeable Route Card
**Decision:** Card at top with spring physics
**Result:** Feels native and polished
**Lesson:** Small details (velocity tracking, haptics) make huge UX difference

### Dismissible Hints
**Decision:** Don't show hints forever, let users dismiss
**Result:** Less clutter, users in control
**Lesson:** Always give users escape hatches for helper UI

### Bottom-Right FAB Stack
**Decision:** Group all controls in one corner
**Result:** Cleaner than scattered buttons
**Lesson:** Follow platform conventions (FABs bottom-right on Android)

### Timeline vs List for Waypoints
**Decision:** Vertical timeline with connecting lines
**Result:** Visually communicates order and journey
**Lesson:** Use appropriate metaphors (timeline = journey)

---

## API/Cloud Learnings

### Google Cloud Setup Friction
**Problem:** Many moving parts (project, billing, APIs, keys, restrictions)
**Solution:** Created automated script and comprehensive docs
**Lesson:** Developer experience matters - reduce friction for new contributors

### Unrestricted Keys for Dev
**Decision:** Use unrestricted keys during development
**Result:** No SHA-1 fingerprint headaches during iteration
**Lesson:** Optimize for dev speed, restrict in production

### API Costs
**Reality:** Maps SDK free, Directions API has generous free tier
**Lesson:** For MVP/personal apps, free tier is plenty

---

## Build System

### Version Catalog
**Decision:** Use `libs.versions.toml` for dependencies
**Result:** Centralized version management
**Lesson:** Worth the initial setup for multi-module projects (even this small one)

### BuildConfig for API Keys
**Decision:** Load from `local.properties` → BuildConfig
**Result:** Keys stay out of Git, accessible in code
**Lesson:** Standard Android pattern, works great

### ProGuard Rules
**Decision:** Document all rules with comments
**Result:** Future us understands why each rule exists
**Lesson:** ProGuard rules are tribal knowledge - write them down

---

## Documentation Strategy

### Multi-Tiered Docs
**Structure:**
- README: Quick start, what/why
- CONTRIBUTING: How to help
- docs/setup: Step-by-step guides
- docs/api: API reference
- docs/development: Internal notes (this file)

**Result:** Different audiences find what they need
**Lesson:** Documentation has personas too

### Consolidation Needed
**Observation:** Started with 15+ markdown files
**Action:** Consolidating redundant API key troubleshooting docs
**Lesson:** Documentation sprawl happens fast - periodic cleanup needed

---

## Things to Add Before 1.0

### Must Have
- [ ] Unit tests for core logic
- [ ] Error handling for offline mode
- [ ] Camera auto-zoom to route bounds
- [ ] Save/load routes locally
- [ ] Production API key with restrictions

### Nice to Have
- [ ] Dark mode theme refinement
- [ ] Route alternatives (show 2-3 options)
- [ ] Estimated travel time
- [ ] Share route functionality
- [ ] Custom map styles

### Polish
- [ ] App icon (currently using default)
- [ ] Splash screen
- [ ] Onboarding flow for first launch
- [ ] Analytics (privacy-respecting)
- [ ] Crash reporting

---

## Metrics (if we tracked them)

**What we'd want to know:**
- Route calculation success rate
- Average waypoints per route
- Most common errors
- API response times
- Permission grant rates

**Why we don't have them yet:**
- Privacy concerns
- MVP focus
- Not sure which analytics library

**For future:**
- Consider Firebase Analytics (free tier)
- Or self-hosted Plausible (privacy-first)

---

## Reflections

### What Surprised Us
- Google Maps Compose wrapper less mature than expected
- Amount of boilerplate for swipe gestures
- How much docs matter for API key setup
- Spring physics makes everything feel better

### What We're Proud Of
- Clean architecture despite time pressure
- Comprehensive documentation
- Professional GitHub repo setup
- UX attention to detail (haptics, animations)

### What We'd Do Again
- Compose-first approach
- Version catalog from day 1
- Thorough .gitignore
- Security focus (API keys never in Git)

### What We'd Skip
- Some of the doc sprawl (fewer files, more consolidation)
- Maybe use MVI instead of MVVM for simpler state
- Could've used Maps SDK directly instead of Compose wrapper

---

## For Future Contributors

If you're reading this:
1. Check the main README first
2. Read CONTRIBUTING.md for process
3. Ask questions in Issues/Discussions
4. Don't be afraid to refactor
5. Tests welcome (we need them!)

---

**Last Updated:** October 2025  
**Next Review:** After 1.0 release

