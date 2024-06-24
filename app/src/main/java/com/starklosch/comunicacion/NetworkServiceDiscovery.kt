package com.starklosch.comunicacion

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import java.io.Closeable

class NetworkServiceDiscovery(context: Context) : Closeable {
    private val _services = mutableSetOf<ServiceInfo>()
    val services
        get() = _services.toList()

    private val nsdManager = context.getSystemService<NsdManager>()
        ?: throw Exception("No se pudo obtener el servicio NsdManager")

    var discoveredServicesUpdated: (Collection<ServiceInfo>) -> Unit = {}

    fun discover(service: String, protocol: String) {
        nsdManager.discoverServices(
            "_$service._$protocol",
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun stop(){
        nsdManager.stopServiceDiscovery(discoveryListener)
    }

    override fun close() {
        stop()
    }

    private val resolver: Resolver =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceUpdatedInfoCallback(nsdManager)
        } else {
            ServiceUpdatedResolveListener(nsdManager)
        }.apply {
            resolvedCallback = { service ->
                // Replace with new info
                _services.remove(ServiceInfo(service))
                _services.add(ServiceInfo(service))

                discoveredServicesUpdated(services)
            }
        }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        val TAG = "NSD"

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success: $service")
            // Don't add services just discovered. Resolve first, then add
            resolver.resolveService(service)
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            Log.e(TAG, "Service lost: $service")
            _services.remove(ServiceInfo(service))
            discoveredServicesUpdated(services)
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }
}

// Simple discovery to call as composable
//@Composable
//fun NetworkServiceDiscovery(service: String, protocol: String, onFound: (NsdServiceInfo) -> Unit) {
//    val context = LocalContext.current
//    val nsdManager = remember {
//        context.getSystemService<NsdManager>()
//    }
//
//    val discoveryListener = remember {
//        object : NsdManager.DiscoveryListener {
//            val TAG = "NSD"
//
//            // Called as soon as service discovery begins.
//            override fun onDiscoveryStarted(regType: String) {
//                Log.d(TAG, "Service discovery started")
//            }
//
//            override fun onServiceFound(service: NsdServiceInfo) {
//                // A service was found! Do something with it.
//                Log.d(TAG, "Service discovery success: $service")
//                onFound(service)
//            }
//
//            override fun onServiceLost(service: NsdServiceInfo) {
//                // When the network service is no longer available.
//                // Internal bookkeeping code goes here.
//                Log.e(TAG, "Service lost: $service")
//            }
//
//            override fun onDiscoveryStopped(serviceType: String) {
//                Log.i(TAG, "Discovery stopped: $serviceType")
//            }
//
//            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
//                Log.e(TAG, "Discovery failed: Error code: $errorCode")
//                nsdManager?.stopServiceDiscovery(this)
//            }
//
//            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
//                Log.e(TAG, "Discovery failed: Error code: $errorCode")
//                nsdManager?.stopServiceDiscovery(this)
//            }
//        }
//    }
//
//    DisposableEffect(key1 = true) {
//        nsdManager?.discoverServices(
//            "_$service._$protocol",
//            NsdManager.PROTOCOL_DNS_SD,
//            discoveryListener
//        )
//        onDispose {
//            nsdManager?.stopServiceDiscovery(discoveryListener)
//        }
//    }
//}

abstract class Resolver(val manager: NsdManager) {
    var resolvedCallback: (NsdServiceInfo) -> Unit = {}
    abstract fun resolveService(service: NsdServiceInfo)
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class ServiceUpdatedInfoCallback(manager: NsdManager) : Resolver(manager),
    NsdManager.ServiceInfoCallback,
    Closeable {
    private val TAG = "NSD"
    private var registered = false

    override fun onServiceInfoCallbackRegistrationFailed(i: Int) {
        Log.d(TAG, "Service callback registration failed: $i")
    }

    override fun onServiceUpdated(service: NsdServiceInfo) {
        Log.d(TAG, "Service updated: $service")
        resolvedCallback(service)
    }

    override fun onServiceLost() {
        Log.d(TAG, "Service lost")
    }

    override fun onServiceInfoCallbackUnregistered() {
        Log.d(TAG, "Service callback registration unregistered")
    }

    override fun resolveService(service: NsdServiceInfo) {
        if (registered) {
//            Log.d(TAG, "Unregistering")
            manager.unregisterServiceInfoCallback(this)
        }

        manager.registerServiceInfoCallback(
            service,
            { Log.d(TAG, "ServiceInfoCallback Executor: $service"); it.run() },
            this
        )
        registered = true
    }

    override fun close() {
        if (registered)
            manager.unregisterServiceInfoCallback(this)
    }
}

class ServiceUpdatedResolveListener(manager: NsdManager) : Resolver(manager),
    NsdManager.ResolveListener {
    private val TAG = "NSD"

    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        // Called when the resolve fails. Use the error code to debug.
        Log.e(TAG, "Resolve failed: $errorCode")
    }

    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
        Log.e(TAG, "Resolve Succeeded. $serviceInfo")
        resolvedCallback(serviceInfo)
    }

    override fun resolveService(service: NsdServiceInfo) {
        manager.resolveService(service, this)
    }
}