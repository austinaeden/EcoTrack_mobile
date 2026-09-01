package com.example.ecotrack.ui

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.ecotrack.data.ActivityLog
import com.example.ecotrack.data.AppDatabase
import com.example.ecotrack.data.NetworkResult
import com.example.ecotrack.data.RetrofitClient
import com.example.ecotrack.data.WeatherResponse
import com.google.android.gms.location.*
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * MainViewModel handles the business logic for the app's main screens.
 * It manages location tracking, weather data fetching, and database operations.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).activityLogDao()

    private val currentUserId = MutableStateFlow(-1)

    /**
     * Sets the active user ID to filter logs and associate new activities.
     */
    fun setCurrentUser(userId: Int) {
        currentUserId.value = userId
    }
    
    /**
     * Observes activity logs scoped specifically to the current logged-in user.
     * Automatically updates whenever the user changes or the database is updated.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allLogs: LiveData<List<ActivityLog>> = currentUserId.flatMapLatest { userId ->
        if (userId == -1) kotlinx.coroutines.flow.flowOf(emptyList())
        else dao.getAllLogs(userId)
    }.asLiveData()

    /**
     * Represents the current state of the weather network request (Loading, Success, or Error).
     */
    private val _weatherState = MutableLiveData<NetworkResult<WeatherResponse>>()
    val weatherState: LiveData<NetworkResult<WeatherResponse>> = _weatherState

    /**
     * Holds the most recent GPS location of the user.
     */
    private val _userLocation = MutableLiveData<Location?>()
    val userLocation: LiveData<Location?> = _userLocation

    /**
     * Stores a list of coordinates visited during the current session to draw a route on the map.
     */
    private val _routePoints = MutableLiveData<List<GeoPoint>>(emptyList())
    val routePoints: LiveData<List<GeoPoint>> = _routePoints

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    /**
     * Callback that triggers whenever the GPS provides a new location update.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            _userLocation.value = location
            
            // Append the new coordinate to the route points for map drawing
            val currentPoints = _routePoints.value?.toMutableList() ?: mutableListOf()
            currentPoints.add(GeoPoint(location.latitude, location.longitude))
            _routePoints.value = currentPoints
        }
    }

    /**
     * Configures and starts high-accuracy location tracking.
     * It requests updates every 5 seconds.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        // Get the last known location immediately for a faster initial UI response
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && _userLocation.value == null) {
                _userLocation.value = location
                _routePoints.value = listOf(GeoPoint(location.latitude, location.longitude))
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * Stops requesting location updates to conserve device battery.
     */
    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Fetches current weather data from OpenWeatherMap API using latitude and longitude.
     * Updates [weatherState] with the result.
     */
    fun fetchWeatherByLocation(lat: Double, lon: Double, apiKey: String) {
        _weatherState.value = NetworkResult.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getWeatherByCoordinates(lat, lon, "metric", apiKey)
                if (response.isSuccessful && response.body() != null) {
                    _weatherState.value = NetworkResult.Success(response.body()!!)
                } else {
                    _weatherState.value = NetworkResult.Error("Location not found", response.code())
                }
            } catch (e: Exception) {
                _weatherState.value = NetworkResult.Error("Network failure: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Saves a new activity log linked to the current user.
     */
    fun saveLog(title: String, steps: Int, temp: Double) {
        val userId = currentUserId.value
        if (userId == -1) return
        
        viewModelScope.launch {
            dao.insertLog(ActivityLog(userId = userId, title = title, stepCount = steps, temperature = temp))
        }
    }

    /**
     * Re-saves a log (useful for UNDO functionality).
     */
    fun saveLog(log: ActivityLog) {
        viewModelScope.launch {
            dao.insertLog(log)
        }
    }

    /**
     * Deletes a specific activity log from the local database.
     */
    fun deleteLog(log: ActivityLog) {
        viewModelScope.launch {
            dao.deleteLog(log)
        }
    }

    /**
     * Processes raw activity logs to generate a summary of steps for the last 7 days.
     * This is useful for displaying charts/graphs in the UI.
     * @return A list of Pairs containing the day name (e.g., "Mon") and the total steps.
     */
    fun getWeeklyStepData(logs: List<ActivityLog>): List<Pair<String, Float>> {
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val weeklyData = mutableMapOf<String, Float>()

        // Initialize last 7 days with 0 to ensure the chart has no gaps
        for (i in 0 until 7) {
            val dCalendar = java.util.Calendar.getInstance()
            dCalendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dayName = days[dCalendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
            weeklyData[dayName] = 0f
        }

        // Add up all steps for logs that occurred within the last week
        logs.forEach { log ->
            val logCalendar = java.util.Calendar.getInstance()
            logCalendar.timeInMillis = log.timestamp
            
            val diff = (System.currentTimeMillis() - log.timestamp) / (1000 * 60 * 60 * 24)
            if (diff < 7) {
                val dayName = days[logCalendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                weeklyData[dayName] = (weeklyData[dayName] ?: 0f) + log.stepCount.toFloat()
            }
        }

        // Return the data sorted from 6 days ago up to today
        val result = mutableListOf<Pair<String, Float>>()
        for (i in 6 downTo 0) {
            val dCalendar = java.util.Calendar.getInstance()
            dCalendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dayName = days[dCalendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
            result.add(dayName to (weeklyData[dayName] ?: 0f))
        }
        return result
    }

    /**
     * Called when the ViewModel is no longer used (e.g., when the Activity is finished).
     * Ensures that location tracking stops to prevent memory leaks or battery drain.
     */
    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
