# API Key Debugging Checklist

## Current Situation
- ✅ API key unrestricted in Google Cloud
- ❌ Map still not loading
- ❌ "Failed to calculate route" error

## Root Cause Analysis

The issue is likely one of these:

### 1. **API Key Not Propagating to App**
Even though the key is in `local.properties`, it might not be compiled into the app.

### 2. **Wrong APIs Enabled**
The key might not have access to the specific APIs needed.

### 3. **Billing Not Enabled**
Google Cloud requires billing enabled even for free tier usage.

---

## Debugging Steps

### Step 1: Verify in Google Cloud Console (Website)

**Go to:** https://console.cloud.google.com/

1. **Select Your Project** (top dropdown)
   - Should see: `maps-route-picker-3259`

2. **Check APIs are Enabled**
   - Go to: **APIs & Services → Dashboard**
   - Click: **+ ENABLE APIS AND SERVICES**
   - Search and verify these are enabled:
     - ✅ **Maps SDK for Android**
     - ✅ **Directions API**
     - ✅ **Geocoding API**
     - ✅ **Roads API**
   
   **OR via direct links:**
   - Maps SDK: https://console.cloud.google.com/apis/library/maps-android-backend.googleapis.com
   - Directions: https://console.cloud.google.com/apis/library/directions-backend.googleapis.com

3. **Check Billing Account**
   - Go to: **Billing → Account Management**
   - Verify: **Billing account is LINKED** to project
   - If not linked, click: **Link a billing account**

4. **Check API Key**
   - Go to: **APIs & Services → Credentials**
   - Find: "Maps Route Picker Android Key"
   - Click the key name
   - Verify:
     - ✅ **Key restrictions**: Should say "None" (for development)
     - ✅ **API restrictions**: Should say "Don't restrict key" (for development)

5. **Check API Usage (to see if requests are coming in)**
   - Go to: **APIs & Services → Dashboard**
   - Look at the graphs
   - If you see requests but errors → API key works, but wrong permissions
   - If you see zero requests → API key not reaching Google

---

### Step 2: Check Android Studio Build Logs

The API key should be visible in the build output:

1. Open Android Studio
2. Click **Build → Rebuild Project**
3. Open **Build** tab at bottom
4. Search for: `MAPS_API_KEY`
5. You should see: `manifestPlaceholders["MAPS_API_KEY"] = AIzaSyBkg...`

If you DON'T see this, the key isn't being read from `local.properties`.

---

### Step 3: Check App's Manifest at Runtime

After building, verify the manifest has the key:

```bash
# In Windows Command Prompt or PowerShell
cd C:\Users\monro\AndroidStudioProjects\MapsRoutePicker

# Extract the compiled manifest
.\gradlew.bat :app:processDebugManifest

# Check the merged manifest
type app\build\intermediates\merged_manifests\debug\AndroidManifest.xml | findstr API_KEY
```

You should see:
```xml
<meta-data android:name="com.google.android.geo.API_KEY" 
           android:value="AIzaSyBkg6g3QaPrWi409mlNSzoSuMN7aAbY7Jo" />
```

If it shows `${MAPS_API_KEY}` instead, the key wasn't substituted.

---

### Step 4: Enable Verbose Logging

Add detailed logging to see exactly what's failing:

**Check Logcat in Android Studio:**
1. Run the app on your device
2. Open **Logcat** tab
3. Set filter to: `Maps` or `Google`
4. Look for red error lines

**Common errors you might see:**
- `API_KEY_INVALID` → Key doesn't exist or typo
- `API_KEY_EXPIRED` → Deleted or regenerated
- `BILLING_NOT_ENABLED` → Need to enable billing
- `PROJECT_INVALID` → Wrong project ID
- `REQUESTS_OVER_QUOTA` → Used up free tier (unlikely)

---

### Step 5: Try Creating Completely New Key

If nothing else works, create a fresh key:

**In WSL:**
```bash
cd /mnt/c/Users/monro/AndroidStudioProjects/MapsRoutePicker
./tools/fix_api_key.sh
# Choose option 1 (Create NEW unrestricted API key)
```

This will create a brand new key with a different ID.

**Then:**
1. Copy the new key
2. Edit `local.properties`
3. Replace `MAPS_API_KEY=...` with new key
4. **Important:** Rebuild project in Android Studio
5. **Uninstall old app** from device (to clear cached API key)
6. Reinstall fresh

---

## Quick Verification Commands (WSL)

```bash
# Check project is active
gcloud config get-value project

# Check billing is enabled
gcloud beta billing projects describe maps-route-picker-3259

# List all enabled APIs
gcloud services list --enabled | grep -E "maps|directions|geocoding|roads"

# Verify your API key exists
gcloud alpha services api-keys list --format="table(displayName,keyString,restrictions)"

# Test if API key works (replace with your key)
curl "https://maps.googleapis.com/maps/api/directions/json?origin=Chicago,IL&destination=Milwaukee,WI&key=AIzaSyBkg6g3QaPrWi409mlNSzoSuMN7aAbY7Jo"
```

The curl command should return JSON with route data, not an error.

---

## Most Likely Causes (In Order)

### 🔴 1. Billing Not Enabled (90% chance)
**Symptom:** APIs enabled, key unrestricted, but still fails
**Fix:** 
- Go to: https://console.cloud.google.com/billing
- Link a billing account to `maps-route-picker-3259`
- Even free tier requires a billing account on file

### 🟡 2. API Key Not Compiled Into App (5% chance)
**Symptom:** Manifest shows `${MAPS_API_KEY}` instead of actual key
**Fix:**
- Check `local.properties` has no syntax errors
- Rebuild project (Build → Clean + Rebuild)
- Uninstall old app, install fresh

### 🟢 3. Wrong Project Selected (3% chance)
**Symptom:** APIs look enabled but key doesn't work
**Fix:**
- Verify gcloud project: `gcloud config get-value project`
- Verify API key is in same project: `gcloud alpha services api-keys list`

### 🔵 4. Cache Issue (2% chance)
**Symptom:** Everything looks correct but fails
**Fix:**
- Uninstall app from device completely
- Delete: `app/build` folder
- Gradle sync + Clean + Rebuild
- Reinstall fresh

---

## Expected Success Indicators

When it's working, you should see:

1. ✅ **Map loads** with Google Maps tiles visible
2. ✅ **Blue dot** shows your current location (after granting permission)
3. ✅ **Error message disappears** or changes to different error
4. ✅ **Logcat shows** successful API requests

---

## Next Steps

**Do this NOW:**

1. **Check Google Cloud Console billing**
   - https://console.cloud.google.com/billing
   - Verify billing account is linked

2. **Check enabled APIs**
   - https://console.cloud.google.com/apis/dashboard
   - Should see all 4 APIs listed

3. **Run curl test** (in WSL):
   ```bash
   curl "https://maps.googleapis.com/maps/api/directions/json?origin=40.7128,-74.0060&destination=40.7589,-73.9851&key=AIzaSyBkg6g3QaPrWi409mlNSzoSuMN7aAbY7Jo"
   ```

4. **If curl fails**, the problem is Google Cloud configuration
5. **If curl succeeds**, the problem is Android app configuration

**Report back what you find and we'll fix it!**

