package cat.ri.noko

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import cat.ri.noko.core.SettingsManager

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
    }
}
