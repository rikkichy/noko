package cat.ri.noko.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.Animatable
import cat.ri.noko.ui.util.shake
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import cat.ri.noko.ui.theme.NokoFieldShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cat.ri.noko.R
import cat.ri.noko.core.AvatarStorage
import cat.ri.noko.core.CharacterCodec
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
import cat.ri.noko.ui.theme.nokoTopAppBarColors
import cat.ri.noko.ui.util.rememberNokoHaptics
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private enum class ImportState { DETECTING, PASSPHRASE, PREVIEW, IMPORTING, ERROR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterImportScreen(
    uri: Uri,
    onBack: () -> Unit,
) {
    val initialTitle = stringResource(R.string.import_title_detecting)
    var importTitle by remember(initialTitle) { mutableStateOf(initialTitle) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(importTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = nokoTopAppBarColors(),
            )
        },
    ) { padding ->
        CharacterImportContent(
            uri = uri,
            onTitleChange = { importTitle = it },
            onComplete = onBack,
            onBack = onBack,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun CharacterImportContent(
    uri: Uri,
    onTitleChange: ((String) -> Unit)? = null,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()

    var state by remember { mutableStateOf(ImportState.DETECTING) }
    var format by remember { mutableStateOf(CharacterCodec.CharacterFormat.UNKNOWN) }
    var characters by remember { mutableStateOf<List<CharacterCodec.ImportedCharacter>>(emptyList()) }
    val selected = remember { mutableStateListOf<Int>() }
    var errorMessage by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var passphraseError by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    val selectedCount = selected.size
    val totalCount = characters.size

    val titleDetecting = stringResource(R.string.import_title_detecting)
    val titleDecrypt = stringResource(R.string.import_title_decrypt)
    val titleImportOne = stringResource(R.string.import_title_import_one)
    val titleImportCountTemplate = stringResource(R.string.import_title_import_count, selectedCount, totalCount)
    val titleFailed = stringResource(R.string.import_title_failed)

    val unrecognizedMsg = stringResource(R.string.import_unrecognized)
    val passphraseRequiredMsg = stringResource(R.string.import_passphrase_required)
    val wrongPassphraseMsg = stringResource(R.string.import_passphrase_wrong)

    LaunchedEffect(state, totalCount, selectedCount, titleDetecting, titleDecrypt, titleImportOne, titleImportCountTemplate, titleFailed) {
        onTitleChange?.invoke(
            when (state) {
                ImportState.DETECTING -> titleDetecting
                ImportState.PASSPHRASE -> titleDecrypt
                ImportState.PREVIEW, ImportState.IMPORTING -> {
                    if (totalCount == 1) titleImportOne
                    else titleImportCountTemplate
                }
                ImportState.ERROR -> titleFailed
            },
        )
    }

    fun handleResult(result: CharacterCodec.ImportResult) {
        when (result) {
            is CharacterCodec.ImportResult.Success -> {
                characters = result.characters
                selected.clear()
                selected.addAll(result.characters.indices)
                state = ImportState.PREVIEW
            }
            is CharacterCodec.ImportResult.Error -> {
                errorMessage = result.message
                state = ImportState.ERROR
            }
        }
    }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            format = CharacterCodec.detectFormat(context, uri)
            when (format) {
                CharacterCodec.CharacterFormat.TAVERN_PNG -> {
                    handleResult(CharacterCodec.importFromPng(context, uri))
                }
                CharacterCodec.CharacterFormat.CHARACTER_AI_JSON -> {
                    handleResult(CharacterCodec.importFromCharacterAiJson(context, uri))
                }
                CharacterCodec.CharacterFormat.NOKC -> {
                    state = ImportState.PASSPHRASE
                }
                CharacterCodec.CharacterFormat.UNKNOWN -> {
                    errorMessage = unrecognizedMsg
                    state = ImportState.ERROR
                }
            }
        }
    }

    when (state) {
        ImportState.DETECTING -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        ImportState.PASSPHRASE -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.import_passphrase_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = {
                        if (it.length <= 256) {
                            passphrase = it
                            passphraseError = null
                        }
                    },
                    label = { Text(stringResource(R.string.import_passphrase)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = passphraseError != null,
                    supportingText = passphraseError?.let { err -> { Text(err) } },
                    shape = NokoFieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(shakeOffset.value.toInt(), 0) },
                )

                Button(
                    onClick = {
                        if (passphrase.isBlank()) {
                            passphraseError = passphraseRequiredMsg
                            scope.launch {
                                haptics.reject()
                                shakeOffset.shake()
                            }
                            return@Button
                        }
                        val passChars = passphrase.toCharArray()
                        passphrase = ""
                        passphraseError = null
                        state = ImportState.DETECTING
                        scope.launch {
                            val result = try {
                                withContext(Dispatchers.IO) {
                                    CharacterCodec.importFromNokc(context, uri, passChars)
                                }
                            } finally {
                                passChars.fill('\u0000')
                            }
                            if (result is CharacterCodec.ImportResult.Error &&
                                result.message == "Wrong passphrase"
                            ) {
                                state = ImportState.PASSPHRASE
                                passphraseError = wrongPassphraseMsg
                                haptics.reject()
                                shakeOffset.shake()
                            } else {
                                handleResult(result)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = NokoFieldShape,
                ) {
                    Text(stringResource(R.string.import_decrypt))
                }
            }
        }

        ImportState.PREVIEW -> {
            Column(
                modifier = modifier.fillMaxSize(),
            ) {
                val formatLabel = stringResource(
                    when (format) {
                        CharacterCodec.CharacterFormat.TAVERN_PNG -> R.string.import_format_tavern
                        CharacterCodec.CharacterFormat.CHARACTER_AI_JSON -> R.string.import_format_cai
                        CharacterCodec.CharacterFormat.NOKC -> R.string.import_format_nokc
                        else -> R.string.import_format_unknown
                    }
                )
                Text(
                    formatLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(characters) { index, char ->
                        val isSelected = index in selected
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            shape = NokoFieldShape,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (totalCount > 1) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selected.add(index) else selected.remove(index)
                                        },
                                    )
                                }
                                if (char.avatarBytes != null) {
                                    val bitmap = remember(char.avatarBytes) {
                                        BitmapFactory.decodeByteArray(
                                            char.avatarBytes, 0, char.avatarBytes.size,
                                        )
                                    }
                                    if (bitmap != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(bitmap)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Spacer(Modifier.size(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(char.name, style = MaterialTheme.typography.titleSmall)
                                    if (char.description.isNotBlank()) {
                                        Text(
                                            char.description.take(100) +
                                                if (char.description.length > 100) "..." else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                        )
                                    }
                                    if (!char.greetingMessage.isNullOrBlank()) {
                                        Text(
                                            stringResource(R.string.import_has_greeting),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (selected.isEmpty()) return@Button
                        state = ImportState.IMPORTING
                        scope.launch {
                            val toImport = selected.sorted().map { characters[it] }
                            withContext(Dispatchers.IO) {
                                toImport.forEach { char ->
                                    val id = UUID.randomUUID().toString()
                                    val avatarFileName = char.avatarBytes?.let { bytes ->
                                        AvatarStorage.saveBytes(context, bytes, id)
                                    }
                                    val entry = PersonaEntry(
                                        id = id,
                                        type = PersonaType.CHARACTER,
                                        name = char.name,
                                        description = char.description,
                                        personality = char.personality,
                                        scenario = char.scenario,
                                        greetingMessage = char.greetingMessage,
                                        avatarFileName = avatarFileName,
                                    )
                                    SettingsManager.saveEntry(entry)
                                }
                            }
                            haptics.confirm()
                            onComplete()
                        }
                    },
                    enabled = selected.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = NokoFieldShape,
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(androidx.compose.ui.res.pluralStringResource(R.plurals.import_button_count, selectedCount, selectedCount))
                }
            }
        }

        ImportState.IMPORTING -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.size(16.dp))
                    Text(stringResource(R.string.import_importing))
                }
            }
        }

        ImportState.ERROR -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = onBack,
                        shape = NokoFieldShape,
                    ) {
                        Text(stringResource(R.string.common_go_back))
                    }
                }
            }
        }
    }
}
