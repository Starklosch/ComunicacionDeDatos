package com.starklosch.comunicacion

import android.net.InetAddresses
import android.net.nsd.NsdServiceInfo


data class ServiceInfo(
    val name: String,
    val type: String,
    val port: Int,
) {
    var addresses: List<String> = emptyList()
        private set

    var info: NsdServiceInfo? = null
        private set

    constructor(info: NsdServiceInfo) : this(info.serviceName, info.serviceType, info.port) {
        this.info = info
        this.addresses = info.getAddresses().map { it.hostName }
    }

    constructor(name: String, type: String, port: Int, addresses: List<String>) : this(name, type, port) {
        this.addresses = addresses
    }
}