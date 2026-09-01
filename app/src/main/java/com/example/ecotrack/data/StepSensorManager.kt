package com.example.ecotrack.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

/**
 * StepSensorManager handles physical movement detection and daily step persistence.
 */
class StepSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val prefs = context.getSharedPreferences("eco_track_prefs", Context.MODE_PRIVATE)

    // Session-based steps (can be reset by user)
    var currentSteps = 0
        private set

    // Daily accumulated steps (never reset by user, only at midnight)
    var dailyTotal = 0
        private set

    private var initialHardwareSteps = -1
    private var hardwareSessionSteps = 0
    private var virtualSessionSteps = 0
    
    private var lastStepTimestamp: Long = 0
    private val movementThreshold = 1.0f
    private val stepThreshold = 2.5f
    private val stepCooldownMs = 450L
    private val gravity = SensorManager.GRAVITY_EARTH

    private var accumulatedDistanceMeters = 0f
    private val stepDistanceStandard = 1.0f 
    
    private var lastMovementTimestamp: Long = 0
    private val motionTimeoutMs = 2500L 

    private var onStepUpdateListener: ((Int) -> Unit)? = null
    private var onMovementDetectedListener: (() -> Unit)? = null
    private var onDailyTotalListener: ((Int, String) -> Unit)? = null

    init {
        loadDailyTotal()
    }

    private fun loadDailyTotal() {
        val today = getTodayDate()
        val lastSavedDate = prefs.getString("last_daily_date", "")
        if (today == lastSavedDate) {
            dailyTotal = prefs.getInt("daily_total_steps", 0)
        } else {
            // New day detected on app start
            dailyTotal = 0
            prefs.edit().putString("last_daily_date", today).putInt("daily_total_steps", 0).apply()
        }
    }

    private fun getTodayDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun startListening(onStepUpdate: (Int) -> Unit, onMovement: () -> Unit, onDailyUpdate: (Int, String) -> Unit) {
        onStepUpdateListener = onStepUpdate
        onMovementDetectedListener = onMovement
        onDailyTotalListener = onDailyUpdate
        
        stepSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        gyroSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        
        // Initial broadcast
        onStepUpdateListener?.invoke(currentSteps)
        onDailyTotalListener?.invoke(dailyTotal, getTodayDate())
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    fun addVirtualStep(distanceMovedMeters: Float) {
        checkMidnightReset()
        val currentTime = System.currentTimeMillis()
        val isPhysicallyMoving = (currentTime - lastMovementTimestamp) < motionTimeoutMs

        if (distanceMovedMeters < 0.5f || !isPhysicallyMoving) return 
        
        accumulatedDistanceMeters += distanceMovedMeters
        while (accumulatedDistanceMeters >= stepDistanceStandard) {
            virtualSessionSteps++
            dailyTotal++
            accumulatedDistanceMeters -= stepDistanceStandard
        }
        updateTotals()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        checkMidnightReset()
        when (event?.sensor?.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalStepsSinceBoot = event.values[0].toInt()
                if (initialHardwareSteps < 0) initialHardwareSteps = totalStepsSinceBoot
                
                val newHardwareSteps = totalStepsSinceBoot - initialHardwareSteps
                val diff = newHardwareSteps - hardwareSessionSteps
                if (diff > 0) {
                    hardwareSessionSteps = newHardwareSteps
                    dailyTotal += diff
                    updateTotals()
                }
            }
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
            Sensor.TYPE_GYROSCOPE -> {
                val rotation = sqrt(event.values[0] * event.values[0] + 
                                   event.values[1] * event.values[1] + 
                                   event.values[2] * event.values[2])
                if (rotation > 0.4f) {
                    lastMovementTimestamp = System.currentTimeMillis()
                    onMovementDetectedListener?.invoke()
                }
            }
        }
    }

    private fun handleAccelerometer(event: SensorEvent) {
        val magnitude = sqrt(event.values[0] * event.values[0] + 
                             event.values[1] * event.values[1] + 
                             event.values[2] * event.values[2]) - gravity
        val currentTime = System.currentTimeMillis()
        
        if (magnitude > movementThreshold) {
            lastMovementTimestamp = currentTime
            onMovementDetectedListener?.invoke()
        }

        if (magnitude > stepThreshold && (currentTime - lastStepTimestamp) > stepCooldownMs) {
            lastStepTimestamp = currentTime
            virtualSessionSteps++
            dailyTotal++
            updateTotals()
        }
    }

    private fun updateTotals() {
        currentSteps = hardwareSessionSteps + virtualSessionSteps
        onStepUpdateListener?.invoke(currentSteps)
        onDailyTotalListener?.invoke(dailyTotal, getTodayDate())
        
        // Persist daily total
        prefs.edit().putInt("daily_total_steps", dailyTotal).apply()
    }

    private fun checkMidnightReset() {
        val today = getTodayDate()
        val lastSavedDate = prefs.getString("last_daily_date", "")
        if (today != lastSavedDate) {
            // MIDNIGHT RESET
            dailyTotal = 0
            currentSteps = 0
            virtualSessionSteps = 0
            hardwareSessionSteps = 0
            initialHardwareSteps = -1
            accumulatedDistanceMeters = 0f
            
            prefs.edit()
                .putString("last_daily_date", today)
                .putInt("daily_total_steps", 0)
                .apply()
            
            onStepUpdateListener?.invoke(0)
            onDailyTotalListener?.invoke(0, today)
        }
    }

    fun reset() {
        // Only resets the session UI, NOT the daily total
        virtualSessionSteps = 0
        hardwareSessionSteps = 0
        initialHardwareSteps = -1
        currentSteps = 0
        accumulatedDistanceMeters = 0f
        onStepUpdateListener?.invoke(0)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
