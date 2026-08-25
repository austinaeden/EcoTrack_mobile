package com.example.ecotrack

import android.app.Application
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import org.osmdroid.config.Configuration

class EcoTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. OSMDroid Configuration: Set a TRULY UNIQUE User-Agent first.
        // OSM servers block generic names like "EcoTrack" or anything with "com.example".
        val config = Configuration.getInstance()
        config.userAgentValue = "EcoTrack-Fitness-Personal-Project-Unique-987654321-User"
        
        // Load configuration from shared preferences
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        config.load(this, sharedPrefs)
        
        // 2. Apply Material You dynamic colors
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
