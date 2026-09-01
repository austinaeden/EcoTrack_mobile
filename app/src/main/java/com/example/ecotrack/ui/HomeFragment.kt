package com.example.ecotrack.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ecotrack.R
import com.example.ecotrack.data.NetworkResult
import com.example.ecotrack.data.StepSensorManager
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    
    private lateinit var stepSensorManager: StepSensorManager
    private val adapter = ActivityAdapter()

    private lateinit var tvCity: TextView
    private lateinit var tvWeatherTemp: TextView
    private lateinit var tvLiveSteps: TextView

    private var lastLocation: android.location.Location? = null
    private var lastWeatherLocation: android.location.Location? = null
    private var currentTemp: Double = 0.0
    private val apiKey = "d5113f92ed94636874a70fbc79f6c49f"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startStepCounter()
    }

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            viewModel.startLocationUpdates()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvCity = view.findViewById(R.id.tvCity)
        tvWeatherTemp = view.findViewById(R.id.tvWeatherTemp)
        tvLiveSteps = view.findViewById(R.id.tvLiveSteps)
        val btnSaveLog = view.findViewById<Button>(R.id.btnSaveLog)
        val btnResetSteps = view.findViewById<Button>(R.id.btnResetSteps)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        stepSensorManager = StepSensorManager(requireContext())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        btnResetSteps.setOnClickListener {
            stepSensorManager.reset()
            Toast.makeText(requireContext(), "Steps Reset!", Toast.LENGTH_SHORT).show()
        }

        // Observe Logs
        viewModel.allLogs.observe(viewLifecycleOwner) { logs ->
            adapter.submitList(logs)
        }

        // Observe Shared Location for Weather Updates and "GPS Walking" steps
        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            location?.let { newLoc ->
                // Only fetch weather if we haven't fetched it yet, or if the user moved > 1km
                if (lastWeatherLocation == null || lastWeatherLocation!!.distanceTo(newLoc) > 1000) {
                    viewModel.fetchWeatherByLocation(newLoc.latitude, newLoc.longitude, apiKey)
                    lastWeatherLocation = newLoc
                }
                
                // Distance-based step calculation
                lastLocation?.let { oldLoc ->
                    val distance = oldLoc.distanceTo(newLoc)
                    
                    // Filter out GPS Jitter: Only count movement if it's > 1.0 meter
                    if (distance > 1.0f) {
                        stepSensorManager.addVirtualStep(distance)
                    }
                }
                lastLocation = newLoc
            }
        }

        // Observe Weather State
        viewModel.weatherState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    tvCity.text = "Loading Weather..."
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
                }
            }
        }

        btnSaveLog.setOnClickListener {
            val steps = stepSensorManager.currentSteps
            viewModel.saveLog("Activity Session", steps, currentTemp)
            Toast.makeText(requireContext(), "Activity Saved!", Toast.LENGTH_SHORT).show()
        }

        checkSensorPermissions()
        checkLocationPermissions()
        setupSwipeToDelete(recyclerView)
    }

    private fun setupSwipeToDelete(recyclerView: RecyclerView) {
        val deleteBackground = ColorDrawable(Color.RED)
        val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false
            
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val log = adapter.currentList[position]
                viewModel.deleteLog(log)
                Snackbar.make(recyclerView, "Log deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") { viewModel.saveLog(log) }
                    .show()
            }

            override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, state: Int, active: Boolean) {
                val itemView = vh.itemView
                if (dX > 0) deleteBackground.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                else if (dX < 0) deleteBackground.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                deleteBackground.draw(c)
                super.onChildDraw(c, rv, vh, dX, dY, state, active)
            }
        }).attachToRecyclerView(recyclerView)
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startLocationUpdates()
        } else {
            requestLocationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun checkSensorPermissions() {
        // We always start the counter because the Accelerometer fallback 
        // doesn't require ACTIVITY_RECOGNITION permission.
        startStepCounter()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    private fun startStepCounter() {
        stepSensorManager.startListening(
            onStepUpdate = { steps ->
                tvLiveSteps.text = "Current Steps: $steps"
            },
            onMovement = {
                viewModel.onMovementDetected()
            },
            onDailyUpdate = { dailyTotal, date ->
                // Sync the "All Day" total to the database for statistics
                viewModel.syncDailySteps(date, dailyTotal)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stepSensorManager.stopListening()
    }
}