# Project Cleanup Recommendations

This document outlines recommendations for cleaning up and professionalizing the MapsRoutePicker project.

## ✅ Completed Improvements

### Repository Structure
1. ✅ **Added LICENSE file** (MIT License) - Essential for open source
2. ✅ **Enhanced .gitignore** - Comprehensive Android/Gradle exclusions
3. ✅ **Added CONTRIBUTING.md** - Clear contribution guidelines
4. ✅ **Added SECURITY.md** - Security policy and API key protection guide
5. ✅ **Created local.properties.example** - Template for secure configuration
6. ✅ **Added .editorconfig** - Consistent code formatting across editors
7. ✅ **Added GitHub templates**:
   - Pull request template
   - Bug report issue template
   - Feature request issue template
8. ✅ **Added CI/CD workflow** - GitHub Actions for automated builds

### Code Improvements
1. ✅ **Cleaned up strings.xml** - Removed boilerplate, added professional strings
2. ✅ **Configured ProGuard rules** - Comprehensive rules for release builds
3. ✅ **Improved build.gradle.kts** - Better error messaging for missing API key
4. ✅ **Updated README.md** - Added security warnings and better setup instructions

## 🔧 Recommended Next Steps

### 1. Documentation Consolidation (Priority: Medium)
**Issue**: 15 markdown files in docs/ - some may be redundant

**Recommendation**:
- Review and consolidate similar docs (e.g., API_KEY_DEBUGGING.md, API_KEY_TROUBLESHOOTING.md)
- Consider moving internal docs (LESSONS_LEARNED.md) to a separate /internal or /notes folder
- Keep only user-facing documentation in docs/
- Archive completed tasks (UX_IMPROVEMENTS_COMPLETE.md) or remove if covered in git history

**Suggested Structure**:
```
docs/
├── setup/
│   ├── GOOGLE_CLOUD_SETUP.md (keep)
│   └── API_KEY_SETUP.md (consolidate troubleshooting docs here)
├── development/
│   ├── ARCHITECTURE.md (new - explain code structure)
│   └── TESTING.md (new - testing guidelines)
└── api/
    ├── DIRECTIONS_API.md
    ├── GEOCODING_API.md
    └── PLACES_API.md
```

### 2. Code Quality (Priority: High)

**Add ktlint or detekt** for code style enforcement:
```kotlin
// In build.gradle.kts
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}
```

**Add unit tests** - Test folders exist but are empty:
- ViewModel tests (MapViewModel)
- Repository tests
- Utility function tests (polyline decoder, etc.)

### 3. Release Configuration (Priority: High)

**Enable ProGuard/R8 for release builds**:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true  // Change to true
        isShrinkResources = true  // Add this
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

**Add signing configuration** (for Play Store releases):
- Create a keystore
- Configure signing in build.gradle.kts
- Document the process in CONTRIBUTING.md

### 4. App Naming Consistency (Priority: Medium)

**Current Issue**: Mixed naming
- Package: `mapsroutepicker`
- README: `PickMyRoute`
- Module: `MapsRoutePicker`

**Recommendation**: Choose one name and apply consistently
- **Option 1**: Rename to "PickMyRoute" everywhere (matches branding)
- **Option 2**: Keep "MapsRoutePicker" (matches package, less work)

If choosing PickMyRoute:
1. Update `settings.gradle.kts`: `rootProject.name = "PickMyRoute"`
2. Update all references in build files
3. Update AndroidManifest.xml theme names
4. Consider renaming GitHub repository

### 5. Unused Resources (Priority: Low)

**strings.xml**: Now cleaned ✅

**themes.xml**: Check if these are used:
- Theme.MapsRoutePicker.NoActionBar
- AppBarOverlay
- PopupOverlay

Since the app uses 100% Compose, traditional themes may not be needed.

### 6. Version Management (Priority: Medium)

**Add version catalog aliases** for all plugins in libs.versions.toml:
```toml
[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-android = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

Add missing version:
```toml
ksp = "2.0.21-1.0.28"
```

### 7. Security Audit (Priority: CRITICAL)

**⚠️ IMMEDIATE ACTION REQUIRED**:

Your `local.properties` file contains your API key. Ensure it's never committed to Git.

**ACTION ITEMS**:
1. ✅ `.gitignore` now excludes `local.properties`
2. 🔴 **Check git history** - if this file was ever committed:
   ```bash
   git log --all --full-history -- local.properties
   ```
3. 🔴 If it was committed, the key is compromised - you MUST:
   - Delete the key in Google Cloud Console
   - Create a new key
   - Consider this key public (anyone can use it)
4. ✅ `local.properties.example` template created for safe sharing

**Restrict the new API key**:
- Application restrictions: Android apps only
- Package name: `com.stonecode.mapsroutepicker`
- SHA-1: Your app's certificate fingerprint
- API restrictions: Only enable Maps SDK, Directions API, Geocoding API

### 8. GitHub Repository Setup (Priority: High)

**Configure repository settings**:
1. Add description and topics (Android, Kotlin, Jetpack-Compose, Google-Maps, Navigation)
2. Add website URL (if you have one)
3. Enable Issues
4. Enable Discussions (for community questions)
5. Set up branch protection rules for `main`:
   - Require pull request reviews
   - Require status checks (CI) to pass
   - Require branches to be up to date

### 9. Additional Professional Touches

**Add badges to README**:
```markdown
![Android CI](https://github.com/yourusername/MapsRoutePicker/workflows/Android%20CI/badge.svg)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)
```

**Add app screenshots** - The README has a placeholder

**Create a demo GIF/video** showing the app in action

**Add CHANGELOG.md** for tracking version history

## 📊 Summary

### High Priority (Do First)
- 🔴 **SECURITY**: Regenerate exposed API key
- Configure release builds with ProGuard enabled
- Add unit tests
- Set up signing configuration

### Medium Priority (Next Steps)
- Consolidate documentation
- Resolve naming inconsistencies
- Add code quality tools (ktlint/detekt)
- Complete version catalog setup

### Low Priority (Nice to Have)
- Clean up unused resources
- Add screenshots/demo
- Add badges to README
- Set up GitHub Discussions

## 🎯 Result

After implementing these changes, your project will:
- ✅ Look professional and production-ready
- ✅ Be secure (API keys properly managed)
- ✅ Follow Android best practices
- ✅ Be welcoming to contributors
- ✅ Have clear documentation
- ✅ Be ready for Play Store release

## 📝 Tools Created

New files added for professionalism:
- `LICENSE` - MIT License
- `CONTRIBUTING.md` - Contribution guidelines
- `SECURITY.md` - Security policy
- `local.properties.example` - Configuration template
- `.editorconfig` - Code style consistency
- `.github/PULL_REQUEST_TEMPLATE.md`
- `.github/ISSUE_TEMPLATE/bug_report.md`
- `.github/ISSUE_TEMPLATE/feature_request.md`
- `.github/workflows/android-ci.yml` - Automated CI
- Enhanced `.gitignore`
- Updated `strings.xml` with professional strings
- Configured `proguard-rules.pro` with comprehensive rules
- Improved README.md with security warnings
