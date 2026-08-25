# 🌿 EcoTrack

**EcoTrack** is a native Android application designed to help individuals monitor their physical activity alongside live weather conditions. By combining real-time step counting with location-based atmospheric data, EcoTrack empowers users to make informed decisions about their outdoor workouts, track daily progress, and maintain a healthier lifestyle.

---

## How to use EcoTrack

To use EcoTrack, open the app and grant the requested location and activity recognition permissions so it can access your hardware step sensor and local weather data. Upon opening, the home screen immediately displays your live step count alongside your current city’s temperature and humidity. When you finish a workout or daily walk, tap Save Activity to record your session—including your steps and environmental conditions—into the persistent log list below. You can easily clean up past entries by swiping any log item left or right to delete it, and if you remove one by mistake, simply tap UNDO on the notification banner that appears at the bottom of the screen.

---

## 📱 What Can a User Do with EcoTrack?

Whether you are going for a daily walk, tracking a run, or planning outdoor routines, EcoTrack helps you:
* **Track Daily Steps Live:** Monitor your steps continuously using your device's built-in step counter sensors.
* **Check Real-Time Local Weather:** Get current outdoor temperature and humidity updates based on your precise location.
* **Log Workout Sessions:** Save outdoor activities along with step counts and weather context for future reference.
* **Manage Activity History:** Browse past activity logs and remove entries with a quick swipe.
* **Restore Accidentally Deleted Logs:** Undo deleted logs instantly using built-in snackbar notifications.

---

## ✨ Key Features

* 📍 **Automated Location & Weather Sync:** Fetches real-time temperature and humidity via OpenWeatherMap API using `FusedLocationProviderClient`.
* 👟 **Live Hardware Step Counter:** Integrates with Android `ACTIVITY_RECOGNITION` and native step counter sensors for minimal battery drain.
* 🗑️ **Swipe-to-Delete with Undo:** Built using `ItemTouchHelper` with visual canvas drawing for quick and intuitive list management.
* 💾 **Offline-First Storage:** Automatically persists all user activity logs locally so data is never lost.
* 🛡️ **Runtime Permission Handling:** Smooth permission flows for Android 10+ Activity Recognition and Location access.

---

## 🛠️ Architecture & Tech Stack

EcoTrack follows **Modern Android Development (MAD)** practices and **MVVM Architecture** for optimal performance, maintainability, and testing.

* **Language:** [Kotlin](https://kotlinlang.org/)
* **Architecture Pattern:** MVVM (Model-View-ViewModel)
* **UI Components:** Android Jetpack ViewBinding / XML, `RecyclerView`, `ItemTouchHelper`, Material Components (`Snackbar`)
* **Asynchronous Operations:** Kotlin Coroutines & `LiveData`
* **Network & Data:** Retrofit / OkHttp (OpenWeatherMap API integration)
* **Database & Persistence:** Room Database / Jetpack DataStore
* **Location Services:** Google Play Services Location (`FusedLocationProviderClient`)
* **Hardware Sensors:** Android `SensorManager` (`TYPE_STEP_COUNTER`)

---

## 🚀 How to Run the App (Cloning Guide)

If you have cloned this repository and want to run the project on your machine, follow these steps:

### Prerequisites
* **Android Studio:** Ladybug or newer recommended.
* **JDK:** Java 17 or higher.
* **Android Device / Emulator:** API Level 24 (Android 7.0) or higher. An physical device with step counter sensors is recommended for testing live steps.
* **OpenWeatherMap API Key:** Required for live weather features.

---

### Step-by-Step Setup

#### 1. Clone the Repository
Open your terminal and clone the repository:
```bash
git clone [https://github.com/your-username/EcoTrack.git](https://github.com/your-username/EcoTrack.git)
cd EcoTrack