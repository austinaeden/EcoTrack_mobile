package com.example.ecotrack

import android.app.Application
import com.google.android.material.color.DynamicColors
import org.osmdroid.config.Configuration
import java.io.File

class EcoTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Apply Material You dynamic colors
        DynamicColors.applyToActivitiesIfAvailable(this)

        // OSMDroid Global Configuration
        val config = Configuration.getInstance()
        
        // 1. Set a unique User-Agent to comply with Tile Usage Policy
        config.userAgentValue = packageName

        // 2. Configure cache directory using externalCacheDir for better cleanup and performance
        val cacheDir = externalCacheDir ?: cacheDir
        val osmdroidDir = File(cacheDir, "osmdroid")
        
        config.osmdroidBasePath = osmdroidDir
        config.osmdroidTileCache = File(osmdroidDir, "tiles")
    }
}
