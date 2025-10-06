# Firebase Analytics Integration Plan
**PickMyRoute - Maps Route Picker**

**Created:** October 5, 2025  
**Status:** 📋 Planning Phase - Not Started  
**Priority:** Medium  
**Estimated Effort:** 4-6 hours  
**Complexity:** Medium

---

## 🎯 Objective

Integrate Firebase Analytics into PickMyRoute to track user behavior, app performance, and feature usage. This will provide data-driven insights for:
- Understanding how users interact with waypoint routing
- Measuring navigation feature adoption
- Identifying user pain points and drop-off locations
- Optimizing route planning workflows
- A/B testing future features

---

## 📊 Why Firebase Analytics?

### Benefits for PickMyRoute:
1. **Free tier is generous** - Unlimited events and users
2. **Native Android integration** - Automatic lifecycle tracking
3. **Real-time dashboard** - See usage patterns immediately
4. **User properties** - Segment by usage patterns (power users vs casual)
5. **Conversion funnels** - Track destination → waypoints → navigation flow
6. **Crash reporting integration** - Firebase Crashlytics works seamlessly
7. **Remote config ready** - Future A/B testing capability

### Key Metrics to Track:
- Route planning frequency (daily active users)
- Average waypoints per route (feature adoption)
- Navigation session duration
- Map interaction patterns (taps, zooms, pans)
- Error rates and types
- Feature discovery (swipe gestures, search usage)

---

## 🏗️ Architecture Overview

### Components to Add:
```
app/
├── google-services.json          [NEW] Firebase config file
├── src/main/java/.../
│   ├── analytics/                [NEW] Analytics module
│   │   ├── AnalyticsHelper.kt   [NEW] Main tracking interface
│   │   ├── AnalyticsEvents.kt   [NEW] Event name constants
│   │   ├── AnalyticsParams.kt   [NEW] Parameter name constants
│   │   └── AnalyticsModule.kt   [NEW] Hilt DI module
│   ├── ui/map/
│   │   └── MapViewModel.kt      [MODIFY] Add analytics tracking
│   └── MapsRoutePickerApp.kt    [MODIFY] Initialize Firebase
```

### Data Flow:
```
User Action → ViewModel → AnalyticsHelper → Firebase SDK → Firebase Console
                  ↓
          Repository (unchanged)
```

---

## 📦 Phase 1: Firebase Project Setup

