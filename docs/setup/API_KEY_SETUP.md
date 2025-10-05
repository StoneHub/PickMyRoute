# Google Maps API Key Setup & Troubleshooting

Complete guide for setting up and troubleshooting Google Maps API keys for the Maps Route Picker app.

---

## Table of Contents
1. [Initial Setup](#initial-setup)
2. [Creating API Keys](#creating-api-keys)
3. [Troubleshooting](#troubleshooting)
4. [Security Best Practices](#security-best-practices)

---

## Initial Setup

### Prerequisites
- Google Cloud account with billing enabled (free credits work)
- WSL (Windows Subsystem for Linux) for CLI setup (optional)
- Android Studio

### Step 1: Create Google Cloud Project

**Via Web Console:**
1. Go to https://console.cloud.google.com/
2. Click "Select a project" → "New Project"
3. Name: "Maps Route Picker" (or your preference)
4. Note your Project ID (e.g., `maps-route-picker-3259`)

**Via CLI (WSL):**
```bash
# Install gcloud CLI
curl https://sdk.cloud.google.com | bash
exec -l $SHELL

# Create project
PROJECT_ID="maps-route-picker-$(date +%s)"
gcloud projects create $PROJECT_ID --name="Maps Route Picker"
gcloud config set project $PROJECT_ID
```

### Step 2: Enable Billing

⚠️ **CRITICAL**: Billing must be enabled even for free tier usage.

1. Go to https://console.cloud.google.com/billing
2. Link a billing account to your project
3. Free tier includes:
   - Maps SDK for Android: Unlimited
   - Directions API: 40,000 requests/month
   - Geocoding API: 40,000 requests/month

### Step 3: Enable Required APIs

**Via Web Console:**
1. Go to https://console.cloud.google.com/apis/library
2. Search and enable each:
   - **Maps SDK for Android**
   - **Directions API**
   - **Geocoding API**
   - **Roads API** (optional)

**Via CLI:**
```bash
gcloud services enable \
  maps-android-backend.googleapis.com \
  directions-backend.googleapis.com \
  geocoding-backend.googleapis.com \
  roads.googleapis.com

# Verify
gcloud services list --enabled | grep -E "maps|directions|geocoding|roads"
```

---

## Creating API Keys

### Development Key (Quick Start)

**Automated Script (Recommended):**
```bash
cd /mnt/c/Users/monro/AndroidStudioProjects/MapsRoutePicker
chmod +x tools/fix_api_key.sh
./tools/fix_api_key.sh
```

**Manual Creation via CLI:**
```bash
# Create unrestricted key for development
gcloud alpha services api-keys create \
  --display-name="Maps Route Picker - Dev"

# Get the key string
gcloud alpha services api-keys list --format="table(displayName,keyString)"
```

**Via Web Console:**
1. Go to https://console.cloud.google.com/apis/credentials
2. Click "Create Credentials" → "API Key"
3. Copy the key immediately
4. For development: Leave unrestricted

### Add Key to Project

1. Open `local.properties` in your project root
2. Add or update:
   ```properties
   sdk.dir=C\:\\Users\\YourUser\\AppData\\Local\\Android\\Sdk
   MAPS_API_KEY=AIzaSyB...YourKeyHere
   ```
3. Save the file (already in `.gitignore` - safe from Git)

### Rebuild App

In Android Studio:
1. **Build → Clean Project**
2. **Build → Rebuild Project**
3. **Uninstall old app** from device
4. Run app fresh

---

## Troubleshooting

### Map Not Loading / "Failed to calculate route"

**Verify APIs are enabled:**
```bash
# Check enabled services
gcloud services list --enabled | grep -E "maps|directions|geocoding"

# Should see:
# - maps-android-backend.googleapis.com
# - directions-backend.googleapis.com
# - geocoding-backend.googleapis.com
```

**Check billing:**
```bash
gcloud beta billing projects describe YOUR_PROJECT_ID
```

**Test API key with curl:**
```bash
curl "https://maps.googleapis.com/maps/api/directions/json?origin=Chicago,IL&destination=Milwaukee,WI&key=YOUR_KEY_HERE"
```
Should return JSON with route data, not an error.

### "API key not valid"

**1. Check key is in build:**
```cmd
cd C:\Users\monro\AndroidStudioProjects\MapsRoutePicker
.\gradlew.bat :app:processDebugManifest
type app\build\intermediates\merged_manifests\debug\AndroidManifest.xml | findstr API_KEY
```

Should show actual key, not `${MAPS_API_KEY}`.

**2. Check local.properties syntax:**
- No quotes around the key
- No spaces around the `=`
- Key on single line
- Correct format: `MAPS_API_KEY=AIzaSyB...`

**3. Rebuild completely:**
```cmd
.\gradlew.bat clean
# Delete app from device
# Rebuild in Android Studio
# Reinstall fresh
```

### "This API project is not authorized"

**Cause:** Specific API not enabled for your project.

**Fix:**
- Map loads but route fails → Enable Directions API
- Address search fails → Enable Geocoding API
- General errors → Enable Maps SDK for Android

### Logcat Debugging

**In Android Studio:**
1. Run app on device
2. Open **Logcat** tab (bottom)
3. Filter by: `Maps` or `Google` or `API`
4. Look for red error lines

**Common errors:**
- `API_KEY_INVALID` → Typo or wrong key
- `BILLING_NOT_ENABLED` → Link billing account
- `PROJECT_INVALID` → Wrong project ID
- `API_NOT_ENABLED` → Enable missing API

### Still Not Working?

**Complete reset:**
1. Create completely new API key in Cloud Console
2. Delete `app/build` folder
3. Update `local.properties` with new key
4. Clean + Rebuild project
5. Uninstall app completely from device
6. Reinstall fresh

**Check propagation time:**
- New keys take 1-5 minutes to activate
- API enablement takes 30 seconds - 2 minutes
- Wait a bit and try again

---

## Security Best Practices

### Development Environment ✅

**Current setup (secure for development):**
- ✅ API key in `local.properties` (gitignored)
- ✅ Unrestricted key for easy testing
- ✅ Never committed to Git

**Keep it secure:**
- Never commit `local.properties` to Git
- Don't share keys in screenshots/logs
- Monitor usage regularly

### Production Environment 🔒

**Before releasing to Play Store:**

1. **Create production API key:**
   ```bash
   gcloud alpha services api-keys create \
     --display-name="Maps Route Picker - Production"
   ```

2. **Get your release SHA-1:**
   ```cmd
   keytool -list -v -keystore path\to\release.keystore -alias your_alias
   ```

3. **Add application restrictions:**
   - Go to https://console.cloud.google.com/apis/credentials
   - Click your production key
   - Set "Application restrictions" → "Android apps"
   - Add package: `com.stonecode.mapsroutepicker`
   - Add SHA-1 from your release keystore

4. **Add API restrictions:**
   - Set "API restrictions" → "Restrict key"
   - Select only:
     - Maps SDK for Android
     - Directions API
     - Geocoding API

5. **Use separate keys for debug/release:**
   ```kotlin
   // In build.gradle.kts
   buildTypes {
       debug {
           buildConfigField("String", "MAPS_API_KEY", 
               "\"${project.findProperty("MAPS_API_KEY_DEBUG") ?: ""}\"")
       }
       release {
           buildConfigField("String", "MAPS_API_KEY", 
               "\"${project.findProperty("MAPS_API_KEY_RELEASE") ?: ""}\"")
       }
   }
   ```

### If Key is Exposed

**Immediate actions:**
1. ⚠️ Delete the key in Google Cloud Console immediately
2. Create a new key with restrictions
3. Check git history: `git log --all --full-history -- local.properties`
4. If committed: Consider the key public forever

**Check for exposure:**
```bash
# Search git history for API keys
git log --all --source --full-history -S "AIzaSy" --pretty=format:"%h %s"
```

---

## Quick Reference Commands

```bash
# View current project
gcloud config get-value project

# List API keys
gcloud alpha services api-keys list

# List enabled APIs
gcloud services list --enabled

# Enable an API
gcloud services enable maps-android-backend.googleapis.com

# Check billing
gcloud billing accounts list
gcloud beta billing projects describe PROJECT_ID

# Test Directions API
curl "https://maps.googleapis.com/maps/api/directions/json?origin=40.7128,-74.0060&destination=40.7589,-73.9851&key=YOUR_KEY"
```

---

## Monitoring & Quotas

### View Usage
- Dashboard: https://console.cloud.google.com/google/maps-apis/metrics
- Billing: https://console.cloud.google.com/billing

### Free Tier Limits
- **Maps SDK:** Unlimited (mobile)
- **Directions:** 40,000 requests/month free
- **Geocoding:** 40,000 requests/month free
- **Cost after free tier:** $5 per 1,000 requests

### Set Budget Alerts
1. Go to https://console.cloud.google.com/billing/budgets
2. Create budget alert (e.g., $5 warning)
3. Get email notifications before charges

---

## Expected Success Indicators

When everything is working correctly:

✅ **Map loads** with Google Maps tiles  
✅ **Blue dot** shows current location  
✅ **Route calculation** works without errors  
✅ **Logcat shows** successful API responses  
✅ **No red errors** in Logcat related to Maps

---

## Additional Resources

- [Google Cloud Console](https://console.cloud.google.com)
- [Maps Platform Docs](https://developers.google.com/maps/documentation/android-sdk)
- [Directions API Docs](https://developers.google.com/maps/documentation/directions)
- [Billing Information](https://developers.google.com/maps/billing-and-pricing/pricing)
- [Project GitHub](https://github.com/yourusername/MapsRoutePicker)

---

## Need Help?

1. Check Logcat for detailed errors
2. Verify all APIs are enabled in Cloud Console
3. Ensure billing is linked to project
4. Try the automated fix script: `./tools/fix_api_key.sh`
5. Review this guide's troubleshooting section

**Last Updated:** October 2025

