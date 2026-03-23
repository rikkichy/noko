package cat.ri.noko.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.withFrameMillis
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.AvatarStorage
import cat.ri.noko.core.ChatStorage
import cat.ri.noko.core.PromptBuilder
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.core.api.OpenRouterClient
import cat.ri.noko.model.defaultPromptPreset
import cat.ri.noko.model.ChatMessage
import cat.ri.noko.model.ChatSessionMeta
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
import cat.ri.noko.model.api.ChatRequest
import cat.ri.noko.ui.ChatAction
import cat.ri.noko.ui.util.parseMarkdown
import cat.ri.noko.ui.util.rememberNokoHaptics
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import java.util.UUID


private fun stripTrailingDelimiters(text: String): String {
    val trailing = listOf("***", "**", "~~", "*", "`")
    for (d in trailing) {
        if (text.endsWith(d)) return text.dropLast(d.length)
    }
    return text
}

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
    var input by remember { mutableStateOf(TextFieldValue()) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = rememberNokoHaptics()
    val imeVisible = WindowInsets.isImeVisible


    var currentChatId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var isSecretChat by remember { mutableStateOf(false) }


    val allEntries by SettingsManager.allEntries.collectAsState(initial = emptyList())
    val personas = remember(allEntries) { allEntries.filter { it.type == PersonaType.PERSONA } }
    val characters = remember(allEntries) { allEntries.filter { it.type == PersonaType.CHARACTER } }

    val selectedPersonaId by SettingsManager.selectedPersonaId.collectAsState(initial = null)
    val selectedCharacterId by SettingsManager.selectedCharacterId.collectAsState(initial = null)

    val activePersona = remember(allEntries, selectedPersonaId) {
        selectedPersonaId?.let { id -> allEntries.find { it.id == id } }
    }
    val activeCharacter = remember(allEntries, selectedCharacterId) {
        selectedCharacterId?.let { id -> allEntries.find { it.id == id } }
    }

    var showCharacterPicker by remember { mutableStateOf(false) }
    var showPersonaPicker by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var hasStreamedContent by remember { mutableStateOf(false) }
    var streamJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val apiKey by SettingsManager.apiKey.collectAsState(initial = "")
    val modelId by SettingsManager.selectedModelId.collectAsState(initial = "")
    val presets by SettingsManager.promptPresets.collectAsState(initial = listOf(defaultPromptPreset()))
    val selectedPresetId by SettingsManager.selectedPresetId.collectAsState(initial = "default")
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
        val preview = "$senderName: ${lastMsg.content.take(100).replace('\n', ' ')}"
        val meta = ChatSessionMeta(
            id = currentChatId,
            characterId = character.id,
            characterName = character.name,
            characterAvatarFileName = character.avatarFileName,
            lastMessagePreview = preview,
            lastMessageRole = lastMsg.role.name,
            updatedAt = System.currentTimeMillis(),
        )
        scope.launch {
            ChatStorage.saveChat(
                chatId = currentChatId,
                messages = messages.toList(),
                meta = meta,
            )
        }
    }


    fun startStreaming() {
        if (apiKey.isBlank() || modelId.isBlank()) return
        isGenerating = true
        hasStreamedContent = false
        val assistantIdx = messages.size
        messages.add(ChatMessage(
            role = ChatMessage.Role.ASSISTANT,
            content = "",
            senderName = activeCharacter?.name,
            senderAvatarFileName = activeCharacter?.avatarFileName,
        ))

        streamJob = scope.launch {
            val buffer = StringBuilder()
            try {
                OpenRouterClient.configure(apiKey)
                val apiMessages = PromptBuilder.buildMessages(
                    preset = activePreset,
                    persona = activePersona,
                    character = activeCharacter,
                    chatMessages = messages.dropLast(1),
                )
                val request = ChatRequest(
                    model = modelId,
                    messages = apiMessages,
                    stream = true,
                )
                var lastUiUpdate = 0L
                var wordCount = 0
                OpenRouterClient.streamChat(request).collect { token ->
                    buffer.append(token)
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

                messages[assistantIdx] = messages[assistantIdx].copy(
                    content = buffer.toString(),
                )
                haptics.confirm()
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (assistantIdx < messages.size) {
                    messages[assistantIdx] = messages[assistantIdx].copy(
                        content = buffer.toString(),
                        stoppedByUser = true,
                    )
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
                            content = activeCharacter.greetingMessage,
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
                    content = activeCharacter.greetingMessage,
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
                        modifier = Modifier.clickable {
                            haptics.tap()
                            showCharacterPicker = true
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SmallAvatar(
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
                                content = activeCharacter.greetingMessage,
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
                    val charName = activeCharacter?.name ?: "Assistant"
                    TypingIndicator(characterName = charName)
                }
            }

            items(messages.asReversed(), key = { it.id }) { message ->

                if (isGenerating && message == messages.lastOrNull()
                    && message.role == ChatMessage.Role.ASSISTANT && message.content.isBlank()
                ) return@items

                val isStreamingThis = isGenerating
                    && message == messages.lastOrNull()
                    && message.role == ChatMessage.Role.ASSISTANT
                    && message.content.isNotBlank()

                MessageBubble(
                    message = message,
                    persona = activePersona,
                    character = activeCharacter,
                    isStreaming = isStreamingThis,
                    isGenerating = isGenerating,
                    onRegenerate = if (message.role == ChatMessage.Role.ASSISTANT && !message.isGreeting) {
                        {
                            val idx = messages.indexOfFirst { it.id == message.id }
                            if (idx >= 0) {

                                messages.removeAt(idx)
                                startStreaming()
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


        AnimatedVisibility(
            visible = imeVisible,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(100)),
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
                    .clickable {
                        haptics.tap()
                        showPersonaPicker = true
                    },
            ) {
                SmallAvatar(
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
                    .weight(1f),
                placeholder = { Text(placeholder) },
                singleLine = false,
                maxLines = 4,
                shape = MaterialTheme.shapes.extraLarge,
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
            selectedId = selectedCharacterId,
            onSelect = { entry ->
                haptics.confirm()
                scope.launch { SettingsManager.setSelectedCharacterId(entry.id) }
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

@Composable
internal fun SmallAvatar(
    entry: PersonaEntry?,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    size: Int,
) {
    SmallAvatar(
        name = entry?.name,
        avatarFileName = entry?.avatarFileName,
        fallbackIcon = fallbackIcon,
        size = size,
    )
}

@Composable
internal fun SmallAvatar(
    name: String?,
    avatarFileName: String?,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    size: Int,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarFileName != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(AvatarStorage.getFile(context, avatarFileName))
                    .build(),
                contentDescription = name,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(size.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        fallbackIcon,
                        contentDescription = null,
                        modifier = Modifier.size((size / 2).dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TypingIndicator(characterName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        LoadingIndicator(modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$characterName is typing…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    persona: PersonaEntry?,
    character: PersonaEntry?,
    isStreaming: Boolean = false,
    isGenerating: Boolean = false,
    onRegenerate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onRollback: (() -> Unit)? = null,
) {
    if (message.isGreeting) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = parseMarkdown(message.content),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    val isUser = message.role == ChatMessage.Role.USER
    val name = message.senderName
        ?: if (isUser) persona?.name ?: "You"
        else character?.name ?: "Assistant"
    val avatarFileName = message.senderAvatarFileName
        ?: if (isUser) persona?.avatarFileName
        else character?.avatarFileName
    val canInteract = !isStreaming && !isGenerating
    var showActions by remember { mutableStateOf(false) }


    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(message.stoppedByUser) {
        if (message.stoppedByUser) {
            repeat(3) {
                shakeOffset.animateTo(6f, tween(40))
                shakeOffset.animateTo(-6f, tween(40))
            }
            shakeOffset.animateTo(0f, tween(40))
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.offset(x = shakeOffset.value.dp),
        ) {
            if (!isUser) {
                SmallAvatar(
                    name = name,
                    avatarFileName = avatarFileName,
                    fallbackIcon = Icons.Filled.SmartToy,
                    size = 32,
                )
                Spacer(Modifier.width(8.dp))
            }

            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 280.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh,
                            )
                        )
                        .then(if (canInteract) Modifier.clickable { showActions = !showActions } else Modifier),
                ) {

                    var wasStreaming by remember { mutableStateOf(false) }
                    if (isStreaming) wasStreaming = true

                    if (wasStreaming) {
                        StreamingText(
                            text = message.content,
                            isStreaming = isStreaming,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    } else {
                        Text(
                            text = parseMarkdown(message.content),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                AnimatedVisibility(
                    visible = showActions && canInteract,
                    enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                    exit = fadeOut(tween(100)) + shrinkVertically(tween(100)),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        if (!isUser && onRegenerate != null) {
                            AssistChip(
                                onClick = { showActions = false; onRegenerate() },
                                label = { Text("Regenerate", style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                        if (onEdit != null) {
                            AssistChip(
                                onClick = { showActions = false; onEdit() },
                                label = { Text("Edit", style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                        if (onRollback != null) {
                            AssistChip(
                                onClick = { showActions = false; onRollback() },
                                label = { Text("Rollback", style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
                if (message.stoppedByUser) {
                    Text(
                        text = "Stopped by you",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }

            if (isUser) {
                Spacer(Modifier.width(8.dp))
                SmallAvatar(
                    name = name,
                    avatarFileName = avatarFileName,
                    fallbackIcon = Icons.Filled.Person,
                    size = 32,
                )
            }
        }
    }
}


@Composable
private fun StreamingText(
    text: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle,
) {
    val targetText by rememberUpdatedState(text)
    var displayed by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        var lastFrameMs = 0L
        while (true) {
            withFrameMillis { frameMs ->
                val target = targetText
                if (displayed.length < target.length) {
                    val behind = target.length - displayed.length


                    val dt = if (lastFrameMs == 0L) 16f else (frameMs - lastFrameMs).toFloat()
                    val scale = dt / 16f
                    val baseChunk = when {
                        behind > 200 -> 30
                        behind > 100 -> 15
                        behind > 50 -> 8
                        behind > 20 -> 4
                        else -> 1
                    }
                    val chunk = (baseChunk * scale).toInt().coerceAtLeast(1)
                    displayed = target.substring(0, (displayed.length + chunk).coerceAtMost(target.length))
                }
                lastFrameMs = frameMs
            }
        }
    }

    if (displayed.length > text.length) displayed = text


    val renderText = if (isStreaming && displayed.length < text.length) {
        stripTrailingDelimiters(displayed)
    } else {
        displayed
    }

    Text(
        text = parseMarkdown(renderText),
        modifier = modifier,
        style = style,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonaPickerSheet(
    title: String,
    entries: List<PersonaEntry>,
    selectedId: String?,
    onSelect: (PersonaEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No entries yet. Create one in Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                entries.forEach { entry ->
                    val isSelected = entry.id == selectedId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(entry) },
                        color = if (isSelected)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SmallAvatar(
                                entry = entry,
                                fallbackIcon = Icons.Filled.Person,
                                size = 40,
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (entry.description.isNotBlank()) {
                                    Text(
                                        entry.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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
