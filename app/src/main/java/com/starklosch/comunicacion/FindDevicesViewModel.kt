package com.starklosch.comunicacion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FindDevicesViewModel(application: Application) : AndroidViewModel(application) {
    private val nsd = NetworkServiceDiscovery(application)
    private val _services = MutableStateFlow(listOf<ServiceInfo>())
    val services = _services.asStateFlow()

    init {
        nsd.discoveredServicesUpdated = {
            _services.value = it.toList()
        }
        startDiscovery()
    }

    fun startDiscovery(){
        nsd.discover("esp32", "tcp")
    }

    fun stopDiscovery(){
        nsd.stop()
    }
}