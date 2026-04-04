package cat.ri.noko

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import cat.ri.noko.core.SettingsManager

class NokoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsManager.initBasic(this)
        val channel = NotificationChannel(
            "stream_complete",
            "Chat replies",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Notifies when AI finishes replying" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
