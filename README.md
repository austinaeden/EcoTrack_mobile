# 🌿 EcoTrack

**EcoTrack** is a high-precision physical activity and environmental tracking application for Android. Built with Kotlin and modern Jetpack components, it uniquely correlates fitness movement with real-time weather metrics, providing users with a comprehensive context for their outdoor activities.

---

## 🌟 Key Features

### 🚀 High-Precision Motion Engine
*   **Hybrid Tracking:** Combines `FusedLocationProviderClient` for sub-meter GPS accuracy with hardware **Accelerometer** and **Gyroscope** sensor fusion.
*   **Intelligent Step Logic:** Implements a strict **1 Meter = 1 Step** ratio. The app intelligently distinguishes between active walking and passive travel (like being in a car) by gating GPS movement with physical device vibration detection.
*   **Persistent Route Trails:** Automatically records your walking path throughout the day. Even if the app is closed, your trail is saved in the local database and visible upon return.
*   **Real-Time Responsiveness:** 1-second update intervals ensure your live route polyline and step count are always in sync with your actual movement.

### 🔐 Advanced Security & Multi-User Support
*   **Hybrid Authentication:** Seamlessly integrates **Firebase Authentication** for cloud identity with a **Local Room Database** fallback. 
*   **Offline Access:** Supports offline login by securely caching hashed credentials, allowing you to access your data even in remote areas without signal.
*   **Secure Sign-Up:** Enhanced registration flow with password confirmation and strict validation rules.
*   **Data Isolation:** All activity logs, step history, and route points are cryptographically associated with a unique Firebase UID.

### 📊 Comprehensive Analytics & History
*   **Cloud Sync:** Integrated **Firebase Firestore** support. Data is automatically backed up at midnight or can be manually synced via the Settings page.
*   **Weekly Trends:** Visualizes your activity performance over the last 7 days using interactive bar charts.
*   **Persistent Daily History:** Tracks your total daily steps in a dedicated history list that survives manual session resets.
*   **Midnight Reset:** Automatically finalizes daily totals, uploads data to the cloud, and resets live counters at exactly 12:00 AM.

### 🗺️ Interactive Mapping
*   **Live Polyline:** Draws a continuous blue trail of your daily movement on the map.
*   **Custom Controls:** Includes onscreen **Zoom In/Out** buttons and a **Reset Path** button to manually clear your trail when starting a new journey.
*   **Auto-Centering:** The map smoothly animates to follow your current location in real-time.

### 🎨 Personalization & Support
*   **Theme Switching:** Full support for **Dark Mode** and Light Mode, with preferences persisted across app restarts using SharedPreferences.
*   **Material You:** Adapts UI colors dynamically based on the device's wallpaper (Android 12+).
*   **Direct Feedback:** Integrated support system allowing users to send feedback directly to the developer's email from within the app.

---

## 🛠️ Architecture & Tech Stack

*   **Language:** 100% Kotlin
*   **Architecture:** MVVM (Model-View-ViewModel) + Single-Activity Architecture
*   **Database:** Room Persistence Library (Offline first)
*   **Cloud:** Firebase Auth (Identity) & Firestore (Backup)
*   **Networking:** Retrofit 2 & GSON
*   **Mapping:** OSMDroid (OpenStreetMap) + Carto Voyager Tiles
*   **Charts:** MPAndroidChart
*   **Hardware:** FusedLocationProvider (GPS), SensorManager (Accelerometer, Gyroscope, Step Counter)

---

## ⚙️ Project Structure

```text
com.example.ecotrack/
 ├── EcoTrackApp.kt              # Global configuration & Theme initialization
 ├── MainActivity.kt             # Main navigation host
 ├── data/
 │    ├── AppDatabase.kt         # Room database configuration (v7)
 │    ├── ActivityLog.kt         # Activity session entity
 │    ├── DailyStep.kt           # All-day step history entity
 │    ├── RoutePoint.kt          # Persistent map coordinate entity
 │    ├── LocalUser.kt           # Offline credential caching
 │    ├── FirebaseSyncManager.kt # Cloud backup logic
 │    ├── StepSensorManager.kt   # Sensor fusion & step calculation logic
 │    └── RetrofitClient.kt      # Weather API integration
 └── ui/
      ├── LoginActivity.kt       # Hybrid Online/Offline Auth logic
      ├── HomeFragment.kt        # Live dashboard & movement tracking
      ├── RouteFragment.kt       # Interactive route mapping & path drawing
      ├── AnalyticsFragment.kt   # Graphs and daily history lists
      ├── SettingsFragment.kt    # Theme control, Cloud Sync, and Feedback
      └── MainViewModel.kt       # Shared state & business logic
```

---

## 🔧 Setup & Installation

### Prerequisites
- **Android Studio:** Ladybug or newer.
- **Firebase:** Requires `google-services.json` in the `/app` folder.
- **API Key:** OpenWeatherMap API key required in `HomeFragment.kt`.

### Implementation Steps
1.  **Firebase Setup:** Enable "Email/Password" sign-in provider in your Firebase Console and initialize Firestore in "Test Mode".
2.  **Permissions:** Grant **Location** (Fine/Coarse) and **Physical Activity** permissions.
3.  **Testing Tracking:** Use the **Virtual Sensors** (Device Pose) in the emulator to simulate shaking while moving along a GPS route to trigger step increments.

---

## 👨‍💻 Developer
Developed with by **Austin Aeden**.  
For support or feedback, please use the in-app feedback tool or contact: `austinaeden@gmail.com`
