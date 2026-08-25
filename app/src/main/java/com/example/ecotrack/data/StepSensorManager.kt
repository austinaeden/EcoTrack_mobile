package com.example.ecotrack.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class StepSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    var currentSteps = 0
        private set

    private var initialSteps = -1
    private var onStepUpdateListener: ((Int) -> Unit)? = null

    fun startListening(onStepUpdate: (Int) -> Unit) {
        onStepUpdateListener = onStepUpdate
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()

            if (initialSteps < 0) {
                initialSteps = totalStepsSinceBoot
            }

            currentSteps = totalStepsSinceBoot - initialSteps
            onStepUpdateListener?.invoke(currentSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}