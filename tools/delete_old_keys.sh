#!/bin/bash

# Delete old exposed API keys from Google Cloud
# These keys were accidentally exposed in documentation

echo "=========================================="
echo "Deleting Old Exposed API Keys"
echo "=========================================="
echo ""

# Set project
PROJECT_ID="maps-route-picker-3259"
gcloud config set project $PROJECT_ID

echo "Fetching list of API keys..."
echo ""

# List all keys with their IDs
gcloud alpha services api-keys list --format="table(name,displayName,keyString)" > /tmp/api_keys.txt
cat /tmp/api_keys.txt

echo ""
echo "=========================================="
echo "Searching for exposed keys to delete..."
echo "=========================================="

# Keys that were exposed in documentation (need to delete these)
EXPOSED_KEYS=(
    "AIzaSyBGR7wAaHZBVQE8wW_cLu3ggy5DhXoKjyM"
    "AIzaSyA6eQU1Q_YWW-WmxzkOY1AoKu-bdm4D9bA"
    "AIzaSyBkg6g3QaPrWi409mlNSzoSuMN7aAbY7Jo"
)

# Your current valid key (DO NOT DELETE)
CURRENT_KEY="AIzaSyDM4yBi9pCaNEEUap1Li9TjPMP9fN8rZn4"

echo ""
echo "⚠️  Will DELETE these exposed keys:"
for key in "${EXPOSED_KEYS[@]}"; do
    echo "  - $key"
done

echo ""
echo "✅ Will KEEP this key (current in local.properties):"
echo "  - $CURRENT_KEY"
echo ""

read -p "Continue with deletion? (y/N): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

echo ""
echo "Deleting exposed keys..."
echo ""

# Delete each exposed key
for key_string in "${EXPOSED_KEYS[@]}"; do
    echo "Looking for key: $key_string"

    # Get the key resource name
    KEY_NAME=$(gcloud alpha services api-keys list \
        --filter="keyString:$key_string" \
        --format="value(name)" \
        --limit=1)

    if [ -z "$KEY_NAME" ]; then
        echo "  ⚠️  Key not found (may already be deleted)"
    else
        echo "  🗑️  Deleting: $KEY_NAME"
        gcloud alpha services api-keys delete "$KEY_NAME" --quiet

        if [ $? -eq 0 ]; then
            echo "  ✅ Deleted successfully"
        else
            echo "  ❌ Failed to delete"
        fi
    fi
    echo ""
done

echo ""
echo "=========================================="
echo "Remaining API Keys:"
echo "=========================================="
gcloud alpha services api-keys list --format="table(displayName,keyString,createTime)"

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "Your current key is safe in local.properties"
echo "Make sure to:"
echo "  1. Never commit local.properties to Git"
echo "  2. Check git history: git log --all --full-history -- local.properties"
echo "  3. If exposed, regenerate the current key too"

