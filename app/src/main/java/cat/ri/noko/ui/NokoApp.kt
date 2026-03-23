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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.ui.screens.ChatScreen
import cat.ri.noko.ui.screens.HomeScreen
import cat.ri.noko.ui.screens.OnboardingScreen
import cat.ri.noko.ui.screens.SettingsNavHost
import cat.ri.noko.ui.theme.NokoTheme
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NokoApp() {
    NokoTheme {

        @Suppress("USELESS_CAST")
        val onboardingComplete by SettingsManager.onboardingComplete
            .map { it as Boolean? }
            .collectAsState(initial = null)
        var onboardingDismissed by remember { mutableStateOf(false) }

        if (onboardingComplete == null) return@NokoTheme

        if (!onboardingDismissed && onboardingComplete == false) {
            OnboardingScreen(onComplete = { onboardingDismissed = true })
            return@NokoTheme
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
