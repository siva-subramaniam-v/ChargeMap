package com.example.chargemap.repository

import androidx.lifecycle.MutableLiveData
import com.example.chargemap.BuildConfig
import com.example.chargemap.domain.Station
import com.example.chargemap.network.Network
import com.example.chargemap.network.asDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Repository {
    val chargingStation = MutableLiveData<List<Station>>()

    suspend fun fetchStations(lat: String, long: String) {
        withContext(Dispatchers.IO) {
            val stations = Network.retrofitService.getStations(lat, long, key = BuildConfig.OPEN_CHARGE_MAP_API_KEY)
            chargingStation.postValue(stations.asDomainModel())
        }
    }
}