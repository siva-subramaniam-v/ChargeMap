package com.example.chargemap.map.station

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.chargemap.R
import com.example.chargemap.domain.Station
import com.example.chargemap.util.BitmapHelper
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

class StationRenderer(
    private val context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<Station>
): DefaultClusterRenderer<Station>(context, map, clusterManager) {

    private val evIcon: BitmapDescriptor by lazy {
        val color = ContextCompat.getColor(
            context, R.color.ev_station_green
        )
        BitmapHelper.vectorToBitmap(
            context, R.drawable.ev_station_24, color
        )
    }

    override fun onBeforeClusterItemRendered(item: Station, markerOptions: MarkerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions)
        markerOptions.title(item.title)
            .position(item.latLng)
            .icon(evIcon)
    }

    override fun onClusterItemRendered(clusterItem: Station, marker: Marker) {
        super.onClusterItemRendered(clusterItem, marker)
        marker.tag = clusterItem
    }
}













