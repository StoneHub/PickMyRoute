#!/bin/bash

# Maps Route Picker - API Key Fix Script
# Run this in WSL to create a properly configured API key

echo "============================================"
echo "Maps Route Picker - API Key Configuration"
echo "============================================"
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo "❌ gcloud CLI not found. Please install it first:"
    echo "   curl https://sdk.cloud.google.com | bash"
    echo "   exec -l \$SHELL"
    exit 1
fi

# Get current project
PROJECT_ID=$(gcloud config get-value project 2>/dev/null)

if [ -z "$PROJECT_ID" ]; then
    echo "❌ No active project. Run 'gcloud init' first."
    exit 1
fi

echo "✅ Active project: $PROJECT_ID"
echo ""

# Step 1: List enabled services
echo "📋 Checking enabled APIs..."
ENABLED_APIS=$(gcloud services list --enabled --format="value(config.name)" 2>/dev/null)

REQUIRED_APIS=(
    "maps-android-backend.googleapis.com"
    "directions-backend.googleapis.com"
    "geocoding-backend.googleapis.com"
    "roads.googleapis.com"
)

MISSING_APIS=()

for api in "${REQUIRED_APIS[@]}"; do
    if echo "$ENABLED_APIS" | grep -q "$api"; then
        echo "   ✅ $api"
    else
        echo "   ❌ $api (MISSING)"
        MISSING_APIS+=("$api")
    fi
done

# Step 2: Enable missing APIs
if [ ${#MISSING_APIS[@]} -gt 0 ]; then
    echo ""
    echo "🔧 Enabling missing APIs..."
    gcloud services enable "${MISSING_APIS[@]}"
    echo "   ✅ APIs enabled"
fi

echo ""
echo "============================================"
echo "API Key Options"
echo "============================================"
echo ""
echo "Choose an option:"
echo ""
echo "1) Create NEW unrestricted API key (recommended for development)"
echo "   - Works immediately"
echo "   - No SHA-1 fingerprint needed"
echo "   - Can be restricted later for production"
echo ""
echo "2) List existing API keys (to check what you have)"
echo ""
echo "3) Update existing key to remove restrictions"
echo ""
read -p "Enter choice (1-3): " choice

case $choice in
    1)
        echo ""
        echo "🔑 Creating unrestricted development API key..."

        # Create unrestricted key
        KEY_NAME="maps-route-picker-dev-$(date +%s)"

        gcloud alpha services api-keys create \
            --display-name="Maps Route Picker - Dev (Unrestricted)" \
            --project="$PROJECT_ID" 2>&1 | tee /tmp/key_output.txt

        # Extract key string from output
        KEY_STRING=$(grep -oP 'keyString: \K[A-Za-z0-9_-]+' /tmp/key_output.txt || \
                     gcloud alpha services api-keys list --filter="displayName:'Maps Route Picker - Dev (Unrestricted)'" --format="value(keyString)" --limit=1)

        if [ -n "$KEY_STRING" ]; then
            echo ""
            echo "============================================"
            echo "✅ SUCCESS! Your new API key:"
            echo "============================================"
            echo ""
            echo "$KEY_STRING"
            echo ""
            echo "📝 Next steps:"
            echo "1. Copy the key above"
            echo "2. Edit: local.properties"
            echo "3. Replace the MAPS_API_KEY line with:"
            echo "   MAPS_API_KEY=$KEY_STRING"
            echo "4. Rebuild the app in Android Studio"
            echo ""
        else
            echo "❌ Failed to extract key. List keys manually:"
            echo "   gcloud alpha services api-keys list"
        fi
        ;;

    2)
        echo ""
        echo "📋 Existing API keys:"
        echo ""
        gcloud alpha services api-keys list --format="table(name,displayName,keyString,restrictions)"
        echo ""
        ;;

    3)
        echo ""
        echo "📋 Available API keys:"
        gcloud alpha services api-keys list --format="table(name,displayName)"
        echo ""
        read -p "Enter the key name (format: projects/xxx/locations/global/keys/xxx): " KEY_NAME

        echo ""
        echo "🔧 Removing restrictions from key..."

        # Update key to remove restrictions
        gcloud alpha services api-keys update "$KEY_NAME" \
            --clear-restrictions

        echo "✅ Restrictions removed. Get the key string:"
        gcloud alpha services api-keys get-key-string "$KEY_NAME"
        ;;

    *)
        echo "Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "============================================"
echo "🔒 Security Notes"
echo "============================================"
echo ""
echo "For PRODUCTION, you should restrict your API key:"
echo ""
echo "1. Get your app's SHA-1 fingerprint:"
echo "   In Android Studio > Gradle > Tasks > android > signingReport"
echo "   Or run: keytool -list -v -keystore ~/.android/debug.keystore"
echo "   (password is usually 'android')"
echo ""
echo "2. Restrict the key:"
echo "   gcloud alpha services api-keys update [KEY_NAME] \\"
echo "     --allowed-application=sha1_fingerprint=[YOUR_SHA1],package_name=com.stonecode.mapsroutepicker"
echo ""
echo "3. Restrict APIs:"
echo "   gcloud alpha services api-keys update [KEY_NAME] \\"
echo "     --api-target=service=maps-android-backend.googleapis.com \\"
echo "     --api-target=service=directions-backend.googleapis.com"
echo ""

