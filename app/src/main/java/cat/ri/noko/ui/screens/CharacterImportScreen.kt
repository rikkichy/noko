package cat.ri.noko.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.AvatarStorage
import cat.ri.noko.core.CharacterCodec
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()

    var state by remember { mutableStateOf(ImportState.DETECTING) }
    var format by remember { mutableStateOf(CharacterCodec.CharacterFormat.UNKNOWN) }
    var result by remember { mutableStateOf<CharacterCodec.ImportResult?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var passphraseError by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    fun doImport(importResult: CharacterCodec.ImportResult) {
        when (importResult) {
            is CharacterCodec.ImportResult.Success -> {
                result = importResult
                state = ImportState.PREVIEW
            }
            is CharacterCodec.ImportResult.Error -> {
                errorMessage = importResult.message
                state = ImportState.ERROR
            }
        }
    }

    LaunchedEffect(uri) {
        withContext(Dispatchers.IO) {
            format = CharacterCodec.detectFormat(context, uri)
            when (format) {
                CharacterCodec.CharacterFormat.TAVERN_PNG -> {
                    doImport(CharacterCodec.importFromPng(context, uri))
                }
                CharacterCodec.CharacterFormat.CHARACTER_AI_JSON -> {
                    doImport(CharacterCodec.importFromCharacterAiJson(context, uri))
                }
                CharacterCodec.CharacterFormat.NOKC -> {
                    state = ImportState.PASSPHRASE
                }
                CharacterCodec.CharacterFormat.UNKNOWN -> {
                    errorMessage = "Unrecognized file format"
                    state = ImportState.ERROR
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state) {
                            ImportState.DETECTING -> "Detecting..."
                            ImportState.PASSPHRASE -> "Decrypt character"
                            ImportState.PREVIEW, ImportState.IMPORTING -> "Import character"
                            ImportState.ERROR -> "Import failed"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        when (state) {
            ImportState.DETECTING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            ImportState.PASSPHRASE -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Enter the passphrase to decrypt this character.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = {
                            passphrase = it
                            passphraseError = null
                        },
                        label = { Text("Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = passphraseError != null,
                        supportingText = passphraseError?.let { err -> { Text(err) } },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(shakeOffset.value.toInt(), 0) },
                    )

                    Button(
                        onClick = {
                            if (passphrase.isBlank()) {
                                passphraseError = "Enter a passphrase"
                                scope.launch {
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
                                }
                                return@Button
                            }
                            state = ImportState.DETECTING
                            scope.launch {
                                val importResult = withContext(Dispatchers.IO) {
                                    CharacterCodec.importFromNokc(context, uri, passphrase)
                                }
                                if (importResult is CharacterCodec.ImportResult.Error &&
                                    importResult.message == "Wrong passphrase"
                                ) {
                                    state = ImportState.PASSPHRASE
                                    passphraseError = "Wrong passphrase"
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
                                } else {
                                    doImport(importResult)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text("Decrypt")
                    }
                }
            }

            ImportState.PREVIEW -> {
                val success = result as? CharacterCodec.ImportResult.Success ?: return@Scaffold

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (success.avatarBytes != null) {
                        val bitmap = remember(success.avatarBytes) {
                            BitmapFactory.decodeByteArray(
                                success.avatarBytes, 0, success.avatarBytes.size,
                            )
                        }
                        if (bitmap != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(bitmap)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text(
                        success.name,
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    val formatLabel = when (format) {
                        CharacterCodec.CharacterFormat.TAVERN_PNG -> "TavernAI Card"
                        CharacterCodec.CharacterFormat.CHARACTER_AI_JSON -> "Character.AI"
                        CharacterCodec.CharacterFormat.NOKC -> "Noko Encrypted"
                        else -> "Unknown"
                    }
                    Text(
                        formatLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    if (success.description.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Description",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    success.description.take(500) +
                                        if (success.description.length > 500) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    if (!success.greetingMessage.isNullOrBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Greeting",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    success.greetingMessage.take(300) +
                                        if (success.greetingMessage.length > 300) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.size(8.dp))

                    Button(
                        onClick = {
                            state = ImportState.IMPORTING
                            scope.launch {
                                val id = UUID.randomUUID().toString()
                                val avatarFileName = success.avatarBytes?.let { bytes ->
                                    withContext(Dispatchers.IO) {
                                        AvatarStorage.saveBytes(context, bytes, id)
                                    }
                                }
                                val entry = PersonaEntry(
                                    id = id,
                                    type = PersonaType.CHARACTER,
                                    name = success.name,
                                    description = success.description,
                                    greetingMessage = success.greetingMessage,
                                    avatarFileName = avatarFileName,
                                )
                                SettingsManager.saveEntry(entry)
                                haptics.confirm()
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Import")
                    }
                }
            }

            ImportState.IMPORTING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.size(16.dp))
                        Text("Importing...")
                    }
                }
            }

            ImportState.ERROR -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
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
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Text("Go back")
                        }
                    }
                }
            }
        }
    }
}
