# API Key Troubleshooting Guide

## Problem: "You must use an API key to authenticate each request"

This error means your Google Maps API key is either:
1. **Not properly configured** - Missing required API restrictions
2. **Restricted incorrectly** - SHA-1 fingerprint doesn't match your debug keystore
3. **Missing required APIs** - Not all necessary services are enabled

---

## Quick Fix (Recommended for Development)

### Run the automated fix script in WSL:

```bash
# Navigate to project
cd /mnt/c/Users/monro/AndroidStudioProjects/MapsRoutePicker

# Make script executable
chmod +x tools/fix_api_key.sh

# Run the script
./tools/fix_api_key.sh
```

The script will:
- ✅ Check if required APIs are enabled
- ✅ Enable any missing APIs
- ✅ Create a new **unrestricted** API key for development
- ✅ Show you the key to copy into `local.properties`

---

## Manual Fix Instructions

### Step 1: Check Current API Key Status

```bash
# In WSL
gcloud config set project YOUR_PROJECT_ID

# List all API keys
gcloud alpha services api-keys list --format="table(name,displayName,restrictions)"
```

### Step 2: Enable Required APIs

Your project needs these APIs enabled:

```bash
gcloud services enable \
  maps-android-backend.googleapis.com \
  directions-backend.googleapis.com \
  geocoding-backend.googleapis.com \
  roads.googleapis.com
```

**Verify they're enabled:**
```bash
gcloud services list --enabled | grep -E "maps|directions|geocoding|roads"
```

### Step 3: Create New Unrestricted Key (Development)

```bash
# Create unrestricted key
gcloud alpha services api-keys create \
  --display-name="Maps Route Picker - Dev Unrestricted"

# Get the key string (copy this!)
gcloud alpha services api-keys list \
  --filter="displayName:'Maps Route Picker - Dev Unrestricted'" \
  --format="value(keyString)" \
  --limit=1
```

### Step 4: Update local.properties

1. Open: `C:\Users\monro\AndroidStudioProjects\MapsRoutePicker\local.properties`
2. Replace the `MAPS_API_KEY` line:
   ```properties
   MAPS_API_KEY=YOUR_NEW_KEY_HERE
   ```
3. Save the file

### Step 5: Rebuild the App

In Android Studio:
1. **Build → Clean Project**
2. **Build → Rebuild Project**
3. Run the app on your device

---

## Why Unrestricted Keys for Development?

**Pros:**
- ✅ Works immediately - no SHA-1 configuration needed
- ✅ Works with debug builds, release builds, different machines
- ✅ No restrictions to debug

**Cons:**
- ⚠️ Less secure - should **never** be used in production
- ⚠️ Anyone who finds the key can use your quota

**For Production:** You MUST add application restrictions (SHA-1 + package name).

---

## Production: Add Restrictions to API Key

### Get Your Release SHA-1 Fingerprint

**Option A: Android Studio**
1. Open Gradle panel (View → Tool Windows → Gradle)
2. Navigate to: `MapsRoutePicker → app → Tasks → android → signingReport`
3. Double-click to run
4. Copy the **SHA-1** from the output

**Option B: Command Line**
```bash
# Debug keystore (for testing)
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android

# Release keystore (for production)
keytool -list -v -keystore /path/to/your/release.keystore -alias your_alias
```

### Apply Restrictions

```bash
# Replace with your actual key name from 'gcloud alpha services api-keys list'
KEY_NAME="projects/YOUR_PROJECT_ID/locations/global/keys/YOUR_KEY_ID"

# Add application restriction
gcloud alpha services api-keys update $KEY_NAME \
  --allowed-application=sha1_fingerprint=YOUR_SHA1_HERE,package_name=com.stonecode.mapsroutepicker

# Add API restrictions
gcloud alpha services api-keys update $KEY_NAME \
  --clear-restrictions \
  --api-target=service=maps-android-backend.googleapis.com \
  --api-target=service=directions-backend.googleapis.com \
  --api-target=service=geocoding-backend.googleapis.com \
  --api-target=service=roads.googleapis.com
```

---

## Common Issues

### Issue: "API key not valid. Please pass a valid API key."
**Cause:** Key doesn't have Maps SDK for Android enabled.
**Fix:** Run `gcloud services enable maps-android-backend.googleapis.com`

### Issue: "This API project is not authorized to use this API."
**Cause:** Directions API not enabled for your project.
**Fix:** Run `gcloud services enable directions-backend.googleapis.com`

### Issue: "The provided API key is invalid."
**Cause:** 
- Typo in `local.properties`
- Key was deleted in Google Cloud Console
- Wrong project selected

**Fix:** 
1. Verify key exists: `gcloud alpha services api-keys list`
2. Check for typos in `local.properties` (no spaces, no quotes)
3. Rebuild project after changes

### Issue: Maps loads but routes fail
**Cause:** Maps SDK API enabled, but Directions API not enabled.
**Fix:** Enable Directions API separately:
```bash
gcloud services enable directions-backend.googleapis.com
```

---

## Checking API Usage & Quotas

### View API Calls
```bash
# Open Google Cloud Console
gcloud console --project=YOUR_PROJECT_ID

# Navigate to: APIs & Services → Dashboard
# Or go to: https://console.cloud.google.com/apis/dashboard
```

### Check Free Tier Status
- **Maps SDK for Android:** Unlimited free
- **Directions API:** 40,000 requests/month free, then $5/1000 requests
- **Geocoding API:** 40,000 requests/month free, then $5/1000 requests

---

## Security Best Practices

### Development (Current)
✅ Use unrestricted key stored in `local.properties` (gitignored)
✅ Never commit keys to Git
✅ Keep quotas low during testing

### Production (Before Release)
1. Create separate production API key
2. Add SHA-1 + package name restrictions
3. Add API service restrictions
4. Consider backend proxy for sensitive routes
5. Monitor usage in Cloud Console
6. Set up billing alerts

### Example Production Key Command
```bash
# Create production key with all restrictions
gcloud alpha services api-keys create \
  --display-name="Maps Route Picker - Production" \
  --allowed-application=sha1_fingerprint=YOUR_RELEASE_SHA1,package_name=com.stonecode.mapsroutepicker \
  --api-target=service=maps-android-backend.googleapis.com \
  --api-target=service=directions-backend.googleapis.com
```

---

## Still Not Working?

### Debug Checklist
- [ ] Run `./gradlew clean` and rebuild
- [ ] Check `local.properties` has no syntax errors
- [ ] Verify APIs enabled: `gcloud services list --enabled`
- [ ] Check internet connection on device
- [ ] Look at Logcat for detailed error messages (filter: "Maps")
- [ ] Try creating a completely new API key
- [ ] Verify billing is enabled on Google Cloud project

### Get Help
1. Check Logcat output in Android Studio
2. Look for HTTP response codes in OkHttp logs
3. Visit: https://console.cloud.google.com/apis/credentials
4. Check: https://developers.google.com/maps/documentation/android-sdk/config

---

## Summary TL;DR

**Quick fix in WSL:**
```bash
cd /mnt/c/Users/monro/AndroidStudioProjects/MapsRoutePicker
chmod +x tools/fix_api_key.sh
./tools/fix_api_key.sh
# Follow prompts, copy key to local.properties, rebuild app
```

