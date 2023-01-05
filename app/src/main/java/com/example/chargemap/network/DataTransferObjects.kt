package com.example.chargemap.network

import com.example.chargemap.domain.Station
import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NetworkStation(
    @Json(name = "ID") val id: String,
    @Json(name = "UsageCost") val usageCost: String?,
    @Json(name = "AddressInfo") val addressInfo: AddressInfo
)

@JsonClass(generateAdapter = true)
data class AddressInfo(
    @Json(name = "Title") val title: String,
    @Json(name = "AddressLine1") val address: String,
    @Json(name = "Latitude") val latitude: Double,
    @Json(name = "Longitude") val longitude: Double,
    @Json(name = "Distance") val distance: Double,
)

fun List<NetworkStation>.asDomainModel(): List<Station> {
    return map {
        Station(
            id = it.id,
            usageCost = it.usageCost ?: "",
            stationTitle = it.addressInfo.title,
            address = it.addressInfo.address.trim(),
            latLng = LatLng(it.addressInfo.latitude, it.addressInfo.longitude),
            distance = it.addressInfo.distance
        )
    }
}