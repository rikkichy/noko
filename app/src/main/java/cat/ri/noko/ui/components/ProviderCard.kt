package cat.ri.noko.ui.components

import android.net.Uri
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
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.WifiFind
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.ri.noko.R
import cat.ri.noko.core.DiscoveredInstance
import cat.ri.noko.core.NetworkDiscovery
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
                Icon(
                    Icons.Rounded.Extension,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.provider_card_custom), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.provider_card_custom_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = stringResource(R.string.common_selected),
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
                        label = { Text(stringResource(R.string.provider_card_base_url)) },
                        placeholder = { Text(stringResource(R.string.provider_card_base_url_placeholder)) },
                        singleLine = true,
                        isError = !customUrlValid,
                        supportingText = if (!customUrlValid) {
                            { Text(stringResource(R.string.provider_card_url_validation)) }
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
                            stringResource(R.string.provider_card_requires_api_key),
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
    val haptics = rememberNokoHaptics()
    val urlOverride by SettingsManager.getProviderUrlOverride(provider.id).collectAsState(initial = "")
    var urlInput by remember(urlOverride, provider.id) {
        mutableStateOf(urlOverride.ifBlank { provider.baseUrl })
    }

    // Scan state for local providers
    var isScanning by remember { mutableStateOf(false) }
    var scanResults by remember { mutableStateOf<List<DiscoveredInstance>>(emptyList()) }
    var scanError by remember { mutableStateOf<String?>(null) }

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
                val iconRes = when (provider.id) {
                    "openrouter" -> R.drawable.ic_openrouter
                    "openai" -> R.drawable.ic_openai
                    "ollama" -> R.drawable.ic_ollama
                    "lmstudio" -> R.drawable.ic_lmstudio
                    else -> null
                }
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(
                        Icons.Rounded.Computer,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(provider.name, style = MaterialTheme.typography.titleMedium)
                        if (provider.id == "openrouter") {
                            Text(
                                stringResource(R.string.provider_card_recommended),
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
                        contentDescription = stringResource(R.string.common_selected),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (showUrlEditor && provider.urlEditable) {
                AnimatedVisibility(visible = isSelected) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            label = { Text(stringResource(R.string.provider_card_base_url)) },
                            placeholder = { Text(provider.baseUrl) },
                            singleLine = true,
                            isError = !urlValid,
                            supportingText = if (!urlValid) {
                                { Text(stringResource(R.string.provider_card_url_validation)) }
                            } else null,
                            shape = NokoFieldShape,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        if (provider.isLocal) {
                            val noResultsMsg = stringResource(R.string.provider_card_scan_no_results, provider.name)
                            val scanFailedTemplate = stringResource(R.string.provider_card_scan_failed)
                            FilledTonalButton(
                                onClick = {
                                    haptics.tap()
                                    isScanning = true
                                    scanError = null
                                    scanResults = emptyList()
                                    val port = Uri.parse(provider.baseUrl).port
                                    scope.launch {
                                        try {
                                            val results = NetworkDiscovery.scanSubnet(
                                                port = port,
                                                providerName = provider.name,
                                            )
                                            scanResults = results
                                            if (results.isEmpty()) {
                                                scanError = noResultsMsg
                                            }
                                        } catch (e: Exception) {
                                            scanError = scanFailedTemplate.format(e.message ?: "")
                                        } finally {
                                            isScanning = false
                                        }
                                    }
                                },
                                enabled = !isScanning,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Rounded.WifiFind,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(if (isScanning) stringResource(R.string.provider_card_scanning) else stringResource(R.string.provider_card_scan))
                            }

                            AnimatedVisibility(visible = isScanning) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            AnimatedVisibility(visible = scanError != null) {
                                Text(
                                    scanError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            AnimatedVisibility(visible = scanResults.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    scanResults.forEach { result ->
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            ),
                                            shape = NokoFieldShape,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        haptics.confirm()
                                                        urlInput = result.baseUrl
                                                        val override = if (result.baseUrl == provider.baseUrl) "" else result.baseUrl
                                                        scope.launch { SettingsManager.setProviderUrlOverride(provider.id, override) }
                                                        scanResults = emptyList()
                                                    }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Column {
                                                    Text(
                                                        result.ip,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                    )
                                                    Text(
                                                        pluralStringResource(R.plurals.provider_card_models_count, result.models.size, result.models.size),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                Icon(
                                                    Icons.Rounded.Computer,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
