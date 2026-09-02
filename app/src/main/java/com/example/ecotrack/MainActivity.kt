package com.example.ecotrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ecotrack.ui.AnalyticsFragment
import com.example.ecotrack.ui.HomeFragment
import com.example.ecotrack.ui.RouteFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.ecotrack.R
import com.example.ecotrack.ui.MainViewModel
import androidx.activity.viewModels

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Receive the Logged-in User ID (Firebase UID)
        val userId = intent.getStringExtra("USER_ID")
        if (!userId.isNullOrEmpty()) {
            viewModel.setCurrentUser(userId)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Set HomeFragment as the default screen on initial launch
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Handle navigation item selections
        bottomNav.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_map -> RouteFragment()
                R.id.nav_analytics -> AnalyticsFragment()
                else -> HomeFragment()
            }
            loadFragment(selectedFragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}