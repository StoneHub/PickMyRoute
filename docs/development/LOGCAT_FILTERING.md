# Logcat Filtering Tips

Quick reference for filtering logcat output in Android Studio.

---

## Hide Repetitive Frame Rate Logs

### Method 1: Regex Filter (Recommended)

1. Open **Logcat** tab in Android Studio
2. Click the **dropdown** next to the search box (shows "No Filters")
3. Click **Edit Filter Configuration**
4. Click **+** to add new filter
5. Name it: `Hide Frame Rate Spam`
6. In **Log Message (regex)**, enter:
   ```regex
   ^(?!.*setRequestedFrameRate).*$
   ```
7. Click **OK**
8. Select this filter from the dropdown

### Method 2: Negative Filter (Quick)

In the Logcat search box, type:
```
-setRequestedFrameRate
```

This hides all lines containing "setRequestedFrameRate".

---

## Useful App-Specific Filters

### Show Only Your App's Logs
```
package:com.stonecode.mapsroutepicker
```

### Show Map-Related Logs
```
package:com.stonecode.mapsroutepicker tag:MapsRoutePicker|GoogleMap|Directions
```

### Show Only Errors and Warnings
```
package:com.stonecode.mapsroutepicker level:error|warn
```

### Show API Key Issues
```
package:com.stonecode.mapsroutepicker API_KEY|Maps|Google
```

### Show Network Requests
```
package:com.stonecode.mapsroutepicker OkHttp|Retrofit
```

### Show Location Updates
```
package:com.stonecode.mapsroutepicker tag:Location
```

### Show Compose Recompositions (Debug)
```
package:com.stonecode.mapsroutepicker Recomposition|Compose
```

---

## Custom Filter Examples

### Create "Maps Debug" Filter
1. Open filter configuration
2. Create new filter named "Maps Debug"
3. Set:
   - **Package Name**: `com.stonecode.mapsroutepicker`
   - **Log Tag**: `Maps.*` (regex)
   - **Log Level**: Debug or higher

### Create "Errors Only" Filter
1. Create filter named "Errors Only"
2. Set:
   - **Package Name**: `com.stonecode.mapsroutepicker`
   - **Log Level**: Error

---

## Keyboard Shortcuts

- **Ctrl + F** - Focus search box
- **Ctrl + K** - Clear logcat
- **Ctrl + Shift + F** - Open in-logcat search

---

## Logcat Color Coding

- **Verbose** - Gray
- **Debug** - Blue
- **Info** - Green
- **Warn** - Orange
- **Error** - Red
- **Assert** - Purple

---

## Pro Tips

1. **Create multiple filters** for different debugging scenarios
2. **Use regex** for complex patterns
3. **Combine filters** with search box for fine-grained control
4. **Save filters** for team members (share via settings)
5. **Use negative filters** (`-keyword`) to exclude noise

---

**Last Updated:** October 2025

