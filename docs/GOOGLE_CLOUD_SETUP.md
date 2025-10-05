# Google Cloud Setup Guide

## Prerequisites
- Google Cloud account with billing enabled (free credits work)
- WSL (Windows Subsystem for Linux) installed
- Terminal access

## Step 1: Install Google Cloud CLI in WSL

Open your WSL terminal and run:

```bash
# Download and install gcloud CLI
curl https://sdk.cloud.google.com | bash

# Restart shell to update PATH
exec -l $SHELL

# Verify installation
gcloud --version
```

## Step 2: Initialize gcloud

```bash
# Login to your Google account
gcloud init

# Follow the prompts:
# 1. Login with your Google account (opens browser)
# 2. Select or create a project
# 3. Set default region (optional, choose closest to you)
```

## Step 3: Create Project

Choose a unique project ID (lowercase, numbers, hyphens only):

```bash
# Replace <unique-id> with something unique like: maps-route-picker-12345
PROJECT_ID="maps-route-picker-<unique-id>"

# Create the project
gcloud projects create $PROJECT_ID --name="Maps Route Picker"

# Set as active project
gcloud config set project $PROJECT_ID

# Link billing account (REQUIRED even with free credits)
# List your billing accounts
gcloud billing accounts list

# Link billing (replace BILLING_ACCOUNT_ID with ID from above)
gcloud billing projects link $PROJECT_ID --billing-account=BILLING_ACCOUNT_ID
```

## Step 4: Enable Required APIs

```bash
# Enable all Maps APIs we'll use
gcloud services enable \
  maps-android-backend.googleapis.com \
  directions-backend.googleapis.com \
  geocoding-backend.googleapis.com \
  roads.googleapis.com \
  places-backend.googleapis.com

# Verify APIs are enabled
gcloud services list --enabled | grep maps
```

## Step 5: Create API Key

### Option A: Unrestricted Key (Development Only)
```bash
# Create API key for development (no restrictions)
gcloud alpha services api-keys create \
  --display-name="Maps Route Picker - Development"

# Get the API key value
gcloud alpha services api-keys list --format="value(name,keyString)"
```

### Option B: Restricted Key (Recommended)
```bash
# Create restricted API key
gcloud alpha services api-keys create \
  --display-name="Maps Route Picker - Android" \
  --api-target=service=maps-android-backend.googleapis.com \
  --api-target=service=directions-backend.googleapis.com \
  --api-target=service=geocoding-backend.googleapis.com \
  --api-target=service=roads.googleapis.com

# Get the key
gcloud alpha services api-keys list
```

**Note:** Copy the `keyString` value - you'll need it!

## Step 6: Add API Key to Project

### In Windows (not WSL)

1. Open your project in Android Studio

2. Open or create `local.properties` (already exists in your project):
   ```
   # File: C:\Users\monro\AndroidStudioProjects\MapsRoutePicker\local.properties
   
   sdk.dir=C\:\\Users\\monro\\AppData\\Local\\Android\\Sdk
   MAPS_API_KEY=YOUR_API_KEY_HERE
   ```

3. Replace `YOUR_API_KEY_HERE` with the key from Step 5

4. **IMPORTANT:** `local.properties` is already in `.gitignore` - your key is safe

## Step 7: Get SHA-1 Fingerprint (for API restrictions)

### Debug Certificate (for development)

In Windows PowerShell or Command Prompt:

```cmd
cd C:\Users\monro\AndroidStudioProjects\MapsRoutePicker

# Get debug keystore SHA-1
.\gradlew signingReport
```

Look for the `SHA-1` under `Variant: debug`:
```
SHA-1: AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12
```

### Add SHA-1 to API Key (via Google Cloud Console)

**Option 1: Via CLI (if supported)**
```bash
# Not directly supported via CLI - use console
```

**Option 2: Via Web Console (Recommended)**
1. Go to https://console.cloud.google.com/apis/credentials
2. Select your project
3. Click on your API key
4. Under "Application restrictions":
   - Select "Android apps"
   - Click "Add an item"
   - Package name: `com.stonecode.mapsroutepicker`
   - SHA-1: (paste from signingReport)

