package com.example.ecotrack.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.ecotrack.R
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import android.graphics.Color
import android.widget.Button

class RouteFragment : Fragment() {

    private var map: MapView? = null
    private val viewModel: MainViewModel by activityViewModels()
    
    private val CARTO_API_KEY = "eyJhbGciOiJIUzI1NiJ9.eyJhIjoiYWNfOGM3bXAyaWgiLCJqdGkiOiI0NmYyZjg5NCIsImV4cCI6MTc5MDQyNDg0MH0._RFowJ3wxbOfDCYPA0bHq1qgJPyeGn0pygo0-r7mHIw"

    // Carto Voyager Tile Source with API Key
    private val CARTO_VOYAGER = object : XYTileSource(
        "CartoVoyager",
        0, 20, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
        ),
        "Map tiles by CartoDB, under CC BY 3.0. Data by OpenStreetMap, under ODbL."
    ) {
        override fun getTileURLString(pTileIndex: Long): String {
            return baseUrl + MapTileIndex.getZoom(pTileIndex) + "/" + 
                   MapTileIndex.getX(pTileIndex) + "/" + MapTileIndex.getY(pTileIndex) + 
                   mImageFilenameEnding + "?api_key=" + CARTO_API_KEY
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
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
    ): View? = inflater.inflate(R.layout.fragment_route, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        map = view.findViewById(R.id.map)
        map?.apply {
            setTileSource(CARTO_VOYAGER)
            setMultiTouchControls(true)
            
            // Enable built-in Zoom In/Out buttons
            setBuiltInZoomControls(true)
            
            controller.setZoom(16.0)
            
            // Set fallback center (Nairobi)
            val nairobi = GeoPoint(-1.286389, 36.817223)
            controller.setCenter(nairobi)
        }

        val btnResetPath = view.findViewById<Button>(R.id.btnResetPath)
        btnResetPath.setOnClickListener {
            viewModel.clearRoutePoints()
        }

        // Observe shared user location for center and marker
        viewModel.userLocation.observe(viewLifecycleOwner) { location ->
            location?.let {
                val geoPoint = GeoPoint(it.latitude, it.longitude)
                map?.controller?.animateTo(geoPoint)
                updateMarker(geoPoint)
            }
        }

        // Observe route points to draw the path
        val pathOverlay = Polyline().apply {
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = 10f
        }
        
        viewModel.routePoints.observe(viewLifecycleOwner) { points ->
            pathOverlay.setPoints(points)
            if (points.isNotEmpty()) {
                if (!map?.overlays?.contains(pathOverlay)!!) {
                    map?.overlays?.add(pathOverlay)
                }
            } else {
                map?.overlays?.remove(pathOverlay)
            }
            map?.invalidate()
        }

        checkPermissionsAndStartUpdates()
    }

    private var userMarker: Marker? = null

    private fun updateMarker(geoPoint: GeoPoint) {
        map?.let {
            if (userMarker == null) {
                userMarker = Marker(it).apply {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Your Location"
                }
                it.overlays.add(userMarker)
            }
            userMarker?.position = geoPoint
            it.invalidate()
        }
    }

    private fun checkPermissionsAndStartUpdates() {
        val fineLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            viewModel.startLocationUpdates()
        } else {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
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
