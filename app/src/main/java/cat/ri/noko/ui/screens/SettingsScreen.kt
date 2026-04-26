package cat.ri.noko.ui.screens

import androidx.compose.animation.core.Animatable
import cat.ri.noko.ui.util.shake
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import cat.ri.noko.ui.theme.NokoFieldShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.navigation.NavController
import cat.ri.noko.BuildConfig
import cat.ri.noko.R
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.core.api.ApiClient
import cat.ri.noko.model.PersonaType
import cat.ri.noko.model.getProviderById
import cat.ri.noko.ui.theme.nokoTopAppBarColors
import cat.ri.noko.ui.util.rememberNokoHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()
    val amoled by SettingsManager.amoledMode.collectAsState(initial = false)
    val showAvatars by SettingsManager.showAvatars.collectAsState(initial = true)
    val showNames by SettingsManager.showNames.collectAsState(initial = true)
    val reduceMotion by SettingsManager.reduceMotion.collectAsState(initial = false)
    val nokoGuard by SettingsManager.nokoGuard.collectAsState(initial = true)
    val apiKey by SettingsManager.apiKey.collectAsState(initial = if (SettingsManager.hasApiKey()) "placeholder" else "")
    val hasKey = apiKey.isNotBlank()
    val modelName by SettingsManager.selectedModelName.collectAsState(initial = "")
    val providerId by SettingsManager.selectedProviderId.collectAsState(initial = SettingsManager.getSelectedProviderId())
    val customUrl by SettingsManager.customProviderUrl.collectAsState(initial = "")
    val customAuth by SettingsManager.customProviderAuth.collectAsState(initial = false)
    val provider = remember(providerId) { getProviderById(providerId) }
    val providerName = provider?.name ?: "Custom"
    val providerRequiresAuth = provider?.requiresAuth ?: customAuth
    val urlOverride by SettingsManager.getProviderUrlOverride(providerId).collectAsState(initial = "")
    val providerBaseUrl = if (provider != null) urlOverride.ifBlank { provider.baseUrl } else customUrl
    var apiKeyInput by remember(apiKey) { mutableStateOf("") }
    var isChangingKey by remember { mutableStateOf(false) }
    var isTestingKey by remember { mutableStateOf(false) }
    var keyError by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = nokoTopAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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
                        Icon(Icons.Rounded.People, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.settings_section_personas), style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.tap()
                                navController.navigate("personas/${PersonaType.PERSONA.name}")
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_personas))
                            Text(
                                stringResource(R.string.settings_personas_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.tap()
                                navController.navigate("personas/${PersonaType.CHARACTER.name}")
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_characters))
                            Text(
                                stringResource(R.string.settings_characters_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        Icon(Icons.Rounded.SmartToy, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.settings_section_configuration), style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.tap()
                                navController.navigate("providers")
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_provider))
                            Text(
                                providerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (providerRequiresAuth) {
                        HorizontalDivider()

                        if (hasKey && !isChangingKey) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(stringResource(R.string.settings_api_key), modifier = Modifier.weight(1f))
                                Spacer(Modifier.size(12.dp))
                                TextButton(
                                    onClick = {
                                        haptics.tap()
                                        isChangingKey = true
                                        apiKeyInput = ""
                                        keyError = null
                                    },
                                ) { Text(stringResource(R.string.settings_use_another_key)) }
                            }
                        } else {
                            val placeholder = when (providerId) {
                                "openrouter" -> "sk-or-v1-..."
                                "openai" -> "sk-..."
                                else -> "API key"
                            }
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = {
                                    apiKeyInput = it
                                    if (keyError != null) keyError = null
                                },
                                label = { Text(stringResource(R.string.settings_api_key)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset { IntOffset(shakeOffset.value.toInt(), 0) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                placeholder = { Text(placeholder) },
                                isError = keyError != null,
                                supportingText = if (keyError != null) {
                                    { Text(keyError!!) }
                                } else null,
                                shape = NokoFieldShape,
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    enabled = !isTestingKey && apiKeyInput.isNotBlank(),
                                    onClick = {
                                        haptics.tap()
                                        isTestingKey = true
                                        keyError = null
                                        scope.launch {
                                            runCatching {
                                                ApiClient.configure(apiKeyInput, providerBaseUrl, providerId)
                                                withContext(Dispatchers.IO) {
                                                    ApiClient.validateConnection()
                                                }
                                            }.onSuccess {
                                                SettingsManager.setApiKey(apiKeyInput)
                                                isChangingKey = false
                                            }.onFailure { e ->
                                                keyError = cat.ri.noko.core.api.humanizeException(e)
                                                haptics.reject()
                                                shakeOffset.shake()

                                                if (apiKey.isNotBlank()) {
                                                    ApiClient.configure(apiKey, providerBaseUrl, providerId)
                                                }
                                            }
                                            isTestingKey = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (isTestingKey) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(if (isTestingKey) stringResource(R.string.settings_testing) else stringResource(R.string.common_save))
                                }
                                if (isChangingKey && hasKey) {
                                    TextButton(
                                        onClick = {
                                            haptics.tap()
                                            isChangingKey = false
                                            keyError = null

                                            if (apiKey.isNotBlank()) {
                                                ApiClient.configure(apiKey, providerBaseUrl, providerId)
                                            }
                                        },
                                    ) { Text(stringResource(R.string.common_cancel)) }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.tap()
                                navController.navigate("models")
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_model))
                            Text(
                                if (modelName.isNotBlank()) modelName else stringResource(R.string.settings_tap_to_select),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.tap()
                                navController.navigate("prompt_builder")
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_prompt_editor))
                            Text(
                                stringResource(R.string.settings_prompt_editor_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        Icon(Icons.Rounded.Brush, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.settings_section_appearance), style = MaterialTheme.typography.titleMedium)
                    }

                    val isDarkTheme = isSystemInDarkTheme()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_amoled),
                                color = if (isDarkTheme) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                            Text(
                                if (isDarkTheme) stringResource(R.string.settings_amoled_desc)
                                else stringResource(R.string.settings_amoled_desc_disabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = amoled,
                            onCheckedChange = { enabled ->
                                if (enabled) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setAmoledMode(enabled) }
                            },
                            enabled = isDarkTheme,
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_show_avatars))
                            Text(
                                stringResource(R.string.settings_show_avatars_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = showAvatars,
                            onCheckedChange = { enabled ->
                                if (enabled) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setShowAvatars(enabled) }
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
                            Text(stringResource(R.string.settings_show_names))
                            Text(
                                stringResource(R.string.settings_show_names_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = showNames,
                            onCheckedChange = { enabled ->
                                if (enabled) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setShowNames(enabled) }
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
                            Text(stringResource(R.string.settings_reduce_motion))
                            Text(
                                stringResource(R.string.settings_reduce_motion_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = reduceMotion,
                            onCheckedChange = { enabled ->
                                if (enabled) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setReduceMotion(enabled) }
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
                        Icon(Icons.Rounded.Shield, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.settings_section_advanced), style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_noko_guard))
                            Text(
                                stringResource(R.string.settings_noko_guard_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = nokoGuard,
                            onCheckedChange = { enabled ->
                                if (enabled) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setNokoGuard(enabled) }
                            },
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.tap()
                                navController.navigate("noko_polkit")
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_noko_polkit))
                            Text(
                                stringResource(R.string.settings_noko_polkit_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.settings_credits),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