## Step 8: Configure Android Project

### Update AndroidManifest.xml

Add API key and permissions:

```xml
<manifest>
    <!-- Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <application>
        <!-- Maps API Key -->
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="${MAPS_API_KEY}" />
        
        <!-- Your activities -->
    </application>
</manifest>
```

### Update app/build.gradle.kts

Access the API key in BuildConfig:

```kotlin
android {
    defaultConfig {
        // ...
        
        // Expose API key to BuildConfig
        buildConfigField("String", "MAPS_API_KEY", "\"${project.findProperty("MAPS_API_KEY") ?: ""}\"")
    }
    
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true  // Enable BuildConfig
    }
}
```

## Step 9: Test API Access

Create a simple test to verify APIs work:

```kotlin
// In your app
suspend fun testDirectionsApi() {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/maps/api/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val api = retrofit.create(DirectionsApi::class.java)
    
    val response = api.getDirections(
        origin = "34.0522,-118.2437",
        destination = "34.1016,-118.3288",
        apiKey = BuildConfig.MAPS_API_KEY
    )
    
    Log.d("API Test", "Status: ${response.status}")
    // Should log "Status: OK"
}
```

## Troubleshooting

### "API key not found" error
- Check `local.properties` has correct key
- Rebuild project: `./gradlew clean build`
- Verify API key in BuildConfig: `Log.d("API", BuildConfig.MAPS_API_KEY)`

### "This API key is not authorized"
- Wait 5 minutes after creating key (propagation)
- Check API restrictions match your app package
- Verify SHA-1 fingerprint is correct
- Ensure APIs are enabled in Cloud Console

### "Quota exceeded"
- Check billing is enabled
- Verify you have free credits available
- Check current usage: https://console.cloud.google.com/apis/dashboard

### WSL gcloud command not found after install
```bash
# Add to PATH manually
export PATH=$PATH:$HOME/google-cloud-sdk/bin
echo 'export PATH=$PATH:$HOME/google-cloud-sdk/bin' >> ~/.bashrc
source ~/.bashrc
```

## Monitoring Usage

### Check API usage (Web Console)
https://console.cloud.google.com/google/maps-apis/metrics

### Set up budget alerts
```bash
# Create budget alert (web console is easier)
# Go to: https://console.cloud.google.com/billing/budgets
```

### Estimated Usage (MVP Development)
- **Maps SDK:** FREE (mobile use)
- **Directions API:** ~50-100 requests during dev (well within free tier)
- **Geocoding:** ~20-50 requests during dev
- **Total cost:** $0 (within free tier)

## Security Best Practices

### ✅ DO:
- Use `local.properties` for API keys (already gitignored)
- Add SHA-1 restrictions to keys
- Use API restrictions (limit to needed APIs)
- Monitor usage regularly
- Rotate keys if exposed

### ❌ DON'T:
- Commit API keys to Git
- Use unrestricted keys in production
- Share keys publicly
- Use same key across multiple apps

## Next Steps

After completing setup:
1. ✅ API key added to `local.properties`
2. ✅ SHA-1 added to key restrictions
3. ✅ APIs enabled in Cloud Console
4. 🚀 Ready to start development!

## Quick Reference

```bash
# Common commands
gcloud config get-value project              # Show current project
gcloud services list --enabled               # List enabled APIs
gcloud alpha services api-keys list          # List API keys
gcloud billing accounts list                 # List billing accounts

# Switch projects
gcloud config set project PROJECT_ID

# Disable API (if needed)
gcloud services disable SERVICE_NAME
```

## Resources
- [Google Cloud Console](https://console.cloud.google.com)
- [Maps Platform Console](https://console.cloud.google.com/google/maps-apis)
- [Billing Dashboard](https://console.cloud.google.com/billing)
- [API Metrics](https://console.cloud.google.com/google/maps-apis/metrics)

