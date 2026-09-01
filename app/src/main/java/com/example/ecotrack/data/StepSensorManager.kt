package com.example.ecotrack.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences("eco_track_prefs", Context.MODE_PRIVATE)

    var currentSteps = 0
        private set

    private var initialSteps = -1
    private var onStepUpdateListener: ((Int) -> Unit)? = null

    init {
        checkDayChanged()
    }

    private fun checkDayChanged() {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString("last_step_date", "")

        if (today != lastDate) {
            // New day, reset the offset
            prefs.edit().apply {
                putString("last_step_date", today)
                putInt("step_offset", -1)
                apply()
            }
            initialSteps = -1
        } else {
            initialSteps = prefs.getInt("step_offset", -1)
        }
    }

    fun startListening(onStepUpdate: (Int) -> Unit) {
        onStepUpdateListener = onStepUpdate
        checkDayChanged()
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun reset() {
        initialSteps = -1
        currentSteps = 0
        prefs.edit().putInt("step_offset", -1).apply()
        onStepUpdateListener?.invoke(0)
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()

            if (initialSteps < 0) {
                initialSteps = totalStepsSinceBoot
                prefs.edit().putInt("step_offset", initialSteps).apply()
            }

            // Handle device reboot (sensor resets to 0)
            if (totalStepsSinceBoot < initialSteps) {
                initialSteps = totalStepsSinceBoot
                prefs.edit().putInt("step_offset", initialSteps).apply()
            }

            currentSteps = totalStepsSinceBoot - initialSteps
            onStepUpdateListener?.invoke(currentSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
