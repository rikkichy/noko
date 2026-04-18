package cat.ri.noko

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.ConnectivityManager
import android.net.Network
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.core.api.ApiClient

class NokoApplication : Application() {

    companion object {
        const val CHANNEL_STREAM_COMPLETE = "stream_complete"
    }

    override fun onCreate() {
        super.onCreate()
        SettingsManager.initBasic(this)
        val channel = NotificationChannel(
            CHANNEL_STREAM_COMPLETE,
            "Chat replies",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Notifies when AI finishes replying" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                ApiClient.evictConnectionPool()
            }

            override fun onLost(network: Network) {
                ApiClient.evictConnectionPool()
            }
        })
    }
}
