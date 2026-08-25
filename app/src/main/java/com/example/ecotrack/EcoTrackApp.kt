package com.example.ecotrack

import android.app.Application
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import org.osmdroid.config.Configuration

class EcoTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Apply Material You dynamic colors
        DynamicColors.applyToActivitiesIfAvailable(this)
        
        // Initialize OpenStreetMap configuration globally
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName
    }
}
