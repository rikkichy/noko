package cat.ri.noko.ui.screens

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import cat.ri.noko.MainActivity
import cat.ri.noko.NokoApplication
import cat.ri.noko.R
import cat.ri.noko.core.replaceTemplateVars
import cat.ri.noko.ui.components.MessageBubble
import cat.ri.noko.ui.components.NokoAvatar
import cat.ri.noko.ui.components.PersonaPickerSheet
import cat.ri.noko.ui.components.TypingIndicator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import cat.ri.noko.ui.theme.NokoFieldShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.DropdownMenuItem
import cat.ri.noko.ui.components.NokoDropdown
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PlatformImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.ChatStorage
import cat.ri.noko.core.HallucinationDetector
import cat.ri.noko.core.PromptBuilder
import cat.ri.noko.core.SettingsManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import cat.ri.noko.core.api.ApiClient
import cat.ri.noko.core.api.StreamEvent
import cat.ri.noko.model.getProviderById
import cat.ri.noko.model.defaultPromptPreset
import cat.ri.noko.model.ChatMessage
import cat.ri.noko.model.ChatSessionMeta
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
import cat.ri.noko.model.api.ChatRequest
import cat.ri.noko.model.api.ReasoningParam
import cat.ri.noko.ui.ChatAction
import cat.ri.noko.ui.theme.nokoTopAppBarColors
import cat.ri.noko.ui.util.stripEmojis
import cat.ri.noko.ui.util.structureActions
import cat.ri.noko.ui.util.rememberNokoHaptics
import kotlinx.coroutines.launch
import java.util.UUID



private data class RpFormat(
    val label: String,
    val prefix: String,
    val suffix: String,
)

private data class Participant(
    val name: String?,
    val avatarFileName: String?,
)

@Composable
private fun rememberPlaceholder(personaName: String?): String {
    val genericPlaceholders = stringArrayResource(R.array.chat_placeholder_generic)
    val personaPlaceholders = stringArrayResource(R.array.chat_placeholder_persona)
    return remember(personaName, genericPlaceholders, personaPlaceholders) {
        if (personaName == null) genericPlaceholders.random()
        else personaPlaceholders.random().format(personaName)
    }
}

