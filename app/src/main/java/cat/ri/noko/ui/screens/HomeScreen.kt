package cat.ri.noko.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.AvatarStorage
import cat.ri.noko.core.ChatStorage
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.model.ChatSessionMeta
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.ui.util.rememberNokoHaptics
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.util.Calendar
import kotlinx.coroutines.launch

private val triviaFacts = listOf(
    "Internet RP appeared in the early 1990s, where people used to chat with each other. Things have changed.. yeah.",
    "If your response is long and structured, AI is less prone to impersonate you.",
    "Adding a detailed persona description helps the AI understand your writing style.",
    "Using *action blocks* in your messages helps AI distinguish narration from dialogue.",
    "NokoGuard detects AI hallucinations really well.",
    "Secret chats aren't saved to history. Just saying..",
    "Shorter system prompts often lead to more creative AI responses.",
    "The term 'roleplay' in online contexts dates back to IRC channels in the early 90s.",
)

private fun timeGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetings = when {
        hour in 5..11 -> listOf(
            "Good morning~",
            "Rise and roleplay",
            "We missed you..",
        )
        hour in 12..16 -> listOf(
            "Good afternoon~",
            "Adventures await",
            "New story arc?",
        )
        hour in 17..20 -> listOf(
            "Evening vibes~",
            "Cozy hours",
            "Golden stories",
        )
        else -> listOf(
            "Late night~",
            "Can't sleep?",
            "*sleeps*",
            "Shh..",
        )
    }
    return greetings.random()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    refreshKey: Int = 0,
    onNewChat: () -> Unit,
    onNewSecretChat: () -> Unit,
    onOpenRecentChat: (ChatSessionMeta) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()
    val greeting = remember(refreshKey) { timeGreeting() }
    val recentChats by ChatStorage.recentChats.collectAsState()
    val allEntries by SettingsManager.allEntries.collectAsState(initial = emptyList())
    val entryMap = remember(allEntries) { allEntries.associateBy { it.id } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCharacterId by remember { mutableStateOf<String?>(null) }
    var expandedChatId by remember { mutableStateOf<String?>(null) }
    var selectedChatIds by remember { mutableStateOf(emptySet<String>()) }
    val inSelectionMode = selectedChatIds.isNotEmpty()
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = inSelectionMode) { selectedChatIds = emptySet() }

    LaunchedEffect(refreshKey) {
        searchQuery = ""
        selectedCharacterId = null
        expandedChatId = null
        selectedChatIds = emptySet()
        focusManager.clearFocus()
    }
    var deleteTarget by remember { mutableStateOf<ChatSessionMeta?>(null) }

    val uniqueCharacters = remember(recentChats) {
        recentChats.distinctBy { it.characterId }.map {
            Triple(it.characterId, it.characterName, it.characterAvatarFileName)
        }
    }

    val isFiltering = searchQuery.isNotBlank() || selectedCharacterId != null
    val filteredChats = remember(recentChats, searchQuery, selectedCharacterId) {
        val base = if (!isFiltering) recentChats
        else recentChats.filter { meta ->
            val matchesQuery = searchQuery.isBlank() ||
                meta.characterName.contains(searchQuery, ignoreCase = true) ||
                meta.lastMessagePreview.contains(searchQuery, ignoreCase = true)
            val matchesCharacter = selectedCharacterId == null ||
                meta.characterId == selectedCharacterId
            matchesQuery && matchesCharacter
        }
        base.sortedWith(compareByDescending<ChatSessionMeta> { it.pinned }.thenByDescending { it.updatedAt })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (inSelectionMode) {
                        Text("${selectedChatIds.size} selected")
                    } else {
                        Column {
                            Text(
                                "Noko",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                greeting,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (inSelectionMode) {
                        IconButton(onClick = { selectedChatIds = emptySet() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                        }
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            AnimatedVisibility(visible = !inSelectionMode) {
                Column {
                    DidYouKnowCard(recentChats, entryMap, refreshKey)
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (inSelectionMode) {
                FilledTonalButton(
                    onClick = {
                        haptics.tap()
                        showBulkDeleteDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Delete ${selectedChatIds.size}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = onNewChat,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("New chat")
                    }
                    OutlinedButton(
                        onClick = onNewSecretChat,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Secret chat")
                    }
                }
            }

            if (recentChats.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))

                if (!inSelectionMode) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search chats..") },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                }) {
                                    Icon(
                                        Icons.Filled.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                if (uniqueCharacters.size >= 2) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(uniqueCharacters, key = { it.first }) { (charId, charName, metaAvatar) ->
                            val isSelected = selectedCharacterId == charId
                            val liveAvatar = entryMap[charId]?.avatarFileName ?: metaAvatar
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(56.dp)
                                    .clickable {
                                        selectedCharacterId = if (isSelected) null else charId
                                    },
                            ) {
                                val borderMod = if (isSelected) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else Modifier
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .then(borderMod),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (liveAvatar != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(AvatarStorage.getFile(context, liveAvatar))
                                                .build(),
                                            contentDescription = charName,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            modifier = Modifier.size(40.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Filled.SmartToy,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    charName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    if (isFiltering) "${filteredChats.size} result${if (filteredChats.size != 1) "s" else ""}"
                    else "Recent",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                if (filteredChats.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No chats found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(filteredChats, key = { it.id }) { meta ->
                            val isSelected = meta.id in selectedChatIds
                            Column {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (inSelectionMode) {
                                                    selectedChatIds = if (isSelected) selectedChatIds - meta.id
                                                        else selectedChatIds + meta.id
                                                } else if (expandedChatId == meta.id) {
                                                    expandedChatId = null
                                                } else {
                                                    onOpenRecentChat(meta)
                                                }
                                            },
                                            onLongClick = {
                                                haptics.tap()
                                                if (inSelectionMode) {
                                                    selectedChatIds = if (isSelected) selectedChatIds - meta.id
                                                        else selectedChatIds + meta.id
                                                } else {
                                                    selectedChatIds = selectedChatIds + meta.id
                                                    expandedChatId = null
                                                }
                                            },
                                        ),
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerLow,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (inSelectionMode) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    selectedChatIds = if (isSelected) selectedChatIds - meta.id
                                                        else selectedChatIds + meta.id
                                                },
                                            )
                                        } else {
                                            RecentChatAvatar(
                                                avatarFileName = entryMap[meta.characterId]?.avatarFileName
                                                    ?: meta.characterAvatarFileName,
                                                name = meta.characterName,
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (meta.pinned) {
                                                    Icon(
                                                        Icons.Filled.PushPin,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.primary,
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                }
                                                Text(
                                                    meta.characterName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            Text(
                                                meta.lastMessagePreview
                                                    .replace(Regex("(?<!\\*)\\*(?!\\*)((?:(?!\\*).)+?)\\*(?!\\*)"), "$1"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                                if (!inSelectionMode && expandedChatId == meta.id) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        AssistChip(
                                            onClick = {
                                                haptics.tap()
                                                scope.launch { ChatStorage.togglePin(meta.id) }
                                                expandedChatId = null
                                            },
                                            label = { Text(if (meta.pinned) "Unpin" else "Pin") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.PushPin,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            },
                                        )
                                        AssistChip(
                                            onClick = {
                                                haptics.tap()
                                                deleteTarget = meta
                                            },
                                            label = { Text("Delete") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                labelColor = MaterialTheme.colorScheme.error,
                                                leadingIconContentColor = MaterialTheme.colorScheme.error,
                                            ),
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

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete chat?") },
            text = {
                Text("Chat with ${deleteTarget!!.characterName} will be permanently deleted.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = deleteTarget!!.id
                    deleteTarget = null
                    expandedChatId = null
                    haptics.reject()
                    scope.launch { ChatStorage.deleteChat(id) }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showBulkDeleteDialog) {
        val count = selectedChatIds.size
        val isLarge = count >= 5
        var deleteCountdown by remember { mutableStateOf(if (isLarge) 4 else 0) }

        LaunchedEffect(Unit) {
            if (isLarge) {
                while (deleteCountdown > 0) {
                    kotlinx.coroutines.delay(1000)
                    deleteCountdown--
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("Delete $count ${if (count == 1) "chat" else "chats"}?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    enabled = deleteCountdown == 0,
                    onClick = {
                        haptics.reject()
                        val ids = selectedChatIds
                        showBulkDeleteDialog = false
                        selectedChatIds = emptySet()
                        scope.launch { ChatStorage.deleteChats(ids) }
                    },
                ) {
                    Text(
                        if (deleteCountdown > 0) "Delete ($deleteCountdown)" else "Delete",
                        color = if (deleteCountdown == 0) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private sealed class CardItem {
    data class Fact(val text: String) : CardItem()
    data class CharacterStat(val name: String, val count: Int, val avatarFileName: String?) : CardItem()
    data class PersonaStat(val name: String, val messageCount: Int, val avatarFileName: String?) : CardItem()
}

@Composable
private fun DidYouKnowCard(
    recentChats: List<ChatSessionMeta>,
    entryMap: Map<String, PersonaEntry>,
    refreshKey: Int = 0,
) {
    val titles = listOf("Did you know?", "Interesting fact..", "Fun fact!", "By the way..", "Hmm..")

    val items = remember(recentChats, entryMap, refreshKey) {
        val pool = mutableListOf<CardItem>()
        pool.addAll(triviaFacts.map { CardItem.Fact(it) })

        val charGroups = recentChats
            .filter { it.messageCount > 0 }
            .groupBy { it.characterId }
        for ((charId, chats) in charGroups) {
            val name = chats.first().characterName
            val avatar = entryMap[charId]?.avatarFileName
                ?: chats.firstOrNull { it.characterAvatarFileName != null }?.characterAvatarFileName
            pool.add(CardItem.CharacterStat(name, chats.size, avatar))
        }

        val personaGroups = recentChats
            .filter { it.personaName != null && it.messageCount > 0 }
            .groupBy { it.personaName!! }
        for ((name, chats) in personaGroups) {
            val personaEntry = entryMap.values.find { it.name == name }
            val avatar = personaEntry?.avatarFileName
                ?: chats.firstOrNull { it.personaAvatarFileName != null }?.personaAvatarFileName
            pool.add(CardItem.PersonaStat(name, chats.sumOf { it.messageCount }, avatar))
        }

        pool.random()
    }

    val title = remember(refreshKey) { titles.random() }
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            when (items) {
                is CardItem.Fact -> {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            items.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is CardItem.CharacterStat -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (items.avatarFileName != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(AvatarStorage.getFile(context, items.avatarFileName))
                                    .build(),
                                contentDescription = items.name,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Filled.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "You have ${items.count} chat${if (items.count != 1) "s" else ""} with ${items.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is CardItem.PersonaStat -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (items.avatarFileName != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(AvatarStorage.getFile(context, items.avatarFileName))
                                    .build(),
                                contentDescription = items.name,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            "You've sent ${items.messageCount} message${if (items.messageCount != 1) "s" else ""} as ${items.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentChatAvatar(
    avatarFileName: String?,
    name: String,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(48.dp)
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
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
