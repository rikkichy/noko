package cat.ri.noko.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import cat.ri.noko.ui.components.CountdownDeleteDialog
import cat.ri.noko.ui.components.ExportPassphraseDialog
import cat.ri.noko.ui.components.NokoAvatar
import cat.ri.noko.ui.components.NokoSearchField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import cat.ri.noko.core.AvatarStorage
import cat.ri.noko.core.CharacterCodec
import cat.ri.noko.core.ChatStorage
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.ui.theme.nokoTopAppBarColors
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
import cat.ri.noko.ui.util.rememberNokoHaptics
import cat.ri.noko.ui.util.rememberSelectionMode
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PersonaListScreen(
    type: PersonaType,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
    onImport: ((Uri) -> Unit)? = null,
) {
    val title = if (type == PersonaType.PERSONA) "Personas" else "Characters"
    val entries by (if (type == PersonaType.PERSONA) SettingsManager.personas else SettingsManager.characters)
        .collectAsState(initial = SettingsManager.getEntries(type))
    val biometricAuth by SettingsManager.biometricAuth.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    val selection = rememberSelectionMode()
    var deleteTarget by remember { mutableStateOf<PersonaEntry?>(null) }
    var bulkDeleteTargets by remember { mutableStateOf<List<PersonaEntry>>(emptyList()) }
    var exportTargets by remember { mutableStateOf<List<PersonaEntry>>(emptyList()) }

    BackHandler(enabled = selection.isActive) { selection.clear() }
    var showPassphraseDialog by remember { mutableStateOf(false) }
    var exportPassphrase by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onImport?.invoke(uri)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null && exportTargets.isNotEmpty() && exportPassphrase.isNotBlank()) {
            val passChars = exportPassphrase.toCharArray()
            exportPassphrase = ""
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        CharacterCodec.exportToNokc(context, exportTargets, passChars, uri)
                    }
                } finally {
                    passChars.fill('\u0000')
                }
                haptics.confirm()
                exportTargets = emptyList()
            }
        }
    }

    fun startExport(targets: List<PersonaEntry>) {
        exportTargets = targets
        showPassphraseDialog = true
    }

    fun requestExportWithBiometric(targets: List<PersonaEntry>) {
        if (!biometricAuth) {
            startExport(targets)
            return
        }
        val activity = context as FragmentActivity
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                startExport(targets)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                haptics.reject()
            }

            override fun onAuthenticationFailed() {
                haptics.reject()
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (targets.size == 1) "Export character" else "Export ${targets.size} characters")
            .setSubtitle("Verify your identity to export")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (selection.isActive) "${selection.selectedIds.size} selected" else title)
                },
                navigationIcon = {
                    if (selection.isActive) {
                        IconButton(onClick = { selection.clear() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                        }
                    } else {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = nokoTopAppBarColors(),
            )
        },
        floatingActionButton = {
            if (!selection.isActive) {
                FloatingActionButton(
                    onClick = {
                        haptics.tap()
                        onCreate()
                    },
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (type == PersonaType.CHARACTER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (selection.isActive) {
                        FilledTonalButton(
                            onClick = {
                                haptics.tap()
                                val selected = entries.filter { selection.isSelected(it.id) }
                                requestExportWithBiometric(selected)
                            },
                        ) {
                            Icon(
                                Icons.Rounded.IosShare,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Export ${selection.selectedIds.size}")
                        }
                        FilledTonalButton(
                            onClick = {
                                haptics.tap()
                                bulkDeleteTargets = entries.filter { selection.isSelected(it.id) }
                            },
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Delete ${selection.selectedIds.size}", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        if (onImport != null) {
                            FilledTonalButton(
                                onClick = {
                                    haptics.tap()
                                    SettingsManager.suppressBiometricRelock = true
                                    importLauncher.launch(
                                        arrayOf("image/png", "application/json", "application/octet-stream"),
                                    )
                                },
                            ) {
                                Icon(
                                    Icons.Rounded.FileOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Import")
                            }
                        }
                        if (entries.size > 1) {
                            FilledTonalButton(
                                onClick = {
                                    haptics.tap()
                                    requestExportWithBiometric(entries)
                                },
                            ) {
                                Icon(
                                    Icons.Rounded.IosShare,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Export all")
                            }
                            FilledTonalButton(
                                onClick = {
                                    haptics.tap()
                                    bulkDeleteTargets = entries
                                },
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Delete all", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                if (entries.size > 5 && !selection.isActive) {
                    NokoSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search characters..",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                        focusManager = focusManager,
                    )
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "No ${title.lowercase()} yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                val filteredEntries = remember(entries, searchQuery) {
                    if (searchQuery.isBlank()) entries
                    else entries.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        val isSelected = selection.isSelected(entry.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (selection.isActive) {
                                            selection.toggle(entry.id)
                                        } else {
                                            haptics.tap()
                                            onEdit(entry.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (type == PersonaType.CHARACTER) {
                                            haptics.tap()
                                            selection.toggle(entry.id)
                                        }
                                    },
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (selection.isActive) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selection.toggle(entry.id)
                                        },
                                    )
                                } else {
                                    NokoAvatar(
                                        entry = entry,
                                        fallbackIcon = Icons.Filled.Person,
                                        size = 48,
                                        hasSurface = false,
                                    )
                                }
                                Spacer(Modifier.size(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, style = MaterialTheme.typography.titleSmall)
                                    if (entry.description.isNotBlank()) {
                                        Text(
                                            entry.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                if (type == PersonaType.PERSONA) {
                                    IconButton(
                                        onClick = {
                                            haptics.tap()
                                            deleteTarget = entry
                                        },
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
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

    deleteTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${entry.name}?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.reject()
                        scope.launch {
                            entry.avatarFileName?.let { AvatarStorage.delete(context, it) }
                            ChatStorage.deleteChatsForCharacter(entry.id)
                            SettingsManager.deleteEntry(entry.id)
                        }
                        deleteTarget = null
                    },
                ) {
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

    if (bulkDeleteTargets.isNotEmpty()) {
        val count = bulkDeleteTargets.size
        val isAll = count == entries.size && count >= 5
        val recentChats by ChatStorage.recentChats.collectAsState()

        CountdownDeleteDialog(
            showCountdown = isAll,
            title = { Text(if (isAll) "Woah..hold on." else "Delete $count ${if (count == 1) "character" else "characters"}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isAll) {
                        Text(buildAnnotatedString {
                            append("Are you really sure about that?\n")
                            append("You're about to\n")
                            withStyle(SpanStyle(color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)) {
                                append("DELETE ALL $count CHARACTERS")
                            }
                            append("\nyou've collected and created so far.\n")
                            append("This cannot be undone.")
                        })
                        val chatCounts = remember(recentChats, bulkDeleteTargets) {
                            bulkDeleteTargets.associateWith { entry ->
                                recentChats.count { it.characterId == entry.id }
                            }
                        }
                        val preview = bulkDeleteTargets.sortedByDescending { chatCounts[it] ?: 0 }.take(2)
                        if (preview.any { (chatCounts[it] ?: 0) > 0 }) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                preview.forEach { entry ->
                                    val chats = chatCounts[entry] ?: 0
                                    if (chats > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            NokoAvatar(
                                                entry = entry,
                                                fallbackIcon = Icons.Filled.Person,
                                                size = 32,
                                                hasSurface = false,
                                            )
                                            Column {
                                                Text(entry.name, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    "$chats ${if (chats == 1) "chat" else "chats"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text("This cannot be undone.")
                    }
                }
            },
            onConfirm = {
                haptics.reject()
                val targets = bulkDeleteTargets
                bulkDeleteTargets = emptyList()
                selection.clear()
                scope.launch {
                    targets.forEach { entry ->
                        entry.avatarFileName?.let { AvatarStorage.delete(context, it) }
                        ChatStorage.deleteChatsForCharacter(entry.id)
                        SettingsManager.deleteEntry(entry.id)
                    }
                }
            },
            onDismiss = { bulkDeleteTargets = emptyList() },
        )
    }

    if (showPassphraseDialog && exportTargets.isNotEmpty()) {
        ExportPassphraseDialog(
            exportCount = exportTargets.size,
            onConfirm = { passphrase ->
                exportPassphrase = passphrase
                showPassphraseDialog = false
                SettingsManager.suppressBiometricRelock = true
                val safeName = if (exportTargets.size == 1) {
                    exportTargets.first().name
                        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        .take(50)
                } else {
                    "noko_characters_${exportTargets.size}"
                }
                exportLauncher.launch("$safeName.nokc")
            },
            onDismiss = {
                showPassphraseDialog = false
                exportTargets = emptyList()
            },
        )
    }
}
