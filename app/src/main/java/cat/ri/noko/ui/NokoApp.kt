package cat.ri.noko.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
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
import cat.ri.noko.core.SettingsManager
import kotlinx.coroutines.flow.map
import cat.ri.noko.ui.screens.ChatScreen
import cat.ri.noko.ui.screens.HomeScreen
import cat.ri.noko.ui.screens.OnboardingScreen
import cat.ri.noko.ui.screens.SettingsNavHost
import cat.ri.noko.ui.theme.NokoTheme

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

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                ShortNavigationBar {
                    ShortNavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
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


                Box(
                    Modifier.fillMaxSize().then(
                        if (selectedTab != 1) Modifier.offset(x = 10000.dp) else Modifier
                    )
                ) {
                    ChatScreen(
                        pendingAction = chatAction,
                        onActionConsumed = { chatAction = null },
                    )
                }

                when (selectedTab) {
                    0 -> HomeScreen(
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
                    2 -> SettingsNavHost()
                }
            }
        }
    }
}