### 1.1 Create Firebase Project
**Location:** Firebase Console (https://console.firebase.google.com)

**Steps:**
1. Create new Firebase project named "PickMyRoute" or "MapsRoutePicker"
2. Enable Google Analytics for the project
3. Choose Analytics location (recommend: United States)
4. Accept terms and conditions
5. Wait for project creation (~30 seconds)

**Output:** Firebase project ID (e.g., `pickmyroute-abc123`)

---

### 1.2 Register Android App
**Location:** Firebase Console → Project Settings → Your Apps

**Steps:**
1. Click "Add app" → Android icon
2. Enter Android package name: `com.stonecode.mapsroutepicker`
3. App nickname: "PickMyRoute Android"
4. Debug signing certificate SHA-1 (optional for analytics, required later for auth)
   - Get from: `cd android && ./gradlew signingReport`
   - Copy SHA-1 from "debug" variant
5. Download `google-services.json`
6. Place file in `app/` directory (same level as `build.gradle.kts`)

**Security Note:** `google-services.json` is safe to commit (contains public API keys)

---

### 1.3 Verify google-services.json Placement
**Location:** `app/google-services.json`

**Validation:**
- File must be in `app/` directory, not `app/src/`
- Should contain `"package_name": "com.stonecode.mapsroutepicker"`
- Should have `project_id` matching your Firebase project
- Git: Add to version control (not sensitive like local.properties)

---

## 📝 Phase 2: Gradle Configuration

### 2.1 Update Root build.gradle.kts
**File:** `build.gradle.kts` (project root)

**Changes:**
```kotlin
plugins {
    // ...existing plugins...
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

**Reason:** Google Services plugin processes `google-services.json` at build time

---

### 2.2 Update libs.versions.toml
**File:** `gradle/libs.versions.toml`

**Add to [versions] section:**
```toml
# Firebase
firebaseBom = "33.5.1"  # Oct 2025 latest
googleServices = "4.4.2"
```

**Add to [libraries] section:**
```toml
# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }
```

**Add to [plugins] section:**
```toml
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

**Why BOM?** Firebase Bill of Materials ensures all Firebase libraries use compatible versions

---

### 2.3 Update app/build.gradle.kts
**File:** `app/build.gradle.kts`

**Add to plugins block:**
```kotlin
plugins {
    // ...existing plugins...
    alias(libs.plugins.google.services)
}
```

**Add to dependencies block:**
```kotlin
dependencies {
    // ...existing dependencies...

    // Firebase (BOM manages versions)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    
    // Optional but recommended:
    // implementation(libs.firebase.crashlytics) // For crash reporting
}
```

**Build validation:**
- Run `./gradlew app:dependencies` to verify Firebase appears
- Check for conflicts with existing Play Services versions

---

## 🔧 Phase 3: Code Implementation

### 3.1 Create Analytics Event Constants
**File:** `app/src/main/java/com/stonecode/mapsroutepicker/analytics/AnalyticsEvents.kt`

```kotlin
package com.stonecode.mapsroutepicker.analytics

/**
 * Firebase Analytics event names
 * Follow Firebase naming conventions: lowercase with underscores
 */
object AnalyticsEvents {
    // Route Planning
    const val DESTINATION_SET = "destination_set"
    const val WAYPOINT_ADDED = "waypoint_added"
    const val WAYPOINT_REMOVED = "waypoint_removed"
    const val WAYPOINT_UNDONE = "waypoint_undone"
    const val ROUTE_CALCULATED = "route_calculated"
    const val ROUTE_CLEARED = "route_cleared"
    
    // Navigation
    const val NAVIGATION_STARTED = "navigation_started"
    const val NAVIGATION_STOPPED = "navigation_stopped"
    const val LOCATION_RECENTERED = "location_recentered"
    const val COMPASS_RESET = "compass_reset"
    
    // Search
    const val SEARCH_QUERY = "search_query"
    const val SEARCH_RESULT_SELECTED = "search_result_selected"
    
    // Map Interactions
    const val MAP_TAPPED = "map_tapped"
    const val MAP_LONG_PRESSED = "map_long_pressed"
    
    // Errors
    const val ROUTE_ERROR = "route_error"
    const val LOCATION_PERMISSION_DENIED = "location_permission_denied"
    
    // Feature Discovery
    const val SWIPE_GESTURE_USED = "swipe_gesture_used"
    const val TIMELINE_INTERACTED = "timeline_interacted"
}
```

---

### 3.2 Create Analytics Parameter Constants
**File:** `app/src/main/java/com/stonecode/mapsroutepicker/analytics/AnalyticsParams.kt`

```kotlin
package com.stonecode.mapsroutepicker.analytics

/**
 * Firebase Analytics parameter names
 * Follow Firebase naming conventions: lowercase with underscores
 */
object AnalyticsParams {
    // Route metrics
    const val WAYPOINT_COUNT = "waypoint_count"
    const val ROUTE_DISTANCE_METERS = "route_distance_meters"
    const val ROUTE_DURATION_SECONDS = "route_duration_seconds"
    const val HAS_WAYPOINTS = "has_waypoints"
    
    // Navigation
    const val NAVIGATION_DURATION_SECONDS = "navigation_duration_seconds"
    const val NAVIGATION_MODE = "navigation_mode" // "3d" or "2d"
    
    // Search
    const val SEARCH_QUERY_LENGTH = "search_query_length"
    const val SEARCH_RESULT_COUNT = "search_result_count"
    
    // Errors
    const val ERROR_TYPE = "error_type"
    const val ERROR_MESSAGE = "error_message"
    
    // Map
    const val MAP_ZOOM_LEVEL = "map_zoom_level"
    const val MAP_INTERACTION_TYPE = "interaction_type" // "tap", "swipe", "pinch"
}
```

---

### 3.3 Create Analytics Helper Interface
**File:** `app/src/main/java/com/stonecode/mapsroutepicker/analytics/AnalyticsHelper.kt`

```kotlin
package com.stonecode.mapsroutepicker.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around Firebase Analytics for type-safe event logging
 * 
 * Benefits:
 * - Centralized analytics logic
 * - Easy to mock for testing
 * - Type-safe parameter building
 * - Can add custom logic (sampling, filtering, etc.)
 */
@Singleton
class AnalyticsHelper @Inject constructor() {
    
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics
    
    /**
     * Log an event with optional parameters
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        val bundle = Bundle().apply {
            params?.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
    }
    
    /**
     * Set user property (max 25 properties, max 36 chars per name)
     */
    fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }
    
    /**
     * Set user ID for tracking across sessions
     */
    fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }
    
    /**
     * Enable/disable analytics collection
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }
}

// Extension functions for common events
fun AnalyticsHelper.logDestinationSet(hasWaypoints: Boolean = false) {
    logEvent(AnalyticsEvents.DESTINATION_SET, mapOf(
        AnalyticsParams.HAS_WAYPOINTS to hasWaypoints
    ))
}

fun AnalyticsHelper.logWaypointAdded(totalCount: Int) {
    logEvent(AnalyticsEvents.WAYPOINT_ADDED, mapOf(
        AnalyticsParams.WAYPOINT_COUNT to totalCount
    ))
}

fun AnalyticsHelper.logRouteCalculated(
    distanceMeters: Int,
    durationSeconds: Int,
    waypointCount: Int
) {
    logEvent(AnalyticsEvents.ROUTE_CALCULATED, mapOf(
        AnalyticsParams.ROUTE_DISTANCE_METERS to distanceMeters,
        AnalyticsParams.ROUTE_DURATION_SECONDS to durationSeconds,
        AnalyticsParams.WAYPOINT_COUNT to waypointCount
    ))
}

fun AnalyticsHelper.logNavigationStarted(waypointCount: Int) {
    logEvent(AnalyticsEvents.NAVIGATION_STARTED, mapOf(
        AnalyticsParams.WAYPOINT_COUNT to waypointCount
    ))
}

fun AnalyticsHelper.logError(errorType: String, message: String?) {
    logEvent(AnalyticsEvents.ROUTE_ERROR, mapOf(
        AnalyticsParams.ERROR_TYPE to errorType,
        AnalyticsParams.ERROR_MESSAGE to (message ?: "unknown")
    ))
}
```

---

### 3.4 Create Hilt Module
**File:** `app/src/main/java/com/stonecode/mapsroutepicker/analytics/AnalyticsModule.kt`

```kotlin
package com.stonecode.mapsroutepicker.analytics

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides AnalyticsHelper as singleton throughout the app
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    
    @Provides
    @Singleton
    fun provideAnalyticsHelper(): AnalyticsHelper {
        return AnalyticsHelper()
    }
}
```

---

### 3.5 Update MapViewModel
**File:** `app/src/main/java/com/stonecode/mapsroutepicker/ui/map/MapViewModel.kt`

**Add to constructor:**
```kotlin
@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val routingRepository: RoutingRepository,
    private val placesRepository: PlacesRepository,
    private val analyticsHelper: AnalyticsHelper  // NEW
) : ViewModel() {
```

**Add tracking to key methods:**
```kotlin
private fun setDestination(location: LatLng) {
    _state.update { it.copy(
        destination = location,
        showDestinationInput = false
    )}
    
    // Track destination set
    analyticsHelper.logDestinationSet(hasWaypoints = _state.value.waypoints.isNotEmpty())
    
    if (_state.value.currentLocation != null) {
        calculateRoute()
    }
}

private fun addWaypoint(location: LatLng) {
    val currentWaypoints = _state.value.waypoints
    val newWaypoint = Waypoint(/*...*/)
    
    _state.update { it.copy(waypoints = currentWaypoints + newWaypoint) }
    
    // Track waypoint added
    analyticsHelper.logWaypointAdded(_state.value.waypoints.size)
    
    calculateRoute()
}

private fun calculateRoute() {
    // ...existing code...
    
    try {
        val route = routingRepository.getRoute(/*...*/)
        _state.update { it.copy(route = route, isLoading = false) }
        
        // Track successful route calculation
        analyticsHelper.logRouteCalculated(
            distanceMeters = route.totalDistanceMeters,
            durationSeconds = route.totalDurationSeconds,
            waypointCount = currentState.waypoints.size
        )
        
    } catch (e: Exception) {
        // Track error
        analyticsHelper.logError("route_calculation", e.message)
        
        _state.update { it.copy(
            isLoading = false,
            error = "Failed to calculate route: ${e.message}"
        )}
    }
}

private fun startNavigation() {
    Log.d("MapViewModel", "🚗 Starting navigation mode")
    
    // Track navigation started
    analyticsHelper.logNavigationStarted(_state.value.waypoints.size)
    
    _state.update { it.copy(
        isNavigating = true,
        cameraTilt = 45f
    )}
    // ...existing code...
}
```

---

### 3.6 Initialize Firebase in Application Class
**File:** `app/src/main/java/com/stonecode/mapsroutepicker/MapsRoutePickerApp.kt`

**Add initialization:**
```kotlin
@HiltAndroidApp
class MapsRoutePickerApp : Application() {
    
    @Inject
    lateinit var analyticsHelper: AnalyticsHelper
    
    override fun onCreate() {
        super.onCreate()
        
        // Firebase is automatically initialized via google-services.json
        // but we can set analytics properties here
        
        analyticsHelper.setUserProperty("app_version", BuildConfig.VERSION_NAME)
        
        // Optional: Disable in debug builds
        if (BuildConfig.DEBUG) {
            analyticsHelper.setAnalyticsEnabled(false)
            Log.d("Analytics", "Firebase Analytics disabled in debug build")
        }
    }
}
```

---

## 🧪 Phase 4: Testing & Validation

### 4.1 Local Testing
**Tool:** Android Studio Logcat + Firebase DebugView

**Steps:**
1. Enable debug mode on device:
   ```bash
   adb shell setprop debug.firebase.analytics.app com.stonecode.mapsroutepicker
   ```

2. Run app and trigger events:
   - Set destination (should log `destination_set`)
   - Add waypoints (should log `waypoint_added`)
   - Calculate route (should log `route_calculated`)
   - Start navigation (should log `navigation_started`)

3. Check Logcat for Firebase logs:
   - Filter: `Firebase Analytics`
   - Look for: "Logging event (FE)" messages

4. Open Firebase Console → Analytics → DebugView
   - Should see events in real-time
   - Debug mode expires after 24 hours

**Validation Checklist:**
- [ ] Events appear in Logcat
- [ ] Events appear in DebugView
- [ ] Parameters are correct
- [ ] No crashes or errors
- [ ] Events fire at expected times

---

### 4.2 Production Testing
**Timeline:** After 24 hours of production use

**Firebase Console Checks:**
1. **Dashboard** → Verify active users count
2. **Events** → Check event counts and parameters
3. **Audiences** → Create audience (e.g., "Power Users: 10+ waypoints")
4. **User Properties** → Verify app_version is set

**Key Metrics to Monitor:**
- Daily Active Users (DAU)
- Average waypoints per route
- Navigation adoption rate (% of routes that start navigation)
- Route calculation success rate
- Error frequency

---

## 📈 Phase 5: Analytics Dashboard Setup

### 5.1 Create Custom Events in Firebase Console
**Location:** Firebase Console → Analytics → Events → Manage Custom Definitions

**Custom Events to Register:**
1. `waypoint_added` - Description: "User adds waypoint to route"
2. `navigation_started` - Description: "User begins turn-by-turn navigation"
3. `route_calculated` - Description: "Route successfully calculated"
4. `swipe_gesture_used` - Description: "User discovers swipe-to-cancel"

**Why?** Makes events searchable and adds descriptions for team members

---

### 5.2 Create Conversion Funnels
**Location:** Firebase Console → Analytics → Analysis → Funnel Analysis

**Funnel 1: Route Creation**
1. `destination_set` (Start)
2. `waypoint_added` (Optional)
3. `route_calculated` (Conversion)

**Funnel 2: Navigation Adoption**
1. `route_calculated` (Start)
2. `navigation_started` (Conversion)

**Analysis:** Identify where users drop off

---

### 5.3 Set Up User Properties
**Custom Properties to Track:**
- `total_routes_created` - Incremented counter
- `navigation_user` - "yes" if ever used navigation
- `power_user` - "yes" if > 10 waypoints in a single route
- `search_user` - "yes" if ever used place search

**Implementation:** Update in ViewModel when milestones hit

---

## 🔐 Phase 6: Privacy & Compliance

### 6.1 Update Privacy Policy
**Location:** App Store listing + in-app settings

**Required Disclosures:**
- Analytics data collection (anonymized)
- Device identifiers collected
- Location data usage (already disclosed)
- Right to opt-out

**Template Text:**
```
Analytics: We use Firebase Analytics to understand how users interact 
with PickMyRoute. This helps us improve the app. Data collected includes:
- App interactions (taps, swipes, navigation)
- Device type and OS version
- Approximate location (city-level, not precise coordinates)
- Crash reports

You can opt-out in Settings → Privacy → Analytics.
```

---

### 6.2 Add Analytics Opt-Out Setting
**File:** Create `SettingsScreen.kt` (future)

**Implementation:**
```kotlin
// In SettingsViewModel
fun setAnalyticsEnabled(enabled: Boolean) {
    analyticsHelper.setAnalyticsEnabled(enabled)
    // Save preference
    preferencesRepository.setAnalyticsEnabled(enabled)
}
```

**UI:**
- Settings screen with toggle switch
- "Help improve PickMyRoute" description
- Default: Enabled (industry standard)

---

### 6.3 GDPR Compliance (if targeting EU)
**Required for EU users:**
- Consent dialog on first launch
- Clear explanation of data usage
- Easy opt-out mechanism
- Data deletion request process

**Implementation:** Use consent mode (Phase 7, future)

---

## 🚀 Phase 7: Advanced Features (Future)

### 7.1 Firebase Crashlytics Integration
**Purpose:** Automatic crash reporting with analytics context

**Implementation:**
- Add `firebase-crashlytics` dependency
- Crashlytics automatically links with Analytics
- Custom keys for debugging (waypoint count, route state)

---

### 7.2 Firebase Remote Config
**Purpose:** A/B testing and feature flags

**Use Cases:**
- Test different color schemes for route segments
- Feature rollout (gradual navigation mode release)
- Dynamic max waypoint limits

---

### 7.3 Firebase Performance Monitoring
**Purpose:** Track app performance metrics

**Metrics:**
- Route calculation duration
- Map rendering time
- Network request latency
- Screen rendering performance

---

## 📋 Implementation Checklist

### Pre-Implementation
- [ ] Review this plan with team
- [ ] Estimate implementation timeline (4-6 hours)
- [ ] Create Firebase project
- [ ] Register Android app
- [ ] Download google-services.json

### Phase 1: Setup (1 hour)
- [ ] Place google-services.json in app/ directory
- [ ] Update root build.gradle.kts
- [ ] Update libs.versions.toml with Firebase BOM
- [ ] Update app/build.gradle.kts with plugin + dependencies
- [ ] Sync Gradle and verify no errors

### Phase 2: Code (2 hours)
- [ ] Create AnalyticsEvents.kt
- [ ] Create AnalyticsParams.kt
- [ ] Create AnalyticsHelper.kt
- [ ] Create AnalyticsModule.kt
- [ ] Update MapViewModel with @Inject analyticsHelper
- [ ] Add tracking calls to key methods
- [ ] Initialize in MapsRoutePickerApp

### Phase 3: Testing (1 hour)
- [ ] Enable debug mode on device
- [ ] Test each tracked event
- [ ] Verify events in Logcat
- [ ] Verify events in Firebase DebugView
- [ ] Fix any issues

### Phase 4: Documentation (30 min)
- [ ] Update README with Analytics section
- [ ] Document tracked events for team
- [ ] Update privacy policy
- [ ] Create analytics dashboard guide

### Phase 5: Production (30 min)
- [ ] Disable debug mode
- [ ] Deploy to internal testing track
- [ ] Monitor for 24-48 hours
- [ ] Verify production data flowing
- [ ] Create custom funnels in Firebase Console

---

## 🐛 Potential Issues & Solutions

### Issue 1: google-services.json Not Found
**Symptom:** Build error "File google-services.json is missing"
**Solution:** 
- Verify file is in `app/` directory
- Check filename spelling (exact: `google-services.json`)
- Sync Gradle again

### Issue 2: Duplicate Play Services Versions
**Symptom:** Build conflict with play-services-*
**Solution:**
- Firebase BOM manages versions automatically
- Remove explicit versions from other Play Services dependencies
- Let BOM handle compatibility

### Issue 3: Events Not Appearing in Console
**Symptom:** DebugView shows nothing
**Solution:**
- Wait 5 minutes (events are batched)
- Verify debug mode: `adb shell setprop debug.firebase.analytics.app com.stonecode.mapsroutepicker`
- Check package name matches exactly
- Restart app after enabling debug mode

### Issue 4: Analytics Disabled in Debug
**Symptom:** No events logged during development
**Solution:**
- Remove `setAnalyticsEnabled(false)` from debug builds
- Or use Firebase DebugView which works regardless

### Issue 5: Too Many Events
**Symptom:** Firebase quota warnings
**Solution:**
- Free tier: 500 events per user/day (extremely high)
- If hit: reduce granular events (e.g., don't track every map pan)
- Focus on meaningful user actions

---

## 📚 Resources & Documentation

### Official Documentation
- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Firebase Analytics Events](https://firebase.google.com/docs/analytics/events)
- [Firebase Best Practices](https://firebase.google.com/docs/analytics/best-practices)

### Internal Documentation
- `docs/setup/API_KEY_SETUP.md` - Similar setup pattern for reference
- `PROJECT.md` - Update with analytics milestone
- `README.md` - Update with analytics section

### Firebase Console Links
- Analytics Dashboard: https://console.firebase.google.com/project/YOUR_PROJECT/analytics
- DebugView: https://console.firebase.google.com/project/YOUR_PROJECT/analytics/debugview
- Events: https://console.firebase.google.com/project/YOUR_PROJECT/analytics/events

---

## 🎯 Success Metrics

After implementation, we should be able to answer:
1. **How many users add waypoints?** (Feature adoption rate)
2. **What's the average route complexity?** (Waypoints per route)
3. **Do users discover navigation mode?** (Navigation start rate)
4. **Where do users get stuck?** (Funnel drop-off points)
5. **What errors are common?** (Error event frequency)

---

## 🔄 Maintenance Plan

### Monthly Review
- Check analytics dashboard
- Review top events
- Identify anomalies or issues
- Adjust tracking as needed

### Quarterly Analysis
- Deep dive into user behavior
- Create custom audiences
- Plan features based on data
- Update tracked events as app evolves

### Version Tracking
- Set user property on app update
- Compare metrics across versions
- Track adoption of new features

---

## 📝 Notes for Next Agent

### Important Context:
- **No Firebase currently** - This is a fresh integration
- **Hilt is already set up** - Use @Inject in ViewModels
- **Compose-only UI** - No XML layouts to worry about
- **Already tracking location** - Privacy policy covers location data
- **Maps API in use** - Similar setup pattern to Google Services

### Quick Start Commands:
```bash
# Sync after adding Firebase
./gradlew --refresh-dependencies

# Test build after changes
./gradlew app:assembleDebug

# Enable debug mode on device
adb shell setprop debug.firebase.analytics.app com.stonecode.mapsroutepicker

# View debug logs
adb logcat | grep -i firebase
```

### Files to Create (in order):
1. `app/google-services.json` (download from Firebase)
2. `analytics/AnalyticsEvents.kt`
3. `analytics/AnalyticsParams.kt`
4. `analytics/AnalyticsHelper.kt`
5. `analytics/AnalyticsModule.kt`

### Files to Modify:
1. `build.gradle.kts` (root) - Add Google Services plugin
2. `gradle/libs.versions.toml` - Add Firebase BOM
3. `app/build.gradle.kts` - Add Firebase dependencies
4. `ui/map/MapViewModel.kt` - Inject and use AnalyticsHelper
5. `MapsRoutePickerApp.kt` - Initialize analytics properties

### Testing Priority:
1. ✅ Build succeeds with Firebase SDK
2. ✅ Events logged to Logcat
3. ✅ Events appear in DebugView
4. ✅ No crashes or ANRs
5. ⭐ Production events flowing after 24h

### Don't Forget:
- Add `google-services.json` to git (it's safe, contains public keys)
- Document tracked events for team visibility
- Create Firebase Console access for team members
- Test on multiple devices/Android versions
- Monitor for 48 hours after deployment

---

**End of Plan**

**Status:** Ready for implementation  
**Next Step:** Create Firebase project and download google-services.json  
**Questions?** Review Firebase setup docs or consult with team lead

**Created by:** AI Agent - Copilot  
**Date:** October 5, 2025  
**Version:** 1.0

