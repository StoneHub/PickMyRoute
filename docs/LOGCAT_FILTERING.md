# Logcat Filter Configuration

## To hide repetitive "setRequestedFrameRate" logs in Android Studio:

### Method 1: Use Regex Filter (Recommended)

1. Open **Logcat** tab in Android Studio
2. Click the **dropdown** next to the search box (shows "No Filters")
3. Click **Edit Filter Configuration**
4. Click **+** to add new filter
5. Name it: `Hide Frame Rate Spam`
6. In **Log Message (regex)**, enter:
   ```
   ^(?!.*setRequestedFrameRate).*$
   ```
7. Click **OK**
8. Select this filter from the dropdown

### Method 2: Create Negative Filter

1. Open **Logcat** tab
2. In the search box, type:
   ```
   -setRequestedFrameRate
   ```
3. This will hide all lines containing "setRequestedFrameRate"

### Method 3: Filter by Package

Instead of seeing all logs, filter to only your app's important logs:

1. In Logcat search box, type:
   ```
   package:com.stonecode.mapsroutepicker tag:MapsRoutePicker|GoogleMap|Directions
   ```

This shows only logs from your package with relevant tags.

## Useful Logcat Filters for Debugging

### See only Map-related errors:
```
package:com.stonecode.mapsroutepicker level:error|warn
```

### See API key issues:
```
package:com.stonecode.mapsroutepicker API_KEY|Maps|Google
```

### See network requests:
```
package:com.stonecode.mapsroutepicker OkHttp|Retrofit
```

### See your custom logs only:
```
tag:MapsRoutePicker
```

## Pro Tip: Save Custom Filters

Create and save multiple filters:
- **Debug Everything**: No filters
- **Errors Only**: `level:error`
- **Clean View**: Hide frame rate + other spam
- **Network Only**: `OkHttp|Retrofit`

Switch between them quickly using the dropdown!

