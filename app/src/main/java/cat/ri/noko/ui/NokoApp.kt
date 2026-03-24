package cat.ri.noko.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.ui.screens.BiometricAuthScreen
import cat.ri.noko.ui.screens.ChatScreen
import cat.ri.noko.ui.screens.HomeScreen
import cat.ri.noko.ui.screens.OnboardingScreen
import cat.ri.noko.ui.screens.SettingsNavHost
import cat.ri.noko.ui.theme.NokoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NokoApp() {
    NokoTheme {

        @Suppress("USELESS_CAST")
        val onboardingComplete by SettingsManager.onboardingComplete
            .map { it as Boolean? }
            .collectAsState(initial = null)
        var onboardingDismissed by remember { mutableStateOf(false) }
        val biometricAuth by SettingsManager.biometricAuth.collectAsState(initial = false)
        var needsAuth by remember { mutableStateOf(true) }
        var secureReady by remember { mutableStateOf(false) }

        val lifecycleOwner = LocalLifecycleOwner.current
        val activity = LocalContext.current as? Activity

        DisposableEffect(lifecycleOwner) {
            val observer = object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    if (biometricAuth) needsAuth = true
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        if (onboardingComplete == null) return@NokoTheme

        if (biometricAuth && needsAuth && onboardingComplete == true) {
            BiometricAuthScreen(onSuccess = { needsAuth = false })
            return@NokoTheme
        }

        LaunchedEffect(needsAuth, biometricAuth, onboardingComplete) {
            if (!secureReady && (!biometricAuth || !needsAuth || onboardingComplete == false)) {
                withContext(Dispatchers.IO) { SettingsManager.initSecure() }
                secureReady = true
            }
        }

        if (!secureReady) return@NokoTheme

        if (!onboardingDismissed && onboardingComplete == false) {
            OnboardingScreen(onComplete = { onboardingDismissed = true })
            return@NokoTheme
        }

        val screenSecurity by SettingsManager.screenSecurity.collectAsState(initial = false)
        val clearClipboard by SettingsManager.clearClipboard.collectAsState(initial = false)
        val hideFromRecents by SettingsManager.hideFromRecents.collectAsState(initial = false)
        LaunchedEffect(screenSecurity) {
            if (screenSecurity) {
                activity?.window?.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE,
                )
            } else {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        LaunchedEffect(hideFromRecents) {
            val am = activity?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.appTasks?.firstOrNull()?.setExcludeFromRecents(hideFromRecents)
        }

        DisposableEffect(lifecycleOwner, clearClipboard) {
            val observer = object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    if (clearClipboard) {
                        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        var selectedTab by rememberSaveable { mutableIntStateOf(0) }
        var chatAction by remember { mutableStateOf<ChatAction?>(null) }
        var homeRefreshKey by remember { mutableIntStateOf(0) }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
        val animSpec = tween<Int>(durationMillis = 250)

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                ShortNavigationBar {
                    ShortNavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = {
                            if (selectedTab != 0) homeRefreshKey++
                            selectedTab = 0
                        },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                    )
                    ShortNavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Filled.ChatBubble, contentDescription = "Chat") },
                        label = { Text("Chat") },
                    )
                    ShortNavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                val homeOffset by animateIntAsState(
                    targetValue = (0 - selectedTab) * screenWidthPx,
                    animationSpec = animSpec,
                )
                val chatOffset by animateIntAsState(
                    targetValue = (1 - selectedTab) * screenWidthPx,
                    animationSpec = animSpec,
                )
                val settingsOffset by animateIntAsState(
                    targetValue = (2 - selectedTab) * screenWidthPx,
                    animationSpec = animSpec,
                )

                Box(Modifier.fillMaxSize().offset { IntOffset(homeOffset, 0) }) {
                    HomeScreen(
                        refreshKey = homeRefreshKey,
                        onNewChat = {
                            chatAction = ChatAction.NewChat(isSecret = false)
                            selectedTab = 1
                        },
                        onNewSecretChat = {
                            chatAction = ChatAction.NewChat(isSecret = true)
                            selectedTab = 1
                        },
                        onOpenRecentChat = { meta ->
                            chatAction = ChatAction.OpenRecent(meta)
                            selectedTab = 1
                        },
                    )
                }

                Box(Modifier.fillMaxSize().offset { IntOffset(chatOffset, 0) }) {
                    ChatScreen(
                        pendingAction = chatAction,
                        onActionConsumed = { chatAction = null },
                    )
                }

                Box(Modifier.fillMaxSize().offset { IntOffset(settingsOffset, 0) }) {
                    SettingsNavHost()
                }
            }
        }
    }
}
