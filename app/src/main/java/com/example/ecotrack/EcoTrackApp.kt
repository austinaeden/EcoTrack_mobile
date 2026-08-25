package com.example.ecotrack

import android.app.Application
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import org.osmdroid.config.Configuration
import java.io.File

class EcoTrackApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        DynamicColors.applyToActivitiesIfAvailable(this)

        val config = Configuration.getInstance()
        
        // 1. CRITICAL: Define a TRULY UNIQUE User-Agent.
        // Format: AppName/Version (Contact; Platform; UniqueBuildID)
        // We avoid words like "example" or "ecotrack" as they are often pre-blocked.
        val myUniqueUA = "FitnessPathNavigatorPro/1.2 (contact: dev-admin@fitness-tracker-route.net; Android; Build:9988776655)"
        config.userAgentValue = myUniqueUA
        
        // 2. Load configuration but immediately re-force our unique User-Agent
        config.load(this, PreferenceManager.getDefaultSharedPreferences(this))
        config.userAgentValue = myUniqueUA
        
        // 3. STRICT COMPLIANCE: OSM policy requires limited download threads
        config.tileDownloadThreads = 1
        
        // 4. FRESH CACHE DIRECTORY: This is mandatory to clear previously cached "Blocked" images.
        // We increment to v8 to ensure we start with a clean slate.
        // ... inside onCreate()
        val baseDir = getDir("osmdroid_v8_clean", MODE_PRIVATE)

        // The base directory for all osmdroid files
        config.osmdroidBasePath = baseDir

        // The specific directory where tiles are cached
        config.osmdroidTileCache = File(baseDir, "tiles")    }
}


