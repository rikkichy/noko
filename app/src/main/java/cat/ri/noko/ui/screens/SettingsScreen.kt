package cat.ri.noko.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Description
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.navigation.NavController
import cat.ri.noko.BuildConfig
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.core.api.ApiClient
import cat.ri.noko.model.PersonaType
import cat.ri.noko.model.getProviderById
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
    val providerBaseUrl = provider?.baseUrl ?: customUrl
    var apiKeyInput by remember(apiKey) { mutableStateOf("") }
    var isChangingKey by remember { mutableStateOf(false) }
    var isTestingKey by remember { mutableStateOf(false) }
    var keyError by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                        Icon(Icons.Rounded.Brush, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    }

                    val isDarkTheme = isSystemInDarkTheme()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "AMOLED mode",
                                color = if (isDarkTheme) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                            Text(
                                if (isDarkTheme) "Use pure black background in dark theme."
                                else "Available only on dark theme.",
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
                        Icon(Icons.Rounded.Cloud, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(providerName, style = MaterialTheme.typography.titleMedium)
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
                            Text("Provider")
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
                                Text("API Key", modifier = Modifier.weight(1f))
                                Spacer(Modifier.size(12.dp))
                                TextButton(
                                    onClick = {
                                        haptics.tap()
                                        isChangingKey = true
                                        apiKeyInput = ""
                                        keyError = null
                                    },
                                ) { Text("Use another key") }
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
                                label = { Text("API Key") },
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
                                shape = RoundedCornerShape(20.dp),
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
                                                shakeOffset.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = keyframes {
                                                        durationMillis = 400
                                                        0f at 0
                                                        (-18f) at 50
                                                        18f at 100
                                                        (-14f) at 150
                                                        14f at 200
                                                        (-8f) at 250
                                                        8f at 300
                                                        (-4f) at 350
                                                        0f at 400
                                                    },
                                                )

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
                                    Text(if (isTestingKey) "Testing..." else "Save")
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
                                    ) { Text("Cancel") }
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
                            Text("Model")
                            Text(
                                if (modelName.isNotBlank()) modelName else "Tap to select",
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
                        Icon(Icons.Rounded.Description, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Prompt Builder", style = MaterialTheme.typography.titleMedium)
                    }

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
                            Text("Prompt Template")
                            Text(
                                "Configure what gets sent to the AI",
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
                        Icon(Icons.Rounded.People, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Personas & Characters", style = MaterialTheme.typography.titleMedium)
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
                            Text("Personas")
                            Text(
                                "Your roleplay identities",
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
                            Text("Characters")
                            Text(
                                "AI character profiles",
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
                        Icon(Icons.Rounded.Shield, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Advanced", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("NokoGuard")
                            Text(
                                "Interrupt AI responses that impersonate your persona, use excessive emojis, or switch languages.",
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
                            Text("NokoPolkit")
                            Text(
                                "Chat and app policies.",
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
                    text = "Noko v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
