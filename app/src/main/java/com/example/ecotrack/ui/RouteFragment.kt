package com.example.ecotrack.ui

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.ecotrack.R
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class RouteFragment : Fragment() {

    private var map: MapView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_route, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        map = view.findViewById(R.id.map)
        map?.apply {
            // Ensure tile source is set
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            
            // Fix display glitches by disabling hardware acceleration if needed
            // setLayerType(View.LAYER_TYPE_SOFTWARE, null) 

            // Handle Dark Mode for Map Tiles
            val isNightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            if (isNightMode) {
                val inverseMatrix = ColorMatrix(floatArrayOf(
                    -0.9f, 0f, 0f, 0f, 255f,
                    0f, -0.9f, 0f, 0f, 255f,
                    0f, 0f, -0.9f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                ))
                overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(inverseMatrix))
            }

            // Default position (Nairobi)
            val startPoint = GeoPoint(-1.286389, 36.817223)
            controller.setZoom(16.0)
            controller.setCenter(startPoint)

            val marker = Marker(this)
            marker.position = startPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Current Tracking"
            overlays.add(marker)
        }
    }

    override fun onResume() {
        super.onResume()
        map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        map?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map?.onDetach()
        map = null
    }
}
