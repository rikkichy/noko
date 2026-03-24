package cat.ri.noko

import android.app.Application
import cat.ri.noko.core.SettingsManager

class NokoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SettingsManager.initBasic(this)
    }
}
