package com.example.chargemap.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.chargemap.BuildConfig
import com.example.chargemap.R
import com.example.chargemap.adapter.StationAdapter
import com.example.chargemap.databinding.FragmentHomeBinding
import com.example.chargemap.domain.Station
import com.example.chargemap.map.adapter.MarkerInfoWindowAdapter
import com.example.chargemap.map.place.Place
import com.example.chargemap.map.place.PlaceRenderer
import com.example.chargemap.map.place.PlacesReader
import com.example.chargemap.map.station.StationRenderer
import com.example.chargemap.ui.MainActivity
import com.example.chargemap.ui.viewmodels.HomeViewModel
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.clustering.ClusterManager


class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationManger: LocationManager
    private lateinit var locationCallback: LocationCallback
    private lateinit var map: GoogleMap
    private lateinit var clusterManager: ClusterManager<Station>
    private lateinit var stationRenderer: StationRenderer
    private var marker: Marker? = null

    private val places: List<Place> by lazy {
        PlacesReader(requireContext()).read()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i("Permission: ", "Granted")
        } else {
            Snackbar.make(
                binding.root,
                "Location permission is required to fetch nearby charging stations",
                Snackbar.LENGTH_INDEFINITE
            ).setAction("OK") {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                intent.data = uri
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }.show()
            Log.i("Permission: ", "Denied")
        }
    }

    private fun onClickRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getLocation()
            }

            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        val isGpsEnabled = locationManger.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled =
            ((activity as MainActivity).getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetwork != null

        when {
            !isNetworkEnabled -> Toast.makeText(
                requireContext(), "Please enable internet", Toast.LENGTH_LONG
            ).show()

            !isGpsEnabled -> Toast.makeText(
                requireContext(), "Please enable GPS", Toast.LENGTH_LONG
            ).show()

            else -> {
                val request = LocationRequest.Builder(10000L).build()

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        super.onLocationResult(result)
                        result.locations.lastOrNull()?.let { location ->
                            requestStations(location)
                            addLocationMarker(LatLng(location.latitude, location.longitude))
                            stopFetchingLocation()
                        }
                    }
                }

                fusedLocationProviderClient.requestLocationUpdates(
                    request, locationCallback, Looper.getMainLooper()
                )
            }
        }
    }

    private fun stopFetchingLocation() {
        if (::locationCallback.isInitialized) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Adds markers to the map with clustering support.
     */
    private fun addClusteredMarkers(googleMap: GoogleMap) {
        // Create the ClusterManager class and set the custom renderer.
        val clusterManager = ClusterManager<Place>(requireContext(), googleMap)
        clusterManager.renderer = PlaceRenderer(
            requireContext(), googleMap, clusterManager
        )

        // Set custom info window adapter
        clusterManager.markerCollection.setInfoWindowAdapter(
            MarkerInfoWindowAdapter(
                requireContext()
            )
        )

        // Add the places to the ClusterManager.
        clusterManager.addItems(places)
        clusterManager.cluster()

        // Show polygon
        clusterManager.setOnClusterItemClickListener { item ->
            addCircle(googleMap, item)
            return@setOnClusterItemClickListener false
        }

        // Set ClusterManager as the OnCameraIdleListener so that it
        // can re-cluster when zooming in and out.
        googleMap.setOnCameraIdleListener {
            clusterManager.onCameraIdle()
        }
    }

    private var circle: Circle? = null

    /**
     * Adds a [Circle] around the provided [item]
     */
    private fun addCircle(googleMap: GoogleMap, item: Place) {
        circle?.remove()
        circle = googleMap.addCircle(
            CircleOptions().center(item.latLng).radius(1000.0).fillColor(
                ContextCompat.getColor(
                    requireContext(), R.color.colorPrimaryTranslucent
                )
            ).strokeColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false)

        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        binding.homeViewModel = homeViewModel

        val adapter = StationAdapter()
        binding.stationList.adapter = adapter

        homeViewModel.stations.observe(viewLifecycleOwner) { stations ->

            if (::map.isInitialized) {
                addClusteredStationMarkers(stations)
                //addMarkerClickListener()
            }

            adapter.submitList(stations)

            // update camera view to cover all charging stations
            /*googleMap.setOnMapLoadedCallback {
                val bounds = LatLngBounds.builder()
                places.forEach { bounds.include(it.latLng) }
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 20))
            }*/
        }

        // add GoogleMap object reference
        val mapFragment = childFragmentManager.findFragmentById(
            R.id.map_fragment
        ) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            map = googleMap
            //addClusteredMarkers(googleMap)
        }

        binding.fabMyLocation.setOnClickListener {
            onClickRequestPermission()
//            BottomSheetBehavior.from(binding.bottomSheet.sheet).apply {
//                state = BottomSheetBehavior.STATE_COLLAPSED
//            }
        }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity() as MainActivity)
        locationManger =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        //binding.bottomSheet.sheet.is
//        binding.bottomSheet.sheet.visibility = View.INVISIBLE
//
//        val sheet = binding.bottomSheet.sheet

//        BottomSheetBehavior.from(sheet).apply {
//            peekHeight = 180
//            state = BottomSheetBehavior.STATE_COLLAPSED
//        }

//        map.setOnMapClickListener {
//            binding.bottomSheet.sheet.visibility = View.INVISIBLE
//        }

        return binding.root
    }

    private fun requestStations(location: Location) {
        if (::homeViewModel.isInitialized) {
            homeViewModel.getStations(
                location.latitude.toString(), location.longitude.toString()
            )
        }
    }

    private fun addLocationMarker(location: LatLng) {
        marker?.remove()

        if (::map.isInitialized) {
            val markerOptions = MarkerOptions()
            markerOptions.position(location).icon(
                BitmapDescriptorFactory.defaultMarker()
            )
            marker = map.addMarker(markerOptions)
        }

        // move camera to user's current location
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                location, 11f
            )
        ) //DEFAULT_ZOOM.toFloat()
    }

    private fun addClusteredStationMarkers(stations: List<Station>) {

        if (!::clusterManager.isInitialized) {
            clusterManager = ClusterManager<Station>(requireContext(), map)
        } else {
            clusterManager.clearItems()
            clusterManager.cluster()
        }

        if (!::stationRenderer.isInitialized) {
            stationRenderer = StationRenderer(
                requireContext(),
                map,
                clusterManager
            )

            stationRenderer.minClusterSize = 2
            clusterManager.renderer = stationRenderer
        }

        //clusterManager.markerCollection.setInfoWindowAdapter(InfoWindowAdapter(requireContext()))
        clusterManager.addItems(stations)
        clusterManager.cluster()

        // Set ClusterManager as the OnCameraIdleListener so that it
        // can re-cluster when zooming in and out.
        map.setOnCameraIdleListener {
            clusterManager.onCameraIdle()
        }
    }

//    private fun addMarkerClickListener() {
//        map.setOnMarkerClickListener(clusterManager)
//
//        clusterManager.setOnClusterItemClickListener {
//            binding.bottomSheet. = View.VISIBLE
//            BottomSheetBehavior.from(binding.bottomSheet.sheet).apply { state = BottomSheetBehavior.STATE_EXPANDED}
//            it?.let {
//                binding.bottomSheet.station = it
//            }
//            true
//        }
//    }
}