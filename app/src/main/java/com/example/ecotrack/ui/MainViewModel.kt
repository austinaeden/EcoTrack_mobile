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
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).activityLogDao()
    val allLogs: LiveData<List<ActivityLog>> = dao.getAllLogs().asLiveData()

    private val _weatherState = MutableLiveData<NetworkResult<WeatherResponse>>()
    val weatherState: LiveData<NetworkResult<WeatherResponse>> = _weatherState

    // Shared location data
    private val _userLocation = MutableLiveData<Location?>()
    val userLocation: LiveData<Location?> = _userLocation

    // Track coordinates for the route line
    private val _routePoints = MutableLiveData<List<GeoPoint>>(emptyList())
    val routePoints: LiveData<List<GeoPoint>> = _routePoints

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            _userLocation.value = location
            
            // Append point to the route
            val currentPoints = _routePoints.value?.toMutableList() ?: mutableListOf()
            currentPoints.add(GeoPoint(location.latitude, location.longitude))
            _routePoints.value = currentPoints
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        // Immediate update from last known location
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

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

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
        viewModelScope.launch {
            dao.insertLog(ActivityLog(title = title, stepCount = steps, temperature = temp))
        }
    }

    fun deleteLog(log: ActivityLog) {
        viewModelScope.launch {
            dao.deleteLog(log)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
