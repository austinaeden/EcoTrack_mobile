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
import com.example.ecotrack.data.StepSensorManager
import com.example.ecotrack.data.FirebaseSyncManager
import com.example.ecotrack.data.RoutePoint
import com.google.android.gms.location.*
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * MainViewModel handles the business logic for the app's main screens.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.activityLogDao()
    private val dailyDao = db.dailyStepDao()
    private val routeDao = db.routePointDao()
    
    private val syncManager = FirebaseSyncManager(db)
    private val stepSensorManager = StepSensorManager(application)

    private val currentUserId = MutableStateFlow("")

    fun setCurrentUser(userId: String) {
        currentUserId.value = userId
    }
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allLogs: LiveData<List<ActivityLog>> = currentUserId.flatMapLatest { userId ->
        if (userId.isEmpty()) flowOf(emptyList())
        else dao.getAllLogs(userId)
    }.asLiveData()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dailyStepHistory: LiveData<List<com.example.ecotrack.data.DailyStep>> = currentUserId.flatMapLatest { userId ->
        if (userId.isEmpty()) flowOf(emptyList())
        else dailyDao.getDailyStepsForUser(userId)
    }.asLiveData()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val routePoints: LiveData<List<GeoPoint>> = currentUserId.flatMapLatest { userId ->
        if (userId.isEmpty()) flowOf(emptyList())
        else routeDao.getPointsForUser(userId).map { points ->
            points.map { GeoPoint(it.latitude, it.longitude) }
        }
    }.asLiveData()

    private val _weatherState = MutableLiveData<NetworkResult<WeatherResponse>>()
    val weatherState: LiveData<NetworkResult<WeatherResponse>> = _weatherState

    private val _currentSteps = MutableLiveData(0)
    val currentSteps: LiveData<Int> = _currentSteps

    private val _dailyTotal = MutableLiveData(0)
    val dailyTotal: LiveData<Int> = _dailyTotal

    init {
        stepSensorManager.startListening(
            onStepUpdate = { steps -> _currentSteps.postValue(steps) },
            onMovement = { onMovementDetected() },
            onDailyUpdate = { total, date ->
                _dailyTotal.postValue(total)
                syncDailySteps(date, total)
            },
            onMidnight = {
                viewModelScope.launch {
                    syncManager.syncDataToCloud(currentUserId.value)
                    // Clear route points for the new day
                    routeDao.deleteOldPoints(System.currentTimeMillis())
                }
            }
        )
    }

    fun resetSteps() = stepSensorManager.reset()
    fun addVirtualStep(distance: Float) = stepSensorManager.addVirtualStep(distance)

    fun clearRoutePoints() {
        val userId = currentUserId.value
        if (userId.isEmpty()) return
        viewModelScope.launch {
            routeDao.deletePointsForUser(userId)
        }
    }

    private val _userLocation = MutableLiveData<Location?>()
    val userLocation: LiveData<Location?> = _userLocation

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            _userLocation.value = location
            saveRoutePoint(location.latitude, location.longitude)
        }
    }

    private fun saveRoutePoint(lat: Double, lon: Double) {
        val userId = currentUserId.value
        if (userId.isEmpty()) return
        viewModelScope.launch {
            routeDao.insertPoint(RoutePoint(userId = userId, latitude = lat, longitude = lon))
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(1000)
            .setMinUpdateDistanceMeters(0f) 
            .build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun onMovementDetected() {
        val lastLoc = _userLocation.value ?: return
        saveRoutePoint(lastLoc.latitude, lastLoc.longitude)
    }

    fun stopLocationUpdates() = fusedLocationClient.removeLocationUpdates(locationCallback)

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

    fun saveLog(title: String, steps: Int, temp: Double) {
        val userId = currentUserId.value
        if (userId.isEmpty()) return
        viewModelScope.launch {
            dao.insertLog(ActivityLog(userId = userId, title = title, stepCount = steps, temperature = temp))
        }
    }

    fun saveLog(log: ActivityLog) = viewModelScope.launch { dao.insertLog(log) }

    fun syncDailySteps(date: String, totalSteps: Int) {
        val userId = currentUserId.value
        if (userId.isEmpty()) return
        viewModelScope.launch {
            dailyDao.insertOrUpdate(com.example.ecotrack.data.DailyStep(date, userId, totalSteps))
        }
    }

    fun triggerCloudSync() = viewModelScope.launch { syncManager.syncDataToCloud(currentUserId.value) }

    fun deleteLog(log: ActivityLog) = viewModelScope.launch { dao.deleteLog(log) }

    fun getWeeklyStepData(logs: List<ActivityLog>): List<Pair<String, Float>> {
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val weeklyData = mutableMapOf<String, Float>()
        for (i in 0 until 7) {
            val dCalendar = java.util.Calendar.getInstance()
            dCalendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dayName = days[dCalendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
            weeklyData[dayName] = 0f
        }
        logs.forEach { log ->
            val diff = (System.currentTimeMillis() - log.timestamp) / (1000 * 60 * 60 * 24)
            if (diff < 7) {
                val logCalendar = java.util.Calendar.getInstance()
                logCalendar.timeInMillis = log.timestamp
                val dayName = days[logCalendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                weeklyData[dayName] = (weeklyData[dayName] ?: 0f) + log.stepCount.toFloat()
            }
        }
        val result = mutableListOf<Pair<String, Float>>()
        for (i in 6 downTo 0) {
            val dCalendar = java.util.Calendar.getInstance()
            dCalendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dayName = days[dCalendar.get(java.util.Calendar.DAY_OF_WEEK) - 1]
            result.add(dayName to (weeklyData[dayName] ?: 0f))
        }
        return result
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
