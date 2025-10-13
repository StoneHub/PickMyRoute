# Firebase Dependency Management Rules

## Problem We Solved
Firebase Analytics dependencies were failing to resolve with "Unresolved reference" errors for `Firebase`, `analytics`, `logEvent`, and `param` extension functions.

## Root Causes Identified
1. **Missing KTX imports** - Firebase KTX extension functions require explicit imports
2. **Version catalog misconfiguration** - Firebase dependencies need special handling with BOM (Bill of Materials)

## Rules to Follow

### Rule 1: Always Use Version Catalog for Dependencies
**DO:**
```kotlin
// In app/build.gradle.kts
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.analytics)
implementation(libs.firebase.crashlytics)
```

**DON'T:**
```kotlin
// ❌ Never hardcode versions in build.gradle.kts
implementation("com.google.firebase:firebase-analytics-ktx:22.5.0")
implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
```

**Why:** All version numbers should live in `gradle/libs.versions.toml` for centralized management. Hardcoding defeats the purpose of the version catalog.

### Rule 2: Firebase BOM Pattern - No Version Numbers on Managed Dependencies
**DO:**
```toml
# In gradle/libs.versions.toml
[versions]
firebaseBom = "34.4.0"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }  # No version!
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }  # No version!
```

**DON'T:**
```toml
# ❌ Don't add versions to BOM-managed dependencies
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx", version = "22.5.0" }
```

**Why:** The BOM (Bill of Materials) automatically provides compatible versions for all Firebase libraries. Adding explicit versions overrides the BOM and can cause resolution failures or version conflicts.

### Rule 3: Always Import Firebase KTX Extension Functions
**DO:**
```kotlin
'import com.google.firebase.analytics.ktx.analytics
'import com.google.firebase.analytics.ktx.logEvent
'import com.google.firebase.ktx.Firebase
```

For files using the `param` DSL:
```kotlin
'import com.google.firebase.analytics.ktx.logEvent
'import com.google.firebase.analytics.ktx.param  // Required for param {} DSL
```

**DON'T:**
```kotlin
// ❌ Missing imports will cause "Unresolved reference" errors
'import com.google.firebase.analytics.FirebaseAnalytics  // Not enough!
'import com.google.firebase.ktx.Firebase  // Missing logEvent and param
```

**Why:** Firebase KTX extension functions are not automatically available. The Kotlin compiler needs explicit imports to resolve `logEvent {}` and `param()` DSL functions.

### Rule 4: Apply Google Services Plugin in App Module
**DO:**
```kotlin
// In app/build.gradle.kts plugins block
plugins {
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)  // If using Crashlytics
}
```

**Why:** The `google-services` plugin processes `google-services.json` and generates required Firebase configuration. Without it, Firebase may not initialize properly.

### Rule 5: Keep google-services.json Package Name in Sync
**Verify:**
- `google-services.json` → `client[].client_info.android_client_info.package_name`
- Must match `app/build.gradle.kts` → `android.defaultConfig.applicationId`

**Why:** Mismatched package names prevent Firebase from recognizing your app.

## Troubleshooting Checklist

When you see "Unresolved reference" errors for Firebase:

1. ✅ Check imports - are `logEvent` and `param` imported from `com.google.firebase.analytics.ktx`?
2. ✅ Check `gradle/libs.versions.toml` - do Firebase library entries have NO version numbers?
3. ✅ Check `app/build.gradle.kts` - are you using `libs.firebase.*` aliases, not hardcoded strings?
4. ✅ Sync Gradle - did you run "Sync Project with Gradle Files" after changes?
5. ✅ Check External Libraries - does Android Studio show `firebase-analytics-ktx` in the project dependencies?

## Migration Checklist

When updating Firebase versions:

1. Update ONLY the BOM version in `gradle/libs.versions.toml`:
   ```toml
   firebaseBom = "34.4.0"  # Update this single line
   ```
2. Sync Gradle
3. Check for deprecation warnings (new Firebase versions may deprecate old APIs)
4. Run all builds to ensure compatibility

## Quick Reference

### Correct Firebase Setup (Complete Example)

**gradle/libs.versions.toml:**
```toml
[versions]
firebaseBom = "34.4.0"
googleServices = "4.4.4"
firebaseCrashlyticsGradle = "3.0.6"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
firebase-crashlytics = { group = "com.google.firebase", name = "firebase-crashlytics-ktx" }

[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
firebase-crashlytics = { id = "com.google.firebase.crashlytics", version.ref = "firebaseCrashlyticsGradle" }
```

**app/build.gradle.kts:**
```kotlin
plugins {
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}
```

**Kotlin source file using Analytics:**
```kotlin
'import com.google.firebase.analytics.FirebaseAnalytics
'import com.google.firebase.analytics.ktx.analytics
'import com.google.firebase.analytics.ktx.logEvent
'import com.google.firebase.analytics.ktx.param
'import com.google.firebase.ktx.Firebase

// Now you can use:
Firebase.analytics.logEvent("my_event") {
    param("key", "value")
}
```

## Summary
- **One place for versions:** `gradle/libs.versions.toml`
- **BOM manages Firebase library versions:** Don't add versions to individual Firebase dependencies
- **Always import KTX extensions:** `logEvent`, `param`, `analytics` need explicit imports
- **Use catalog aliases:** Never hardcode dependency coordinates in `build.gradle.kts`

