package com.example.chargemap.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chargemap.repository.Repository
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel() {
    private val repository = Repository()

    init {
        viewModelScope.launch {
            //repository.fetchStations("12.8316089", "80.0518032")
        }
    }

    fun getStations(latitude: String, longitude: String) {
        viewModelScope.launch {
            repository.fetchStations(latitude, longitude)
        }
    }

    val stations = repository.chargingStation
}