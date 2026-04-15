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
import cat.ri.noko.ui.theme.NokoFieldShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import cat.ri.noko.ui.components.CountdownDeleteDialog
import cat.ri.noko.ui.components.DidYouKnowCard
import cat.ri.noko.ui.components.NokoAvatar
import cat.ri.noko.ui.components.NokoSearchField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import cat.ri.noko.ui.theme.nokoTopAppBarColors
import cat.ri.noko.ui.util.rememberNokoHaptics
import cat.ri.noko.ui.util.rememberSelectionMode
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.util.Calendar
import kotlinx.coroutines.launch

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
    val selection = rememberSelectionMode()
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = selection.isActive) { selection.clear() }

    LaunchedEffect(refreshKey) {
        searchQuery = ""
        selectedCharacterId = null
        selection.clear()
        focusManager.clearFocus()
    }

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
                    if (selection.isActive) {
                        Text("${selection.selectedIds.size} selected")
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
                    if (selection.isActive) {
                        IconButton(onClick = { selection.clear() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                        }
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            AnimatedVisibility(visible = !selection.isActive) {
                Column {
                    DidYouKnowCard(recentChats, entryMap, refreshKey)
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (selection.isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            haptics.tap()
                            val ids = selection.selectedIds
                            selection.clear()
                            scope.launch { ids.forEach { ChatStorage.togglePin(it) } }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.PushPin, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pin ${selection.selectedIds.size}")
                    }
                    FilledTonalButton(
                        onClick = {
                            haptics.tap()
                            showBulkDeleteDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Delete ${selection.selectedIds.size}",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
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

                if (!selection.isActive) {
                    NokoSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search chats..",
                        modifier = Modifier.fillMaxWidth(),
                        focusManager = focusManager,
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
                            val isSelected = selection.isSelected(meta.id)
                            Column {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (selection.isActive) {
                                                    selection.toggle(meta.id)
                                                } else {
                                                    onOpenRecentChat(meta)
                                                }
                                            },
                                            onLongClick = {
                                                haptics.tap()
                                                if (selection.isActive) {
                                                    selection.toggle(meta.id)
                                                } else {
                                                    selection.select(meta.id)
                                                }
                                            },
                                        ),
                                    shape = NokoFieldShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerLow,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (selection.isActive) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    selection.toggle(meta.id)
                                                },
                                            )
                                        } else {
                                            NokoAvatar(
                                                name = meta.characterName,
                                                avatarFileName = entryMap[meta.characterId]?.avatarFileName
                                                    ?: meta.characterAvatarFileName,
                                                fallbackIcon = Icons.Filled.SmartToy,
                                                size = 48,
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
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBulkDeleteDialog) {
        val count = selection.selectedIds.size
        CountdownDeleteDialog(
            showCountdown = count >= 5,
            title = { Text("Delete $count ${if (count == 1) "chat" else "chats"}?") },
            text = { Text("This cannot be undone.") },
            onConfirm = {
                haptics.reject()
                val ids = selection.selectedIds
                showBulkDeleteDialog = false
                selection.clear()
                scope.launch { ChatStorage.deleteChats(ids) }
            },
            onDismiss = { showBulkDeleteDialog = false },
        )
    }
}


