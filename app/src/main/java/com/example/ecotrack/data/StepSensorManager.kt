package com.example.ecotrack.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * StepSensorManager handles physical movement detection using three sensors:
 * 1. Step Counter: For accurate walking/running steps.
 * 2. Accelerometer: To detect sub-meter physical movement/shakes.
 * 3. Gyroscope: To detect device orientation/rotation changes.
 */
class StepSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    // Primary sensors
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val prefs = context.getSharedPreferences("eco_track_prefs", Context.MODE_PRIVATE)

    var currentSteps = 0
        private set

    private var initialSteps = -1
    private var hardwareSteps = 0
    private var virtualStepCount = 0
    private var lastStepTimestamp: Long = 0
    private var onStepUpdateListener: ((Int) -> Unit)? = null
    private var onMovementDetectedListener: (() -> Unit)? = null

    // Sensitivity thresholds (m/s^2)
    private val movementThreshold = 1.0f // Threshold to detect walking/shaking motion
    private val stepThreshold = 2.5f     // Threshold for a single sharp step
    private val stepCooldownMs = 450L    // Minimum time between physical steps
    private val gravity = SensorManager.GRAVITY_EARTH

    // Distance tracking for virtual steps
    private var accumulatedDistanceMeters = 0f
    private val stepDistanceStandard = 1.0f // Logic: 1 meter = 1 step
    
    // Intelligent Gating: Tracks if the phone is physically moving (shaking)
    private var lastMovementTimestamp: Long = 0
    private val motionTimeoutMs = 2500L // If no shake in 2.5s, assume user is passive (e.g. in a car)

    /**
     * Starts listening to all motion sensors.
     */
    fun startListening(onStepUpdate: (Int) -> Unit, onMovement: () -> Unit) {
        onStepUpdateListener = onStepUpdate
        onMovementDetectedListener = onMovement
        
        // 1. Hardware Step Counter (Needs ACTIVITY_RECOGNITION permission)
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        
        // 2. Accelerometer (Works without special permissions, perfect for emulators)
        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        
        gyroSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Increments steps based on travel distance, but ONLY if physical motion is detected.
     * This prevents counting steps while traveling in a car or bus.
     */
    fun addVirtualStep(distanceMovedMeters: Float) {
        val currentTime = System.currentTimeMillis()
        val isPhysicallyMoving = (currentTime - lastMovementTimestamp) < motionTimeoutMs

        // If distance is small (GPS jitter) OR user is not physically shaking the phone (in a car)
        // we do NOT count these as steps.
        if (distanceMovedMeters < 0.5f || !isPhysicallyMoving) return 
        
        accumulatedDistanceMeters += distanceMovedMeters
        
        // Every 1 meter = 1 step
        while (accumulatedDistanceMeters >= stepDistanceStandard) {
            virtualStepCount++
            accumulatedDistanceMeters -= stepDistanceStandard
        }
        
        updateTotalSteps()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalStepsSinceBoot = event.values[0].toInt()
                if (initialSteps < 0) initialSteps = totalStepsSinceBoot
                hardwareSteps = totalStepsSinceBoot - initialSteps
                updateTotalSteps()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                handleAccelerometer(event)
            }
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
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(x * x + y * y + z * z) - gravity
        val currentTime = System.currentTimeMillis()
        
        // Update movement timestamp if any significant motion is detected
        if (magnitude > movementThreshold) {
            lastMovementTimestamp = currentTime
            onMovementDetectedListener?.invoke()
        }

        // Detect a sharp "Physical Step" spike
        if (magnitude > stepThreshold && (currentTime - lastStepTimestamp) > stepCooldownMs) {
            lastStepTimestamp = currentTime
            virtualStepCount++
            updateTotalSteps()
        }
    }

    private fun updateTotalSteps() {
        currentSteps = hardwareSteps + virtualStepCount
        onStepUpdateListener?.invoke(currentSteps)
    }

    fun reset() {
        initialSteps = -1
        hardwareSteps = 0
        virtualStepCount = 0
        currentSteps = 0
        accumulatedDistanceMeters = 0f
        prefs.edit().putInt("step_offset", -1).apply()
        onStepUpdateListener?.invoke(0)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
