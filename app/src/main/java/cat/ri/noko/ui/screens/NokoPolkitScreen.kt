package cat.ri.noko.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.SendTimeExtension
import androidx.compose.material.icons.rounded.DisabledVisible
import androidx.compose.material.icons.rounded.TheaterComedy
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import cat.ri.noko.R
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.ui.theme.nokoTopAppBarColors
import cat.ri.noko.ui.util.rememberNokoHaptics
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NokoPolkitScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()
    val trimEmojis by SettingsManager.nokoPolkitTrimEmojis.collectAsState(initial = true)
    val structureActions by SettingsManager.nokoPolkitStructureActions.collectAsState(initial = true)
    val streamNotifications by SettingsManager.nokoPolkitStreamNotifications.collectAsState(initial = false)
    val showReasoning by SettingsManager.nokoPolkitShowReasoning.collectAsState(initial = true)
    val characterHtmlStrip by SettingsManager.characterPolicyHtmlStrip.collectAsState(initial = true)
    val biometricAuth by SettingsManager.biometricAuth.collectAsState(initial = false)
    val screenSecurity by SettingsManager.screenSecurity.collectAsState(initial = false)
    val incognitoKeyboard by SettingsManager.incognitoKeyboard.collectAsState(initial = false)
    val clearClipboard by SettingsManager.clearClipboard.collectAsState(initial = false)
    val hideFromRecents by SettingsManager.hideFromRecents.collectAsState(initial = false)
    val context = LocalContext.current
    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.polkit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = nokoTopAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
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
                        Text(stringResource(R.string.polkit_card_chat), style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.polkit_trim_emojis))
                            Text(
                                stringResource(R.string.polkit_trim_emojis_desc),
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
                            Text(stringResource(R.string.polkit_structure_actions))
                            Text(
                                stringResource(R.string.polkit_structure_actions_desc),
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

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.polkit_show_thinking))
                            Text(
                                stringResource(R.string.polkit_show_thinking_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = showReasoning,
                            onCheckedChange = { value ->
                                if (value) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setNokoPolkitShowReasoning(value) }
                            },
                        )
                    }

                    HorizontalDivider()

                    val notifPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission(),
                    ) { granted ->
                        if (granted) {
                            haptics.toggleOn()
                            scope.launch { SettingsManager.setNokoPolkitStreamNotifications(true) }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.polkit_stream_notifications))
                            Text(
                                if (biometricAuth) stringResource(R.string.polkit_stream_notifications_desc_blocked)
                                else stringResource(R.string.polkit_stream_notifications_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = streamNotifications,
                            enabled = !biometricAuth,
                            onCheckedChange = { value ->
                                if (value) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        haptics.toggleOn()
                                        scope.launch { SettingsManager.setNokoPolkitStreamNotifications(true) }
                                    }
                                } else {
                                    haptics.toggleOff()
                                    scope.launch { SettingsManager.setNokoPolkitStreamNotifications(false) }
                                }
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
                        Icon(Icons.Rounded.TheaterComedy, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.polkit_card_character), style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.polkit_strip_html))
                            Text(
                                stringResource(R.string.polkit_strip_html_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = characterHtmlStrip,
                            onCheckedChange = { value ->
                                if (value) haptics.toggleOn() else haptics.toggleOff()
                                scope.launch { SettingsManager.setCharacterPolicyHtmlStrip(value) }
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
                        Text(stringResource(R.string.polkit_card_app), style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.polkit_biometric_auth))
                            Text(
                                if (biometricAvailable) stringResource(R.string.polkit_biometric_auth_desc)
                                else stringResource(R.string.polkit_biometric_auth_desc_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Switch(
                            checked = biometricAuth,
                            enabled = biometricAvailable,
                            onCheckedChange = { value ->
                                val activity = context as FragmentActivity
                                val executor = ContextCompat.getMainExecutor(context)
                                val callback = object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(
                                        result: BiometricPrompt.AuthenticationResult,
                                    ) {
                                        if (value) haptics.toggleOn() else haptics.toggleOff()
                                        scope.launch { SettingsManager.setBiometricAuth(value) }
                                    }

                                    override fun onAuthenticationError(
                                        errorCode: Int,
                                        errString: CharSequence,
                                    ) {
                                        haptics.reject()
                                    }

                                    override fun onAuthenticationFailed() {
                                        haptics.reject()
                                    }
                                }
                                val prompt = BiometricPrompt(activity, executor, callback)
                                val titleText = if (value)
                                    context.getString(R.string.polkit_biometric_prompt_title_enable)
                                else
                                    context.getString(R.string.polkit_biometric_prompt_title_disable)
                                val info = BiometricPrompt.PromptInfo.Builder()
                                    .setTitle(titleText)
                                    .setSubtitle(context.getString(R.string.polkit_biometric_prompt_subtitle))
                                    .setNegativeButtonText(context.getString(R.string.common_cancel))
                                    .setAllowedAuthenticators(BIOMETRIC_STRONG)
                                    .build()
                                prompt.authenticate(info)
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
                            Text(stringResource(R.string.polkit_screen_security))
                            Text(
                                stringResource(R.string.polkit_screen_security_desc),
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
                            Text(stringResource(R.string.polkit_incognito_keyboard))
                            Text(
                                stringResource(R.string.polkit_incognito_keyboard_desc),
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
                            Text(stringResource(R.string.polkit_clear_clipboard))
                            Text(
                                stringResource(R.string.polkit_clear_clipboard_desc),
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
                            Text(stringResource(R.string.polkit_hide_from_recents))
                            Text(
                                stringResource(R.string.polkit_hide_from_recents_desc),
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
