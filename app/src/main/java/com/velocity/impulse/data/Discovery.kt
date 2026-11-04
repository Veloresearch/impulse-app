package com.velocity.impulse.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.util.concurrent.TimeUnit

/** A computer running Impulse that this phone can see: its address, a name, and who signs in there. */
data class Found(val base: String, val name: String, val people: List<String>)

/**
 * Finds Impulse on the network two ways at once: computers that announce themselves over mDNS
 * (`_impulse._tcp`), and every address of the phone's own Wi-Fi subnet asked on port 11500.
 */
object Discovery {
    private const val PORT = 11500
    private val probe = OkHttpClient.Builder()
        .connectTimeout(700, TimeUnit.MILLISECONDS)
        .readTimeout(1200, TimeUnit.MILLISECONDS)
        .build()

    fun find(ctx: Context): Flow<Found> = channelFlow {
        launch { announced(ctx).collect { send(it) } }
        launch { sweep(ctx) { send(it) } }
    }

    /** Asks one address whether Impulse answers there, and who its profiles are. */
    fun ask(base: String, name: String? = null): Found? = runCatching {
        probe.newCall(Request.Builder().url("$base/api/v2/profiles").build()).execute().use { r ->
            if (!r.isSuccessful) return null
            val o = JSONObject(r.body?.string().orEmpty())
            val people = o.optJSONArray("profiles").objects().map { it.optString("name") }.filter { it.isNotBlank() }
            if (!o.has("profiles")) return null
            Found(base, name ?: base.substringAfter("://").substringBefore(':'), people)
        }
    }.getOrNull()

    private suspend fun sweep(ctx: Context, emit: suspend (Found) -> Unit) = coroutineScope {
        val own = wifiAddress(ctx) ?: return@coroutineScope
        val prefix = own.substringBeforeLast('.')
        val gate = Semaphore(48)
        for (i in 1..254) {
            val ip = "$prefix.$i"
            if (ip == own) continue
            launch(Dispatchers.IO) { gate.withPermit { ask("http://$ip:$PORT")?.let { emit(it) } } }
        }
    }

    private fun wifiAddress(ctx: Context): String? {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return null
        val net = cm.allNetworks.firstOrNull { cm.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true } ?: return null
        return cm.getLinkProperties(net)?.linkAddresses?.map { it.address }?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }?.hostAddress
    }

    private fun announced(ctx: Context): Flow<Found> = callbackFlow {
        val nsd = ctx.getSystemService(NsdManager::class.java)
        if (nsd == null) {
            close()
            return@callbackFlow
        }
        val listener = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(info: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                nsd.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onServiceResolved(s: NsdServiceInfo) {
                        val host = s.host?.hostAddress ?: return
                        val base = if (host.contains(':')) "http://[$host]:${s.port}" else "http://$host:${s.port}"
                        launch(Dispatchers.IO) { ask(base, s.serviceName)?.let { trySendBlocking(it) } }
                    }

                    override fun onResolveFailed(s: NsdServiceInfo, code: Int) {}
                })
            }

            override fun onDiscoveryStarted(type: String) {}
            override fun onDiscoveryStopped(type: String) {}
            override fun onServiceLost(info: NsdServiceInfo) {}
            override fun onStartDiscoveryFailed(type: String, code: Int) = Unit.also { close() }
            override fun onStopDiscoveryFailed(type: String, code: Int) {}
        }
        nsd.discoverServices("_impulse._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose { runCatching { nsd.stopServiceDiscovery(listener) } }
    }
}
