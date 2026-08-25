package com.example.ecotrack.ui

import android.app.Application
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
import kotlinx.coroutines.launch
import java.io.IOException

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).activityLogDao()
    val allLogs: LiveData<List<ActivityLog>> = dao.getAllLogs().asLiveData()

    private val _weatherState = MutableLiveData<NetworkResult<WeatherResponse>>()
    val weatherState: LiveData<NetworkResult<WeatherResponse>> = _weatherState

    fun fetchWeather(city: String, apiKey: String) {
        _weatherState.value = NetworkResult.Loading

        if (apiKey == "YOUR_OPENWEATHER_API_KEY" || apiKey.isBlank()) {
            _weatherState.value = NetworkResult.Error("Please set a valid OpenWeatherMap API key.")
            return
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getWeather(city, "metric", apiKey)
                if (response.isSuccessful && response.body() != null) {
                    _weatherState.value = NetworkResult.Success(response.body()!!)
                } else {
                    val message = when (response.code()) {
                        401 -> "Invalid or inactive API Key (401). Note: New keys take up to 2 hours to activate."
                        404 -> "City '$city' not found (404)."
                        429 -> "API rate limit exceeded (429)."
                        else -> "Server Error (${response.code()}): ${response.message()}"
                    }
                    _weatherState.value = NetworkResult.Error(message, response.code())
                }
            } catch (e: IOException) {
                _weatherState.value = NetworkResult.Error("No internet connection. Please check your network.")
            } catch (e: Exception) {
                _weatherState.value = NetworkResult.Error("Unexpected Error: ${e.localizedMessage ?: "Unknown"}")
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
}