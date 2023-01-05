package com.example.chargemap.domain

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class Station(
    val id: String,
    val usageCost: String,
    val stationTitle: String,
    val address: String,
    val latLng: LatLng,
    val distance: Double,
) : ClusterItem {
    override fun getPosition(): LatLng = latLng
    override fun getTitle(): String = stationTitle
    override fun getSnippet(): String = address
}