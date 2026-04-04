package cat.ri.noko

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import cat.ri.noko.ui.NokoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : FragmentActivity() {

    companion object {
        const val EXTRA_NAVIGATE_TO_CHAT = "navigate_to_chat"
    }

    private val _pendingNavigateToChat = MutableStateFlow(false)
    val pendingNavigateToChat: StateFlow<Boolean> = _pendingNavigateToChat.asStateFlow()

    fun consumeNavigateToChat() { _pendingNavigateToChat.value = false }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent { NokoApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra(EXTRA_NAVIGATE_TO_CHAT, false)) {
            _pendingNavigateToChat.value = true
            intent.removeExtra(EXTRA_NAVIGATE_TO_CHAT)
        }
    }
}
