package cat.ri.noko.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.AvatarStorage
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
import cat.ri.noko.ui.components.ImageCropOverlay
import cat.ri.noko.ui.components.PersonaFormFields
import cat.ri.noko.ui.util.rememberNokoHaptics
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaEditScreen(
    type: PersonaType,
    editId: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()
    val allEntries by SettingsManager.allEntries.collectAsState(initial = emptyList())
    val existing = remember(allEntries, editId) {
        editId?.let { SettingsManager.getEntry(allEntries, it) }
    }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var description by remember(existing) {
        mutableStateOf(
            existing?.description ?: if (type == PersonaType.PERSONA) {
                "A brief description of your persona..."
            } else {
                "Describe the AI character's personality and traits..."
            },
        )
    }
    var greetingMessage by remember(existing) {
        mutableStateOf(existing?.greetingMessage ?: "")
    }
    var avatarFileName by remember(existing) { mutableStateOf(existing?.avatarFileName) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            pendingCropUri = uri
        }
    }

    val isNew = editId == null
    val title = if (isNew) {
        "New ${if (type == PersonaType.PERSONA) "Persona" else "Character"}"
    } else {
        "Edit ${if (type == PersonaType.PERSONA) "Persona" else "Character"}"
    }

    if (pendingCropUri != null) {
        ImageCropOverlay(
            imageUri = pendingCropUri!!,
            onCrop = { bitmap ->
                scope.launch {
                    val entryId = existing?.id ?: java.util.UUID.randomUUID().toString()
                    val fileName = AvatarStorage.save(context, bitmap, entryId)
                    avatarFileName = fileName
                    pendingCropUri = null
                }
            },
            onCancel = { pendingCropUri = null },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (name.isBlank()) return@IconButton
                            haptics.confirm()
                            scope.launch {
                                val entry = PersonaEntry(
                                    id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                                    type = type,
                                    name = name.trim(),
                                    description = description.trim(),
                                    greetingMessage = if (type == PersonaType.CHARACTER && greetingMessage.isNotBlank())
                                        greetingMessage.trim() else null,
                                    avatarFileName = avatarFileName,
                                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                )
                                SettingsManager.saveEntry(entry)
                                onBack()
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PersonaFormFields(
                type = type,
                avatarFileName = avatarFileName,
                onAvatarClick = {
                    haptics.tap()
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                name = name,
                onNameChange = { name = it },
                description = description,
                onDescriptionChange = { description = it },
                greetingMessage = greetingMessage,
                onGreetingChange = { greetingMessage = it },
            )
        }
    }
}
