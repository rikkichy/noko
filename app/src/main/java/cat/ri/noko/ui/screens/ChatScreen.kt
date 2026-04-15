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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import cat.ri.noko.ui.theme.NokoFieldShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonShapes
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.ChatStorage
import cat.ri.noko.core.HallucinationDetector
import cat.ri.noko.core.PromptBuilder
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.core.api.ApiClient
import cat.ri.noko.model.getProviderById
import cat.ri.noko.model.defaultPromptPreset
import cat.ri.noko.model.ChatMessage
import cat.ri.noko.model.ChatSessionMeta
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
import cat.ri.noko.model.api.ChatRequest
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

private fun randomPlaceholder(personaName: String?): String {
    if (personaName == null) return listOf(
        "Message",
        "Write something...",
        "What happens next?",
        "Say something...",
    ).random()
    return listOf(
        "Say something as $personaName",
        "Yap as $personaName",
        "Write as $personaName...",
        "What does $personaName do?",
        "Speak as $personaName...",
        "$personaName says...",
        "Talk as $personaName...",
        "Type as $personaName...",
    ).random()
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
    var hasStreamedContent by remember { mutableStateOf(false) }
    var streamJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val showAvatars by SettingsManager.showAvatars.collectAsState(initial = true)
    val showNames by SettingsManager.showNames.collectAsState(initial = true)
    val reduceMotion by SettingsManager.reduceMotion.collectAsState(initial = false)
    val nokoGuard by SettingsManager.nokoGuard.collectAsState(initial = true)
    val nokoPolkitTrimEmojis by SettingsManager.nokoPolkitTrimEmojis.collectAsState(initial = true)
    val nokoPolkitStructureActions by SettingsManager.nokoPolkitStructureActions.collectAsState(initial = true)
    val streamNotifications by SettingsManager.nokoPolkitStreamNotifications.collectAsState(initial = false)
    val apiKey by SettingsManager.apiKey.collectAsState(initial = "")
    val modelId by SettingsManager.selectedModelId.collectAsState(initial = "")
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

    val placeholder = remember(activePersona) {
        randomPlaceholder(activePersona?.name)
    }

    var editingMessageIdx by remember { mutableStateOf(-1) }
    var editingText by remember { mutableStateOf("") }


    fun saveCurrentChat() {
        if (isSecretChat) return
        val character = activeCharacter ?: return
        val lastMsg = messages.lastOrNull { !it.isGreeting && it.content.isNotBlank() } ?: return
        val senderName = lastMsg.senderName ?: when (lastMsg.role) {
            ChatMessage.Role.USER -> activePersona?.name ?: "You"
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
        hasStreamedContent = false
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
            val buffer = StringBuilder()
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
                )
                var lastUiUpdate = 0L
                var wordCount = 0
                var tokenCount = 0
                var lastGuardCheck = 0
                ApiClient.streamChat(request).collect { token ->
                    buffer.append(token)
                    tokenCount++

                    if (nokoGuard && tokenCount >= 50 && tokenCount - lastGuardCheck >= 50) {
                        lastGuardCheck = tokenCount
                        val violation = HallucinationDetector.scan(
                            content = buffer.toString(),
                            userName = activePersona?.name,
                            previousMessages = messages.subList(0, assistantIdx),
                        )
                        if (violation != null) {
                            messages[assistantIdx] = messages[assistantIdx].copy(
                                content = buffer.toString(),
                                guardBlocked = true,
                                guardReason = "${violation.label} [${violation.code}]",
                            )
                            haptics.reject()
                            throw kotlinx.coroutines.CancellationException("NokoGuard")
                        }
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastUiUpdate > 32) {
                        lastUiUpdate = now
                        if (!hasStreamedContent) hasStreamedContent = true
                        messages[assistantIdx] = messages[assistantIdx].copy(
                            content = buffer.toString(),
                        )
                    }
                    if (token.contains(' ') || token.contains('\n')) {
                        wordCount++
                        if (wordCount % 2 == 0) haptics.tick()
                    }
                }

                val finalContent = buffer.toString()
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
                )
                messages[assistantIdx] = updated.syncActiveAlternative()
                haptics.confirm()
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (assistantIdx < messages.size && !messages[assistantIdx].guardBlocked) {
                    val stopped = messages[assistantIdx].copy(
                        content = buffer.toString(),
                        stoppedByUser = true,
                    )
                    messages[assistantIdx] = stopped.syncActiveAlternative()
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
                if (streamNotifications && assistantIdx < messages.size) {
                    val msg = messages[assistantIdx]
                    val isBackground = !ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                    if (isBackground && msg.content.isNotBlank() && !msg.stoppedByUser && !msg.guardBlocked) {
                        val titles = listOf(
                            "${activeCharacter?.name ?: "AI"} replied to you!",
                            "${activeCharacter?.name ?: "AI"} answered you!",
                            "${activeCharacter?.name ?: "AI"} responded!",
                            "${activeCharacter?.name ?: "AI"} wrote back!",
                        )
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
                                    "Tap to select character",
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
                                        "Secret chat",
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
                IconButton(onClick = {
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
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "New chat")
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
            val showTyping = isGenerating &&
                (lastMsg == null || lastMsg.role != ChatMessage.Role.ASSISTANT || lastMsg.content.isBlank())
            if (showTyping) {
                item(key = "typing_indicator") {
                    TypingIndicator(
                        characterName = activeCharacter?.name ?: "Assistant",
                        avatarFileName = activeCharacter?.avatarFileName,
                        showAvatar = showAvatars,
                        reduceMotion = reduceMotion,
                    )
                }
            }

            items(reversedMessages, key = { it.id }) { message ->

                if (isGenerating && message == lastMsg
                    && message.role == ChatMessage.Role.ASSISTANT && message.content.isBlank()
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
                    onRollback = if (!message.isGreeting) {
                        {
                            val idx = messages.indexOfFirst { it.id == message.id }
                            if (idx >= 0) {
                                while (messages.size > idx + 1) {
                                    messages.removeAt(messages.lastIndex)
                                }
                            }
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
                title = { Text("Edit message") },
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
                    ) { Text("Save") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { editingMessageIdx = -1 },
                    ) { Text("Cancel") }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rpFormats.forEach { fmt ->
                    AssistChip(
                        onClick = {
                            haptics.tap()
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
                        label = { Text(fmt.label) },
                    )
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
                    if (isGenerating && hasStreamedContent) {
                        haptics.reject()
                        streamJob?.cancel()
                        return@FilledIconButton
                    }
                    if (isGenerating) return@FilledIconButton
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
                if (isGenerating && hasStreamedContent) {
                    Icon(Icons.Filled.Stop, contentDescription = "Stop")
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }


    if (showCharacterPicker) {
        PersonaPickerSheet(
            title = "Select Character",
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
            title = "Select Persona",
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
}




