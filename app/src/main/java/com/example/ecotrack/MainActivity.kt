package com.example.ecotrack

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecotrack.data.NetworkResult
import com.example.ecotrack.data.StepSensorManager
import com.example.ecotrack.ui.ActivityAdapter
import com.example.ecotrack.ui.MainViewModel
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var stepSensorManager: StepSensorManager
    private val adapter = ActivityAdapter()

    private lateinit var tvCity: TextView
    private lateinit var tvWeatherTemp: TextView
    private lateinit var tvLiveSteps: TextView

    private var currentTemp: Double = 0.0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startStepCounter()
        } else {
            Toast.makeText(this, "Activity Recognition permission required for step counter", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvCity = findViewById(R.id.tvCity)
        tvWeatherTemp = findViewById(R.id.tvWeatherTemp)
        tvLiveSteps = findViewById(R.id.tvLiveSteps)
        val btnSaveLog = findViewById<Button>(R.id.btnSaveLog)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Step Sensor setup
        stepSensorManager = StepSensorManager(this)

        // Observe ViewModel
        viewModel.allLogs.observe(this) { logs ->
            adapter.submitList(logs)
        }

        // Observe Weather State (Loading, Success, Error)
        viewModel.weatherState.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    tvCity.text = "Fetching weather..."
                    tvWeatherTemp.text = "Please wait..."
                }
                is NetworkResult.Success -> {
                    val weather = result.data
                    currentTemp = weather.main.temp
                    tvCity.text = "Location: ${weather.cityName}"
                    tvWeatherTemp.text = "Temp: ${weather.main.temp}°C | Humidity: ${weather.main.humidity}%"
                }
                is NetworkResult.Error -> {
                    tvCity.text = "Weather Unavailable"
                    tvWeatherTemp.text = "-- °C"
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Fetch Weather
        viewModel.fetchWeather("Nairobi", "d5113f92ed94636874a70fbc79f6c49f")

        // Save activity button action
        btnSaveLog.setOnClickListener {
            val steps = stepSensorManager.currentSteps
            viewModel.saveLog("Outdoor Session", steps, currentTemp)
            Toast.makeText(this, "Activity Saved!", Toast.LENGTH_SHORT).show()
        }

        checkSensorPermissions()

        val deleteBackground = ColorDrawable(Color.RED)
        val deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete)

        val swipeToDeleteCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val logToDelete = adapter.currentList[position]
                    viewModel.deleteLog(logToDelete)

                    Snackbar.make(recyclerView, "Activity log deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO") {
                            viewModel.saveLog(logToDelete.title, logToDelete.stepCount, logToDelete.temperature)
                        }
                        .show()
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMargin = (itemView.height - (deleteIcon?.intrinsicHeight ?: 0)) / 2

                if (dX > 0) { // Swiping Right
                    deleteBackground.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                    deleteIcon?.let { icon ->
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    }
                } else if (dX < 0) { // Swiping Left
                    deleteBackground.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    deleteIcon?.let { icon ->
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - icon.intrinsicWidth
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    }
                } else {
                    deleteBackground.setBounds(0, 0, 0, 0)
                }

                deleteBackground.draw(c)
                deleteIcon?.draw(c)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(recyclerView)
    }

    private fun checkSensorPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                startStepCounter()
            }
        } else {
            startStepCounter()
        }
    }

    private fun startStepCounter() {
        stepSensorManager.startListening { steps ->
            tvLiveSteps.text = "Live Steps: $steps"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stepSensorManager.stopListening()
    }
}