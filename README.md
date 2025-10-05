# PickMyRoute

**Take control of your drive. Choose your roads.**

PickMyRoute is an Android navigation app that lets you force routes through specific roads you prefer, instead of blindly following the algorithm. Perfect for drivers who know better routes, want scenic drives, or need to avoid certain areas.

## 🎯 The Problem It Solves

Google Maps constantly optimizes routes algorithmically, often rerouting you away from roads you specifically want to drive on. PickMyRoute gives you the power to:

- 🗺️ **Set your destination** - Tap anywhere on the map
- 📍 **Add waypoints** - Tap roads you want to drive through
- 🛣️ **Auto-recalculate** - Routes update instantly through your chosen roads
- 🎨 **Visual clarity** - Color-coded route segments for each waypoint
- ↩️ **Easy editing** - Tap waypoint bubbles to remove, with undo support

## ✨ Features

### Core Functionality
- **Custom Waypoint Routing** - Force navigation through specific roads by tapping them
- **Interactive Map** - Fully integrated Google Maps with smooth animations
- **Smart Route Calculation** - Uses Google Directions API with waypoint optimization
- **Color-Coded Routes** - Each route segment gets a unique color matching its waypoint
- **Waypoint Timeline** - Horizontal scrollable widget showing your route progression (🏁 → A → B → C → 🎯)

### User Experience
- **Intuitive Controls** - Google Maps-style FAB controls (My Location, Compass)
- **Swipeable Route Card** - Displays distance and duration with smooth animations
- **Toast Guidance** - Helpful hints for first-time users
- **Undo Support** - Accidentally removed a waypoint? Quick undo via snackbar
- **Loading States** - Elegant full-screen loading during route calculation
- **Error Handling** - Dismissible error messages with helpful fix suggestions

### Technical Highlights
- **100% Jetpack Compose UI** - Modern, declarative Android UI
- **MVVM Architecture** - Clean separation of concerns
- **Hilt Dependency Injection** - Modular, testable code
- **Kotlin Coroutines** - Smooth async operations
- **Location Services** - Real-time location tracking with proper permission handling

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24+ (targeting SDK 34)
- Google Maps API Key (see setup below)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/PickMyRoute.git
   cd PickMyRoute
   ```

2. **Get a Google Maps API Key**
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Enable these APIs:
     - Maps SDK for Android
     - Directions API
     - Geocoding API
   - Create an API key and restrict it to your app's package name

3. **Configure API Key**
   
   Create `local.properties` in the project root (if not exists) and add:
   ```properties
   MAPS_API_KEY=your_actual_api_key_here
   ```

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   # Or just click Run in Android Studio
   ```

## 📱 How to Use

1. **Grant Location Permission** - Allow the app to access your location
2. **Tap Destination** - Tap anywhere on the map to set where you want to go
3. **Add Waypoints** - Tap on roads/locations you want to drive through
4. **Watch Route Update** - The route automatically recalculates through your waypoints
5. **Remove Waypoints** - Tap the colored waypoint bubbles (A, B, C...) to remove them
6. **Navigate** - Follow the color-coded route on the map

## 🏗️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Maps**: Google Maps SDK for Android (Compose)
- **Networking**: Retrofit + OkHttp
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Jetpack Navigation Compose

## 📂 Project Structure

```
app/src/main/
├── java/com/stonecode/mapsroutepicker/
│   ├── data/              # API clients, repositories
│   ├── domain/            # Models, repository interfaces
│   ├── ui/                # Compose UI components
│   │   ├── map/           # Main map screen & ViewModel
│   │   ├── components/    # Reusable UI components
│   │   └── permissions/   # Permission handling
│   └── util/              # Utilities (polyline decoder, etc.)
```

## 🎨 Screenshots

*Coming soon*

## 🛣️ Roadmap

- [ ] Drag to reorder waypoints
- [ ] Save favorite routes
- [ ] Share routes with others
- [ ] Turn-by-turn voice guidance
- [ ] Traffic data integration
- [ ] Places API search for destinations
- [ ] Offline route caching

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Google Maps Platform for mapping and routing services
- Jetpack Compose team for the amazing UI toolkit
- The Android developer community

## 📧 Contact

Project Link: [https://github.com/yourusername/PickMyRoute](https://github.com/yourusername/PickMyRoute)

---

**PickMyRoute** - *Your route, your way.*

