package cat.ri.noko.core

import cat.ri.noko.model.api.ModelsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit

data class DiscoveredInstance(
    val ip: String,
    val port: Int,
    val providerName: String,
    val models: List<String>,
) {
    val baseUrl: String get() = "http://$ip:$port/v1/"
}

object NetworkDiscovery {

    private const val CONNECT_TIMEOUT_MS = 150
    private const val VERIFY_TIMEOUT_MS = 2000
    private const val MAX_CONCURRENT_PROBES = 50

    private val json = Json { ignoreUnknownKeys = true }

    private val verifyClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(VERIFY_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(VERIFY_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .build()
    }

    fun getLocalIpv4(): String? {
        val interfaces = try {
            NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
        } catch (_: Exception) {
            return null
        }
        // Prefer wlan/eth interfaces
        val sorted = interfaces.sortedByDescending { iface ->
            val name = iface.name.lowercase()
            when {
                name.startsWith("wlan") -> 2
                name.startsWith("eth") -> 1
                else -> 0
            }
        }
        for (iface in sorted) {
            if (iface.isLoopback || !iface.isUp) continue
            for (addr in iface.inetAddresses) {
                if (addr is Inet4Address && addr.isSiteLocalAddress) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }

    suspend fun scanSubnet(
        port: Int,
        providerName: String,
    ): List<DiscoveredInstance> {
        val localIp = getLocalIpv4() ?: return emptyList()
        val prefix = localIp.substringBeforeLast('.') + "."
        val semaphore = Semaphore(MAX_CONCURRENT_PROBES)

        return coroutineScope {
            (1..254).map { i ->
                val ip = "$prefix$i"
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        probeAndVerify(ip, port, providerName)
                    }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private suspend fun probeAndVerify(
        ip: String,
        port: Int,
        providerName: String,
    ): DiscoveredInstance? {
        if (!tcpProbe(ip, port)) return null
        val models = verifyService(ip, port) ?: return null
        return DiscoveredInstance(ip, port, providerName, models)
    }

    private fun tcpProbe(ip: String, port: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)
            true
        }
    } catch (_: Exception) {
        false
    }

    private fun verifyService(ip: String, port: Int): List<String>? = try {
        val request = Request.Builder()
            .url("http://$ip:$port/v1/models")
            .build()
        verifyClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            val parsed = json.decodeFromString<ModelsResponse>(body)
            parsed.data.map { it.id }
        }
    } catch (_: Exception) {
        null
    }
}
