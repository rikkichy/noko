package cat.ri.noko.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import cat.ri.noko.ui.theme.NokoFieldShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.SettingsManager
import androidx.compose.material3.Switch
import cat.ri.noko.model.ApiProvider
import cat.ri.noko.ui.util.rememberNokoHaptics
import kotlinx.coroutines.launch

@Composable
fun CustomProviderCard(
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()
    val customUrl by SettingsManager.customProviderUrl.collectAsState(initial = "")
    val customAuth by SettingsManager.customProviderAuth.collectAsState(initial = false)
    var customUrlInput by remember(customUrl) { mutableStateOf(customUrl) }
    var customAuthInput by remember(customAuth) { mutableStateOf(customAuth) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = NokoFieldShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Custom", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Enter your own OpenAI-compatible endpoint.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            AnimatedVisibility(visible = isSelected) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val customUrlValid = customUrlInput.isBlank() || SettingsManager.validateProviderUrl(customUrlInput)
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = {
                            customUrlInput = it
                            if (it.isBlank() || SettingsManager.validateProviderUrl(it)) {
                                scope.launch { SettingsManager.setCustomProviderUrl(it) }
                            }
                        },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.example.com/v1/") },
                        singleLine = true,
                        isError = !customUrlValid,
                        supportingText = if (!customUrlValid) {
                            { Text("Use https:// (or http:// for localhost)") }
                        } else null,
                        shape = NokoFieldShape,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Requires API key",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = customAuthInput,
                            onCheckedChange = { value ->
                                customAuthInput = value
                                if (value) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setCustomProviderAuth(value) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProviderCard(
    provider: ApiProvider,
    isSelected: Boolean,
    onSelect: () -> Unit,
    showUrlEditor: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val urlOverride by SettingsManager.getProviderUrlOverride(provider.id).collectAsState(initial = "")
    var urlInput by remember(urlOverride, provider.id) {
        mutableStateOf(urlOverride.ifBlank { provider.baseUrl })
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = NokoFieldShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (provider.isLocal) Icons.Rounded.Computer else Icons.Rounded.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(provider.name, style = MaterialTheme.typography.titleMedium)
                        if (provider.id == "openrouter") {
                            Text(
                                "Recommended",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (!isSelected) {
                        Text(
                            if (urlOverride.isNotBlank()) urlOverride else provider.baseUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (showUrlEditor && provider.urlEditable) {
                AnimatedVisibility(visible = isSelected) {
                    val urlValid = urlInput.isBlank() || SettingsManager.validateProviderUrl(urlInput)
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            if (it.isBlank() || SettingsManager.validateProviderUrl(it)) {
                                val override = if (it == provider.baseUrl) "" else it
                                scope.launch { SettingsManager.setProviderUrlOverride(provider.id, override) }
                            }
                        },
                        label = { Text("Base URL") },
                        placeholder = { Text(provider.baseUrl) },
                        singleLine = true,
                        isError = !urlValid,
                        supportingText = if (!urlValid) {
                            { Text("Use https:// (or http:// for localhost)") }
                        } else null,
                        shape = NokoFieldShape,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
