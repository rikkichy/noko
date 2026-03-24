package cat.ri.noko.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.SendTimeExtension
import androidx.compose.material.icons.rounded.DisabledVisible
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.ui.util.rememberNokoHaptics
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NokoPolkitScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()
    val trimEmojis by SettingsManager.nokoPolkitTrimEmojis.collectAsState(initial = true)
    val structureActions by SettingsManager.nokoPolkitStructureActions.collectAsState(initial = true)
    val screenSecurity by SettingsManager.screenSecurity.collectAsState(initial = false)
    val incognitoKeyboard by SettingsManager.incognitoKeyboard.collectAsState(initial = false)
    val clearClipboard by SettingsManager.clearClipboard.collectAsState(initial = false)
    val hideFromRecents by SettingsManager.hideFromRecents.collectAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NokoPolkit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SendTimeExtension, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Chat Policies", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Trim emojis")
                            Text(
                                "Remove emojis from AI responses.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = trimEmojis,
                            onCheckedChange = { value ->
                                if (value) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setNokoPolkitTrimEmojis(value) }
                            },
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Structure actions")
                            Text(
                                "Add newlines before and after *action* blocks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = structureActions,
                            onCheckedChange = { value ->
                                if (value) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setNokoPolkitStructureActions(value) }
                            },
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DisabledVisible, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("App Policies", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Screen security")
                            Text(
                                "Prevent screenshots and screen recording.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = screenSecurity,
                            onCheckedChange = { value ->
                                if (value) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setScreenSecurity(value) }
                            },
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Incognito keyboard")
                            Text(
                                "Ask keyboards to disable learning and suggestions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = incognitoKeyboard,
                            onCheckedChange = { value ->
                                if (value) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setIncognitoKeyboard(value) }
                            },
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Clear clipboard on exit")
                            Text(
                                "Wipe clipboard when the app goes to background.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = clearClipboard,
                            onCheckedChange = { value ->
                                if (value) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setClearClipboard(value) }
                            },
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hide from recents")
                            Text(
                                "Exclude the app from the recent apps list.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = hideFromRecents,
                            onCheckedChange = { value ->
                                if (value) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setHideFromRecents(value) }
                            },
                        )
                    }
                }
            }
        }
    }
}