private val rpFormats = listOf(
    RpFormat("*Action*", "*", "*"),
    RpFormat("((OOC))", "((", "))"),
    RpFormat("\"Speech\"", "\"", "\""),
    RpFormat("~Whisper~", "~", "~"),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatScreen(
    pendingAction: ChatAction? = null,
    onActionConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val reversedMessages by remember { derivedStateOf { messages.toList().asReversed() } }
    var input by remember { mutableStateOf(TextFieldValue()) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = rememberNokoHaptics()
    val imeVisible = WindowInsets.isImeVisible
    var chatInputFocused by remember { mutableStateOf(false) }
    val showRpChips = imeVisible && chatInputFocused
    val incognitoKeyboard by SettingsManager.incognitoKeyboard.collectAsState(initial = false)
    val incognitoKeyboardOptions = if (incognitoKeyboard) KeyboardOptions(
        platformImeOptions = PlatformImeOptions(
            privateImeOptions = "com.google.android.inputmethod.latin.noPersonalizedLearning",
        ),
    ) else KeyboardOptions.Default


    var currentChatId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var isSecretChat by remember { mutableStateOf(false) }


    val allEntries by SettingsManager.allEntries.collectAsState(initial = emptyList())
    val personas = remember(allEntries) { allEntries.filter { it.type == PersonaType.PERSONA } }
    val characters = remember(allEntries) { allEntries.filter { it.type == PersonaType.CHARACTER } }

    val selectedPersonaId by SettingsManager.selectedPersonaId.collectAsState(initial = null)
    val selectedCharacterId by SettingsManager.selectedCharacterId.collectAsState(initial = null)
    var characterIdOverride by remember { mutableStateOf<String?>(null) }
    val effectiveCharacterId = characterIdOverride ?: selectedCharacterId

    LaunchedEffect(selectedCharacterId) {
        if (selectedCharacterId == characterIdOverride) characterIdOverride = null
    }

    val activePersona = remember(allEntries, selectedPersonaId) {
        selectedPersonaId?.let { id -> allEntries.find { it.id == id } }
    }
    val activeCharacter = remember(allEntries, effectiveCharacterId) {
        effectiveCharacterId?.let { id -> allEntries.find { it.id == id } }
    }

    var showCharacterPicker by remember { mutableStateOf(false) }
    var showPersonaPicker by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var streamJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val showAvatars by SettingsManager.showAvatars.collectAsState(initial = true)
    val showNames by SettingsManager.showNames.collectAsState(initial = true)
    val reduceMotion by SettingsManager.reduceMotion.collectAsState(initial = false)
    val nokoGuard by SettingsManager.nokoGuard.collectAsState(initial = true)
    val nokoPolkitTrimEmojis by SettingsManager.nokoPolkitTrimEmojis.collectAsState(initial = true)
    val nokoPolkitStructureActions by SettingsManager.nokoPolkitStructureActions.collectAsState(initial = true)
    val streamNotifications by SettingsManager.nokoPolkitStreamNotifications.collectAsState(initial = false)
    val showReasoning by SettingsManager.nokoPolkitShowReasoning.collectAsState(initial = true)
    val apiKey by SettingsManager.apiKey.collectAsState(initial = "")
    val modelId by SettingsManager.selectedModelId.collectAsState(initial = "")
    val modelName by SettingsManager.selectedModelName.collectAsState(initial = "")
    val modelContextLength by SettingsManager.selectedModelContextLength.collectAsState(initial = 0)
    val presets by SettingsManager.promptPresets.collectAsState(initial = listOf(defaultPromptPreset()))
    val selectedPresetId by SettingsManager.selectedPresetId.collectAsState(initial = "default")
    val providerId by SettingsManager.selectedProviderId.collectAsState(initial = SettingsManager.getSelectedProviderId())
    val customUrl by SettingsManager.customProviderUrl.collectAsState(initial = "")
    val customAuth by SettingsManager.customProviderAuth.collectAsState(initial = false)
    val provider = remember(providerId) { getProviderById(providerId) }
    val providerRequiresAuth = provider?.requiresAuth ?: customAuth
    val urlOverride by SettingsManager.getProviderUrlOverride(providerId).collectAsState(initial = "")
    val providerBaseUrl = if (provider != null) urlOverride.ifBlank { provider.baseUrl } else customUrl
    val activePreset = remember(presets, selectedPresetId) {
        presets.find { it.id == selectedPresetId } ?: presets.first()
    }

    val placeholder = rememberPlaceholder(activePersona?.name)

    var editingMessageIdx by remember { mutableStateOf(-1) }
    var editingText by remember { mutableStateOf("") }
    var showStatsSheet by remember { mutableStateOf(false) }
    var activeFormatIdx by remember { mutableStateOf(-1) }
    val notificationTitleTemplates = stringArrayResource(R.array.chat_notification_titles)
    val aiFallbackName = stringResource(R.string.common_ai)


    fun saveCurrentChat() {
        if (isSecretChat) return
        val character = activeCharacter ?: return
        val lastMsg = messages.lastOrNull { !it.isGreeting && it.content.isNotBlank() } ?: return
        val youFallback = context.getString(R.string.common_you)
        val senderName = lastMsg.senderName ?: when (lastMsg.role) {
            ChatMessage.Role.USER -> activePersona?.name ?: youFallback
            ChatMessage.Role.ASSISTANT -> character.name
        }
        val cleanContent = lastMsg.content
            .replace(Regex("(?<!\\*)\\*(?!\\*)((?:(?!\\*).)+?)\\*(?!\\*)"), "$1")
            .replace('\n', ' ')
            .trim()
        val preview = "$senderName: ${cleanContent.take(100)}"
        val meta = ChatSessionMeta(
            id = currentChatId,
            characterId = character.id,
            characterName = character.name,
            characterAvatarFileName = character.avatarFileName,
            lastMessagePreview = preview,
            lastMessageRole = lastMsg.role.name,
            updatedAt = System.currentTimeMillis(),
            messageCount = messages.count { !it.isGreeting },
            personaName = activePersona?.name,
            personaAvatarFileName = activePersona?.avatarFileName,
        )
        scope.launch {
            ChatStorage.saveChat(
                chatId = currentChatId,
                messages = messages.toList(),
                meta = meta,
            )
        }
    }


    fun startStreaming(targetIdx: Int? = null, continueNudge: String? = null) {
        if ((providerRequiresAuth && apiKey.isBlank()) || modelId.isBlank() || providerBaseUrl.isBlank()) return
        isGenerating = true
        val assistantIdx: Int
        if (targetIdx != null) {
            assistantIdx = targetIdx
            messages[assistantIdx] = messages[assistantIdx].copy(
                content = "",
                stoppedByUser = false,
                guardBlocked = false,
                guardReason = null,
                emojisTrimmed = false,
                actionsStructured = false,
                reasoningContent = null,
                reasoningDurationMs = null,
            )
        } else {
            assistantIdx = messages.size
            messages.add(ChatMessage(
                role = ChatMessage.Role.ASSISTANT,
                content = "",
                senderName = activeCharacter?.name,
                senderAvatarFileName = activeCharacter?.avatarFileName,
            ))
        }

        streamJob = scope.launch {
            val contentBuffer = StringBuilder()
            val reasoningBuffer = StringBuilder()
            var reasoningStartMs: Long? = null
            var reasoningDurationMs: Long? = null
            var recoveredFromBlank = false
            try {
                ApiClient.configure(apiKey, providerBaseUrl, providerId)
                val apiMessages = PromptBuilder.buildMessages(
                    preset = activePreset,
                    persona = activePersona,
                    character = activeCharacter,
                    chatMessages = if (targetIdx != null) messages.take(targetIdx) else messages.dropLast(1),
                    continueNudge = continueNudge,
                )
                val request = ChatRequest(
                    model = modelId,
                    messages = apiMessages,
                    stream = true,
                    temperature = activePreset.temperature,
                    maxTokens = activePreset.maxTokens,
                    topP = activePreset.topP,
                    topK = activePreset.topK,
                    frequencyPenalty = activePreset.frequencyPenalty,
                    presencePenalty = activePreset.presencePenalty,
                    reasoning = if (providerId == "openrouter") ReasoningParam(enabled = true) else null,
                )
                var lastUiUpdate = 0L
                var wordCount = 0
                var contentTokenCount = 0
                var lastGuardCheck = 0
                ApiClient.streamChat(request).collect { event ->
                    when (event) {
                        is StreamEvent.Reasoning -> {
                            if (reasoningStartMs == null) reasoningStartMs = System.currentTimeMillis()
                            reasoningBuffer.append(event.text)
                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdate > 32) {
                                lastUiUpdate = now
                                messages[assistantIdx] = messages[assistantIdx].copy(
                                    reasoningContent = reasoningBuffer.toString(),
                                )
                            }
                        }
                        is StreamEvent.Content -> {
                            if (reasoningDurationMs == null && reasoningStartMs != null) {
                                reasoningDurationMs = System.currentTimeMillis() - reasoningStartMs!!
                            }
                            contentBuffer.append(event.text)
                            contentTokenCount++

                            if (nokoGuard && contentTokenCount >= 50 && contentTokenCount - lastGuardCheck >= 50) {
                                lastGuardCheck = contentTokenCount
                                val violation = HallucinationDetector.scan(
                                    content = contentBuffer.toString(),
                                    userName = activePersona?.name,
                                    previousMessages = messages.subList(0, assistantIdx),
                                )
                                if (violation != null) {
                                    messages[assistantIdx] = messages[assistantIdx].copy(
                                        content = contentBuffer.toString(),
                                        guardBlocked = true,
                                        guardReason = "${violation.label} [${violation.code}]",
                                        reasoningContent = reasoningBuffer.toString().ifEmpty { null },
                                        reasoningDurationMs = reasoningDurationMs,
                                    )
                                    haptics.reject()
                                    throw kotlinx.coroutines.CancellationException("NokoGuard")
                                }
                            }

                            val now = System.currentTimeMillis()
                            if (now - lastUiUpdate > 32) {
                                lastUiUpdate = now
                                messages[assistantIdx] = messages[assistantIdx].copy(
                                    content = contentBuffer.toString(),
                                    reasoningContent = reasoningBuffer.toString().ifEmpty { null },
                                    reasoningDurationMs = reasoningDurationMs,
                                )
                            }
                            if (event.text.contains(' ') || event.text.contains('\n')) {
                                wordCount++
                                if (wordCount % 2 == 0) haptics.tick()
                            }
                        }
                    }
                }

                val finalContent = contentBuffer.toString()
                val finalReasoning = reasoningBuffer.toString().ifEmpty { null }
                val finalReasoningDuration = reasoningDurationMs
                    ?: reasoningStartMs?.let { System.currentTimeMillis() - it }
                if (nokoGuard) {
                    val violation = HallucinationDetector.scan(
                        content = finalContent,
                        userName = activePersona?.name,
                        previousMessages = messages.subList(0, assistantIdx),
                    )
                    if (violation != null) {
                        messages[assistantIdx] = messages[assistantIdx].copy(
                            content = finalContent,
                            guardBlocked = true,
                            guardReason = "${violation.label} [${violation.code}]",
                            reasoningContent = finalReasoning,
                            reasoningDurationMs = finalReasoningDuration,
                        )
                        haptics.reject()
                        return@launch
                    }
                }
                var processed = finalContent
                var emojisTrimmed = false
                var actionsStructured = false
                if (nokoPolkitTrimEmojis) {
                    val trimmed = stripEmojis(processed)
                    if (trimmed != processed) {
                        processed = trimmed
                        emojisTrimmed = true
                    }
                }
                if (nokoPolkitStructureActions) {
                    val structured = structureActions(processed)
                    if (structured != processed) {
                        processed = structured
                        actionsStructured = true
                    }
                }
                val updated = messages[assistantIdx].copy(
                    content = processed,
                    emojisTrimmed = emojisTrimmed,
                    actionsStructured = actionsStructured,
                    reasoningContent = finalReasoning,
                    reasoningDurationMs = finalReasoningDuration,
                )
                messages[assistantIdx] = updated.syncActiveAlternative()
                haptics.confirm()
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (assistantIdx < messages.size && !messages[assistantIdx].guardBlocked) {
                    val msg = messages[assistantIdx]
                    val hasContent = contentBuffer.isNotBlank()
                    val hasReasoning = reasoningBuffer.isNotEmpty()
                    if (!hasContent && !hasReasoning) {
                        recoveredFromBlank = true
                        val recovered = msg.recoverFromBlankRegeneration()
                        if (recovered != null) {
                            messages[assistantIdx] = recovered
                        } else {
                            messages.removeAt(assistantIdx)
                        }
                    } else {
                        val finalReasoningDuration = reasoningDurationMs
                            ?: reasoningStartMs?.let { System.currentTimeMillis() - it }
                        val stopped = msg.copy(
                            content = contentBuffer.toString(),
                            stoppedByUser = true,
                            reasoningContent = reasoningBuffer.toString().ifEmpty { null },
                            reasoningDurationMs = finalReasoningDuration,
                        )
                        messages[assistantIdx] = stopped.syncActiveAlternative()
                    }
                }
            } catch (e: Exception) {
                val errorMsg = cat.ri.noko.core.api.humanizeException(e)
                if (assistantIdx < messages.size) {
                    val current = messages[assistantIdx].content
                    messages[assistantIdx] = messages[assistantIdx].copy(
                        content = if (current.isBlank()) "⚠ $errorMsg"
                        else "$current\n\n⚠ $errorMsg",
                    )
                }
            } finally {
                isGenerating = false
                streamJob = null
                saveCurrentChat()
                if (streamNotifications && !recoveredFromBlank && assistantIdx < messages.size) {
                    val msg = messages[assistantIdx]
                    val isBackground = !ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    if (isBackground && msg.content.isNotBlank() && !msg.stoppedByUser && !msg.guardBlocked) {
                        val charName = activeCharacter?.name ?: aiFallbackName
                        val titles = notificationTitleTemplates.map { it.format(charName) }
                        val preview = msg.content.take(100).let { if (msg.content.length > 100) "$it..." else it }
                        val tapIntent = PendingIntent.getActivity(
                            context, currentChatId.hashCode(),
                            Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(MainActivity.EXTRA_NAVIGATE_TO_CHAT, true)
                            },
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                        )
                        val notification = NotificationCompat.Builder(context, NokoApplication.CHANNEL_STREAM_COMPLETE)
                            .setSmallIcon(R.drawable.ic_notif)
                            .setContentTitle(titles.random())
                            .setContentText(preview)
                            .setContentIntent(tapIntent)
                            .setAutoCancel(true)
                            .build()
                        runCatching {
                            NotificationManagerCompat.from(context).notify(currentChatId.hashCode(), notification)
                        }
                    }
                }
            }
        }
    }


    LaunchedEffect(pendingAction) {
        when (val action = pendingAction) {
            is ChatAction.NewChat -> {
                streamJob?.cancel()
                messages.clear()
                currentChatId = UUID.randomUUID().toString()
                isSecretChat = action.isSecret

                if (activeCharacter?.greetingMessage != null) {
                    messages.add(
                        ChatMessage(
                            role = ChatMessage.Role.ASSISTANT,
                            content = activeCharacter.greetingMessage
                                .replaceTemplateVars(activeCharacter.name, activePersona?.name ?: "User"),
                            isGreeting = true,
                            senderName = activeCharacter.name,
                            senderAvatarFileName = activeCharacter.avatarFileName,
                        ),
                    )
                }
                onActionConsumed()
            }
            is ChatAction.OpenRecent -> {
                streamJob?.cancel()
                messages.clear()
                currentChatId = action.meta.id
                isSecretChat = false

                val loaded = ChatStorage.loadChat(action.meta.id)
                if (loaded != null) {
                    messages.addAll(loaded)
                }

                SettingsManager.setSelectedCharacterId(action.meta.characterId)
                onActionConsumed()
            }
            null -> {  }
        }
    }


    LaunchedEffect(activeCharacter?.id) {
        if (messages.isEmpty() && activeCharacter?.greetingMessage != null) {
            messages.add(
                ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    content = activeCharacter.greetingMessage
                        .replaceTemplateVars(activeCharacter.name, activePersona?.name ?: "User"),
                    isGreeting = true,
                    senderName = activeCharacter.name,
                    senderAvatarFileName = activeCharacter.avatarFileName,
                ),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {

        TopAppBar(
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .alpha(if (isGenerating) 0.4f else 1f)
                            .clickable(enabled = !isGenerating) {
                                haptics.tap()
                                showCharacterPicker = true
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NokoAvatar(
                            entry = activeCharacter,
                            fallbackIcon = Icons.Filled.SmartToy,
                            size = 32,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            if (activeCharacter != null) {
                                Text(
                                    activeCharacter.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else {
                                Text(
                                    stringResource(R.string.chat_tap_to_select_character),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (isSecretChat) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.VisibilityOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        stringResource(R.string.chat_secret_label),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            actions = {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = {
                        haptics.tap()
                        menuOpen = true
                    }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.common_more))
                    }
                    NokoDropdown(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_menu_new_chat)) },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                haptics.tap()
                                streamJob?.cancel()
                                messages.clear()
                                currentChatId = UUID.randomUUID().toString()
                                isSecretChat = false

                                if (activeCharacter?.greetingMessage != null) {
                                    messages.add(
                                        ChatMessage(
                                            role = ChatMessage.Role.ASSISTANT,
                                            content = activeCharacter.greetingMessage
                                                .replaceTemplateVars(activeCharacter.name, activePersona?.name ?: "User"),
                                            isGreeting = true,
                                            senderName = activeCharacter.name,
                                            senderAvatarFileName = activeCharacter.avatarFileName,
                                        ),
                                    )
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_menu_chat_info)) },
                            leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                haptics.tap()
                                showStatsSheet = true
                            },
                        )
                    }
                }
            },
            colors = nokoTopAppBarColors(),
        )


        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp),
        ) {

            val lastMsg = messages.lastOrNull()
            val lastHasVisibleReasoning = lastMsg != null &&
                showReasoning && !lastMsg.reasoningContent.isNullOrEmpty()
            val showTyping = isGenerating &&
                (lastMsg == null || lastMsg.role != ChatMessage.Role.ASSISTANT ||
                    (lastMsg.content.isBlank() && !lastHasVisibleReasoning))
            if (showTyping) {
                item(key = "typing_indicator") {
                    TypingIndicator(
                        characterName = activeCharacter?.name ?: stringResource(R.string.common_assistant),
                        avatarFileName = activeCharacter?.avatarFileName,
                        showAvatar = showAvatars,
                        reduceMotion = reduceMotion,
                    )
                }
            }

            items(reversedMessages, key = { it.id }) { message ->

                val hasVisibleReasoning = showReasoning && !message.reasoningContent.isNullOrEmpty()
                if (isGenerating && message == lastMsg
                    && message.role == ChatMessage.Role.ASSISTANT
                    && message.content.isBlank() && !hasVisibleReasoning
                ) return@items

                val isStreamingThis = isGenerating
                    && message == lastMsg
                    && message.role == ChatMessage.Role.ASSISTANT
                    && message.content.isNotBlank()

                MessageBubble(
                    message = message,
                    persona = activePersona,
                    character = activeCharacter,
                    showAvatars = showAvatars,
                    showNames = showNames,
                    reduceMotion = reduceMotion,
                    isStreaming = isStreamingThis,
                    isGenerating = isGenerating,
                    showReasoning = showReasoning,
                    onRegenerate = if (message.role == ChatMessage.Role.ASSISTANT && !message.isGreeting) {
                        {
                            val idx = messages.indexOfFirst { it.id == message.id }
                            if (idx >= 0) {
                                messages[idx] = messages[idx].addRegeneration()
                                startStreaming(targetIdx = idx)
                            }
                        }
                    } else null,
                    onEdit = if (!message.isGreeting) {
                        {
                            val idx = messages.indexOfFirst { it.id == message.id }
                            if (idx >= 0) {
                                editingMessageIdx = idx
                                editingText = message.content
                            }
                        }
                    } else null,
                    onRollback = if (!message.isGreeting && message != lastMsg) {
                        {
                            val idx = messages.indexOfFirst { it.id == message.id }
                            if (idx >= 0) {
                                while (messages.size > idx + 1) {
                                    messages.removeAt(messages.lastIndex)
                                }
                            }
                        }
                    } else null,
                    onDelete = if (!message.isGreeting && message == lastMsg) {
                        {
                            val idx = messages.indexOfFirst { it.id == message.id }
                            if (idx >= 0) messages.removeAt(idx)
                        }
                    } else null,
                    onSwipe = if (message.role == ChatMessage.Role.ASSISTANT && !message.isGreeting && message.swipeCount > 1 && !isGenerating) {
                        { direction ->
                            val msgIdx = messages.indexOfFirst { it.id == message.id }
                            if (msgIdx >= 0) {
                                val current = messages[msgIdx]
                                val swiped = current.swipeTo(current.activeIndex + direction)
                                if (swiped !== current) {
                                    messages[msgIdx] = swiped
                                    saveCurrentChat()
                                }
                            }
                        }
                    } else null,
                )
            }
        }


        if (editingMessageIdx >= 0) {
            AlertDialog(
                onDismissRequest = { editingMessageIdx = -1 },
                title = { Text(stringResource(R.string.chat_edit_title)) },
                text = {
                    TextField(
                        value = editingText,
                        onValueChange = { editingText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 8,
                        keyboardOptions = incognitoKeyboardOptions,
                        shape = NokoFieldShape,
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            if (editingMessageIdx in messages.indices) {
                                messages[editingMessageIdx] = messages[editingMessageIdx].copy(
                                    content = editingText,
                                )
                            }
                            editingMessageIdx = -1
                        },
                    ) { Text(stringResource(R.string.common_save)) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { editingMessageIdx = -1 },
                    ) { Text(stringResource(R.string.common_cancel)) }
                },
            )
        }


        LaunchedEffect(showRpChips) {
            if (showRpChips) {
                delay(100)
                haptics.tick()
            }
        }
        AnimatedVisibility(
            visible = showRpChips,
            enter = if (reduceMotion) expandVertically(tween(0))
                    else fadeIn(tween(200, delayMillis = 80)) + slideInVertically(tween(250, delayMillis = 80)) { it },
            exit = if (reduceMotion) shrinkVertically(tween(0))
                   else fadeOut(tween(170)) + slideOutVertically(tween(200)) { it },
        ) {
            ButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                rpFormats.forEachIndexed { idx, fmt ->
                    val shapes = when (idx) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        rpFormats.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    ToggleButton(
                        checked = activeFormatIdx == idx,
                        onCheckedChange = {
                            haptics.tap()
                            activeFormatIdx = idx
                            val text = input.text
                            val cursor = input.selection.min
                            val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
                            val lineEndIdx = text.indexOf('\n', cursor)
                            val lineEnd = if (lineEndIdx == -1) text.length else lineEndIdx
                            val line = text.substring(lineStart, lineEnd)
                            val wrapped = "${fmt.prefix}${line}${fmt.suffix}"
                            val newText = text.replaceRange(lineStart, lineEnd, wrapped)
                            val newCursor = lineStart + wrapped.length
                            input = TextFieldValue(
                                text = newText,
                                selection = TextRange(newCursor),
                            )
                        },
                        shapes = shapes,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .weight(1f)
                            .then(if (reduceMotion) Modifier else Modifier.animateWidth(interactionSource)),
                    ) {
                        Text(fmt.label, maxLines = 1)
                    }
                }
            }
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .alpha(if (isGenerating) 0.4f else 1f)
                    .clickable(enabled = !isGenerating) {
                        haptics.tap()
                        showPersonaPicker = true
                    },
            ) {
                NokoAvatar(
                    entry = activePersona,
                    fallbackIcon = Icons.Filled.Person,
                    size = 32,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { chatInputFocused = it.isFocused },
                placeholder = { Text(placeholder) },
                singleLine = false,
                maxLines = 4,
                keyboardOptions = incognitoKeyboardOptions,
                shape = NokoFieldShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (isGenerating) {
                        haptics.reject()
                        streamJob?.cancel()
                        return@FilledIconButton
                    }
                    val text = input.text.trim()
                    if (text.isNotEmpty()) {
                        haptics.tap()
                        messages.add(ChatMessage(
                            role = ChatMessage.Role.USER,
                            content = text,
                            senderName = activePersona?.name,
                            senderAvatarFileName = activePersona?.avatarFileName,
                        ))
                        input = TextFieldValue()
                        keyboard?.hide()

                        scope.launch {
                            listState.animateScrollToItem(0)
                        }

                        startStreaming()
                    } else if (messages.isNotEmpty() && messages.last().role == ChatMessage.Role.USER) {
                        haptics.tap()
                        keyboard?.hide()
                        startStreaming()
                    } else if (messages.isNotEmpty() && messages.last().role == ChatMessage.Role.ASSISTANT) {
                        haptics.tap()
                        keyboard?.hide()
                        startStreaming(continueNudge = activePreset.continueNudgePrompt)
                    }
                },
                shapes = IconButtonShapes(
                    shape = CircleShape,
                    pressedShape = RoundedCornerShape(8.dp),
                ),
            ) {
                if (isGenerating) {
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.chat_stop))
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.chat_send))
                }
            }
        }
    }


    if (showCharacterPicker) {
        PersonaPickerSheet(
            title = stringResource(R.string.chat_select_character),
            entries = characters,
            selectedId = effectiveCharacterId,
            onSelect = { entry ->
                haptics.confirm()
                if (entry.id != effectiveCharacterId) {
                    saveCurrentChat()
                    streamJob?.cancel()
                    messages.clear()
                    currentChatId = UUID.randomUUID().toString()
                    isSecretChat = false
                    characterIdOverride = entry.id
                    scope.launch { SettingsManager.setSelectedCharacterId(entry.id) }
                }
                showCharacterPicker = false
            },
            onDismiss = { showCharacterPicker = false },
        )
    }


    if (showPersonaPicker) {
        PersonaPickerSheet(
            title = stringResource(R.string.chat_select_persona),
            entries = personas,
            selectedId = selectedPersonaId,
            onSelect = { entry ->
                haptics.confirm()
                scope.launch { SettingsManager.setSelectedPersonaId(entry.id) }
                showPersonaPicker = false
            },
            onDismiss = { showPersonaPicker = false },
        )
    }


    if (showStatsSheet) {
        val statsSheetState = rememberModalBottomSheetState()
        val userMsgs = messages.count { !it.isGreeting && it.role == cat.ri.noko.model.ChatMessage.Role.USER }
        val assistantMsgs = messages.count { !it.isGreeting && it.role == cat.ri.noko.model.ChatMessage.Role.ASSISTANT }
        val totalChars = messages.filter { !it.isGreeting }.sumOf { it.content.length }
        val estimatedTokens = totalChars / 4
        val contextPct = if (modelContextLength > 0) {
            (estimatedTokens.toFloat() / modelContextLength).coerceIn(0f, 1f)
        } else 0f
        val guardBlocks = messages.count { it.guardBlocked }
        val stoppedCount = messages.count { it.stoppedByUser }

        val userParticipants = remember(messages.size, activePersona?.id) {
            val fromMessages = messages
                .filter { it.role == ChatMessage.Role.USER && !it.isGreeting }
                .map { Participant(it.senderName, it.senderAvatarFileName) }
                .filter { it.name != null || it.avatarFileName != null }
                .distinct()
            fromMessages.ifEmpty {
                activePersona?.let { listOf(Participant(it.name, it.avatarFileName)) } ?: emptyList()
            }
        }
        val characterParticipant = activeCharacter?.let { Participant(it.name, it.avatarFileName) }

        val tipPickModel = stringResource(R.string.chat_tip_pick_model)
        val tipContextUnknown = stringResource(R.string.chat_tip_context_unknown)
        val tipContextFull = stringResource(R.string.chat_tip_context_full, (contextPct * 100).toInt())
        val tipNoPersona = stringResource(R.string.chat_tip_no_persona)
        val tipGuardBlocks = stringResource(R.string.chat_tip_guard_blocks, guardBlocks)
        val tipKickoff = stringResource(R.string.chat_tip_kickoff)
        val tips = buildList {
            if (modelId.isBlank()) add(tipPickModel)
            if (modelId.isNotBlank() && modelContextLength == 0) add(tipContextUnknown)
            if (modelContextLength > 0 && contextPct > 0.8f) add(tipContextFull)
            if (activePersona == null) add(tipNoPersona)
            if (guardBlocks >= 3) add(tipGuardBlocks)
            if (userMsgs == 0 && assistantMsgs <= 1) add(tipKickoff)
        }

        ModalBottomSheet(
            onDismissRequest = { showStatsSheet = false },
            sheetState = statsSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Text(stringResource(R.string.chat_info_title), style = MaterialTheme.typography.titleLarge)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.chat_info_context_usage),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (modelContextLength > 0) {
                                "~${formatCount(estimatedTokens)} / ${formatCount(modelContextLength)}"
                            } else {
                                "~${formatCount(estimatedTokens)} tokens"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    if (modelContextLength > 0) {
                        LinearProgressIndicator(
                            progress = { contextPct },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        stringResource(R.string.chat_info_token_estimate_help),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.chat_info_messages), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (userParticipants.isNotEmpty() || characterParticipant != null) {
                        ParticipantsCard(userParticipants, characterParticipant)
                    }
                    if (guardBlocks > 0 || stoppedCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (guardBlocks > 0) StatTile(stringResource(R.string.chat_info_guards), guardBlocks.toString(), Modifier.weight(1f))
                            if (stoppedCount > 0) StatTile(stringResource(R.string.chat_info_stops), stoppedCount.toString(), Modifier.weight(1f))
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.chat_info_setup), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    InfoRow(stringResource(R.string.chat_info_model), modelName.ifBlank { modelId.ifBlank { stringResource(R.string.settings_tap_to_select) } })
                    InfoRow(stringResource(R.string.chat_info_provider), provider?.name ?: stringResource(R.string.provider_card_custom))
                    InfoRow(stringResource(R.string.chat_info_preset), activePreset.name)
                    if (isSecretChat) InfoRow(stringResource(R.string.chat_info_mode), stringResource(R.string.chat_secret_label))
                }

                if (tips.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.chat_info_tips), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        tips.forEach { TipCard(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun formatUserNames(participants: List<Participant>): String {
    val anonymous = stringResource(R.string.common_assistant)
    val noPersona = stringResource(R.string.common_no_persona)
    val names = participants.map { it.name?.takeIf { n -> n.isNotBlank() } ?: anonymous }
    return when (names.size) {
        0 -> noPersona
        1 -> names[0]
        2 -> stringResource(R.string.chat_participants_two, names[0], names[1])
        else -> stringResource(R.string.chat_participants_overflow, names[0], names[1], names.size - 2)
    }
}

@Composable
private fun StackedAvatars(
    avatars: List<Participant>,
    fallbackIcon: ImageVector,
    ringColor: Color,
    maxShown: Int = 2,
    size: Int = 40,
) {
    val shown = avatars.take(maxShown)
    val overflow = (avatars.size - maxShown).coerceAtLeast(0)
    Row(
        horizontalArrangement = Arrangement.spacedBy((-12).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shown.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .border(2.dp, ringColor, CircleShape),
            ) {
                NokoAvatar(
                    name = null,
                    avatarFileName = null,
                    fallbackIcon = fallbackIcon,
                    size = size,
                )
            }
        } else {
            shown.forEach { p ->
                Box(
                    modifier = Modifier
                        .size(size.dp)
                        .border(2.dp, ringColor, CircleShape),
                ) {
                    NokoAvatar(
                        name = p.name,
                        avatarFileName = p.avatarFileName,
                        fallbackIcon = fallbackIcon,
                        size = size,
                    )
                }
            }
            if (overflow > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .size(size.dp)
                        .border(2.dp, ringColor, CircleShape),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "+$overflow",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantsCard(
    userParticipants: List<Participant>,
    character: Participant?,
) {
    val ringColor = MaterialTheme.colorScheme.surfaceContainer
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StackedAvatars(
                    avatars = userParticipants,
                    fallbackIcon = Icons.Filled.Person,
                    ringColor = ringColor,
                )
                Text(
                    text = formatUserNames(userParticipants),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.common_you),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StackedAvatars(
                    avatars = listOfNotNull(character),
                    fallbackIcon = Icons.Filled.SmartToy,
                    ringColor = ringColor,
                )
                Text(
                    text = character?.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.common_assistant),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.common_ai),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun TipCard(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000f)
    n >= 1_000 -> "%.1fk".format(n / 1_000f)
    else -> n.toString()
}




