# 🌿 EcoTrack

**EcoTrack** is a native Android application built with Kotlin that enables users to track physical activity alongside live weather metrics and outdoor routes. Designed using Modern Android Development (MAD) practices and MVVM architecture, EcoTrack seamlessly bridges hardware step sensors, real-time location-based weather data, and interactive navigation into a unified experience.

---

## 🎯 How EcoTrack Helps You

* **Informed Workout Planning:** See current temperature and humidity before heading out so you can adjust your pace or hydration needs.
* **Seamless Daily Tracking:** Automatic background step sensing requires no manual start/stop toggles, preserving battery life while keeping track of your movement.
* **Activity Contextualization:** Record outdoor sessions with captured weather parameters, creating a historical record of your performance across different weather conditions.
* **Effortless UI Navigation:** Easily transition between your live daily feed, mapping routes, and activity analytics through an intuitive bottom navigation interface.

---

## 📱 What You Can Do

* **Monitor Steps in Real Time:** Track steps continuously using your device's native hardware step sensor.
* **View Local Weather Conditions:** Automatically fetch your current city's temperature and humidity via GPS location services.
* **Log Activity Sessions:** Tap **Save Activity Log** to store workout sessions complete with step count and current temperature.
* **Manage Activity History:** Browse logged sessions in a list and swipe left or right to delete them.
* **Undo Accidental Deletes:** Instantly restore any deleted activity log using the built-in **UNDO** banner.
* **Navigate Dedicated Views:** Switch between your **Home Dashboard**, **Route Tracker**, and **Analytics** via the bottom navigation bar.

---

## 🚀 How to Use EcoTrack

1. **Permissions:** Launch the app and grant **Location** and **Activity Recognition** permissions when prompted.
2. **View Live Conditions:** The **Home** tab immediately displays your current city, live temperature, humidity, and active step count.
3. **Save a Log:** After completing a walk or run, tap **Save Activity Log** to persist your progress and environmental conditions to your history list.
4. **Delete or Undo:** Swipe any item left or right on the history list to delete it. Tap **UNDO** on the bottom popup banner if you deleted an entry by mistake.
5. **Switch Tabs:** Use the **Bottom Navigation Bar** at the bottom of the screen to switch between **Home**, **Route**, and **Analytics**.

---

## ✨ Key Technical Features

* 🧩 **Single-Activity Architecture:** Uses `MainActivity` as a lightweight fragment host, managing screen swaps across `HomeFragment`, `RouteFragment`, and `AnalyticsFragment` via `BottomNavigationView`.
* 🎨 **Material You Dynamic Colors:** Integrates `DynamicColors.applyToActivitiesIfAvailable()` in custom `Application` class (`EcoTrackApp`) to adapt the app UI to the device wallpaper on Android 12+ (API 31+).
* 📍 **GPS & Weather Integration:** Leverages Google Play Services `FusedLocationProviderClient` and OpenWeatherMap API for dynamic geolocation-based weather fetching (with automatic fallback to default locations).
* 👟 **Hardware Step Counter:** Integrates directly with Android's native `SensorManager` (`TYPE_STEP_COUNTER`) for hardware-level tracking with low power overhead.
* 🗑️ **Interactive Canvas Gestures:** Features custom `ItemTouchHelper` implementation with visual background drawing and dynamic swipe margins for smooth list deletions.
* 🔄 **Shared State Management:** Utilizes Activity-scoped `MainViewModel` (`activityViewModels()`) across fragments to maintain consistent state throughout screen transitions.

---

## 🛠️ Architecture & Tech Stack

EcoTrack follows **Android Jetpack** guidelines and **MVVM (Model-View-ViewModel)** pattern:

* **Language:** Kotlin
* **Architecture:** Single-Activity Architecture + MVVM
* **UI Components:** Material Design 3 (MaterialCardView, MaterialButton, BottomNavigationView), Fragments, `RecyclerView`, `ItemTouchHelper`, `Snackbar`
* **Navigation:** Custom Fragment Transactions anchored in `FrameLayout`
* **Asynchronous Operations:** Kotlin Coroutines & `LiveData`
* **Network Services:** Retrofit / OkHttp (OpenWeatherMap API)
* **Location & Hardware:** Google Location Services (`FusedLocationProviderClient`), Android `SensorManager`
* **Personalization:** Material Dynamic Colors (`DynamicColors`)

---

## ⚙️ Project Structure

```text
com.example.ecotrack/
 ├── EcoTrackApp.kt              # App-level config (Dynamic Colors init)
 ├── MainActivity.kt             # Fragment host & BottomNavigationView logic
 ├── data/
 │    ├── NetworkResult.kt       # Sealed class for network state management
 │    └── StepSensorManager.kt   # Hardware step sensor wrapper
 └── ui/
      ├── HomeFragment.kt        # Home screen UI logic & location handlers
      ├── RouteFragment.kt       # Route mapping view
      ├── AnalyticsFragment.kt   # Progress analytics view
      ├── MainViewModel.kt       # Shared activity-scoped ViewModel
      └── ActivityAdapter.kt     # RecyclerView adapter for activity logs
```

---

## 🔧 Setup & Installation

### Prerequisites

- **Android Studio:** Ladybug or newer recommended.
- **JDK:** Java 17.
- **Min SDK:** API Level 24 (Android 7.0+).
- **Target SDK:** API Level 34 / 35.
- **Physical Device:** Recommended for hardware step sensor and GPS testing.

### Step-by-Step Guide

#### 1. Clone the Repository

```bash
git clone https://github.com/your-username/EcoTrack.git
cd EcoTrack
```

#### 2. Configure OpenWeatherMap & Map API Key

Ensure your API key is configured inside your project (`HomeFragment.kt` or `secrets.properties` depending on your setup):

```kotlin
private val apiKey = "YOUR_OPENWEATHERMAP_API_KEY"
```

#### 3. Register Application Class

Ensure `.EcoTrackApp` is registered in `app/src/main/AndroidManifest.xml`:

```xml
<application
    android:name=".EcoTrackApp"
    android:label="@string/app_name"
    ... >
```

#### 4. Build & Run

Open the project in Android Studio, sync Gradle files, and run the app on an emulator or connected physical Android device.
