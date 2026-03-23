package cat.ri.noko.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.AvatarStorage
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.core.api.OpenRouterClient
import cat.ri.noko.core.api.humanizeException
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
import cat.ri.noko.ui.components.ImageCropOverlay
import cat.ri.noko.ui.util.rememberNokoHaptics
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class OnboardingStep { ApiKey, Persona, Character }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()

    var step by remember { mutableStateOf(OnboardingStep.ApiKey) }


    var apiKeyInput by remember { mutableStateOf("") }
    var keyError by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    val keyShake = remember { Animatable(0f) }


    var personaName by remember { mutableStateOf("") }
    var personaDescription by remember { mutableStateOf("") }
    var avatarFileName by remember { mutableStateOf<String?>(null) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    val personaId = remember { java.util.UUID.randomUUID().toString() }
    var nameError by remember { mutableStateOf(false) }
    var descError by remember { mutableStateOf(false) }
    val nameShake = remember { Animatable(0f) }
    val descShake = remember { Animatable(0f) }


    var charName by remember { mutableStateOf("") }
    var charDescription by remember { mutableStateOf("") }
    var charGreeting by remember { mutableStateOf("") }
    var charAvatarFileName by remember { mutableStateOf<String?>(null) }
    val charId = remember { java.util.UUID.randomUUID().toString() }
    var charNameError by remember { mutableStateOf(false) }
    var charDescError by remember { mutableStateOf(false) }
    val charNameShake = remember { Animatable(0f) }
    val charDescShake = remember { Animatable(0f) }

    var showGreetingWarning by remember { mutableStateOf(false) }
    var cropForCharacter by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) pendingCropUri = uri }

    fun shake(animatable: Animatable<Float, *>) {
        scope.launch {
            animatable.animateTo(
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
    }

    if (pendingCropUri != null) {
        ImageCropOverlay(
            imageUri = pendingCropUri!!,
            onCrop = { bitmap ->
                scope.launch {
                    if (cropForCharacter) {
                        val fileName = AvatarStorage.save(context, bitmap, charId)
                        charAvatarFileName = fileName
                    } else {
                        val fileName = AvatarStorage.save(context, bitmap, personaId)
                        avatarFileName = fileName
                    }
                    pendingCropUri = null
                }
            },
            onCancel = { pendingCropUri = null },
        )
        return
    }

    Scaffold(
        topBar = {
            when (step) {
                OnboardingStep.ApiKey -> {
                    TopAppBar(title = { Text("Noko") })
                }
                OnboardingStep.Persona -> {
                    TopAppBar(
                        title = { Text("Create your persona") },
                        navigationIcon = {
                            IconButton(onClick = {
                                haptics.tap()
                                step = OnboardingStep.ApiKey
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                    )
                }
                OnboardingStep.Character -> {
                    TopAppBar(
                        title = { Text("Create a character") },
                        navigationIcon = {
                            IconButton(onClick = {
                                haptics.tap()
                                step = OnboardingStep.Persona
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                OnboardingStep.ApiKey -> {
                    Text(
                        "Welcome!",
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Enter your OpenRouter API key to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            if (keyError != null) keyError = null
                        },
                        label = { Text("API Key") },
                        placeholder = { Text("sk-or-v1-...") },
                        singleLine = true,
                        isError = keyError != null,
                        supportingText = if (keyError != null) {
                            { Text(keyError!!) }
                        } else null,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(keyShake.value.toInt(), 0) },
                    )

                    Button(
                        enabled = !isTesting && apiKeyInput.isNotBlank(),
                        onClick = {
                            haptics.tap()
                            isTesting = true
                            keyError = null
                            scope.launch {
                                runCatching {
                                    OpenRouterClient.configure(apiKeyInput)
                                    withContext(Dispatchers.IO) {
                                        OpenRouterClient.validateKey()
                                    }
                                }.onSuccess {
                                    SettingsManager.setApiKey(apiKeyInput)
                                    step = OnboardingStep.Persona
                                }.onFailure { e ->
                                    keyError = humanizeException(e)
                                    haptics.reject()
                                    shake(keyShake)
                                }
                                isTesting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isTesting) "Checking..." else "Next")
                    }

                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://openrouter.ai/workspaces/default/keys"),
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create OpenRouter API Key")
                    }
                }

                OnboardingStep.Persona -> {
                    Text(
                        "This is how the AI will know you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(4.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptics.tap()
                                    cropForCharacter = false
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (avatarFileName != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(AvatarStorage.getFile(context, avatarFileName!!))
                                        .build(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = "Add avatar",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap to set avatar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = personaName,
                        onValueChange = {
                            personaName = it
                            if (nameError) nameError = false
                        },
                        label = { Text("Name") },
                        placeholder = { Text("Your persona name...") },
                        singleLine = true,
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text("Name is required") }
                        } else null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(nameShake.value.toInt(), 0) },
                    )

                    OutlinedTextField(
                        value = personaDescription,
                        onValueChange = {
                            personaDescription = it
                            if (descError) descError = false
                        },
                        label = { Text("Description") },
                        placeholder = { Text("Describe your persona...") },
                        isError = descError,
                        supportingText = if (descError) {
                            { Text("Description is required") }
                        } else null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .offset { IntOffset(descShake.value.toInt(), 0) },
                        maxLines = 6,
                    )

                    Button(
                        onClick = {
                            haptics.tap()
                            val nameMissing = personaName.isBlank()
                            val descMissing = personaDescription.isBlank()

                            if (nameMissing || descMissing) {
                                haptics.reject()
                                if (nameMissing) {
                                    nameError = true
                                    shake(nameShake)
                                }
                                if (descMissing) {
                                    descError = true
                                    shake(descShake)
                                }
                                return@Button
                            }

                            scope.launch {
                                val entry = PersonaEntry(
                                    id = personaId,
                                    type = PersonaType.PERSONA,
                                    name = personaName.trim(),
                                    description = personaDescription.trim(),
                                    avatarFileName = avatarFileName,
                                )
                                SettingsManager.saveEntry(entry)
                                SettingsManager.setSelectedPersonaId(entry.id)
                                step = OnboardingStep.Character
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Next")
                    }
                }

                OnboardingStep.Character -> {
                    Text(
                        "Create a character for the AI to play.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(4.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptics.tap()
                                    cropForCharacter = true
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (charAvatarFileName != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(AvatarStorage.getFile(context, charAvatarFileName!!))
                                        .build(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(
                                    Icons.Filled.SmartToy,
                                    contentDescription = "Add avatar",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap to set avatar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = charName,
                        onValueChange = {
                            charName = it
                            if (charNameError) charNameError = false
                        },
                        label = { Text("Name") },
                        placeholder = { Text("Character name...") },
                        singleLine = true,
                        isError = charNameError,
                        supportingText = if (charNameError) {
                            { Text("Name is required") }
                        } else null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(charNameShake.value.toInt(), 0) },
                    )

                    OutlinedTextField(
                        value = charDescription,
                        onValueChange = {
                            charDescription = it
                            if (charDescError) charDescError = false
                        },
                        label = { Text("Description") },
                        placeholder = { Text("Describe the character...") },
                        isError = charDescError,
                        supportingText = if (charDescError) {
                            { Text("Description is required") }
                        } else null,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .offset { IntOffset(charDescShake.value.toInt(), 0) },
                        maxLines = 6,
                    )

                    OutlinedTextField(
                        value = charGreeting,
                        onValueChange = { charGreeting = it },
                        label = { Text("Greeting Message") },
                        placeholder = { Text("First message when starting a chat...") },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 4,
                    )

                    fun finishOnboarding() {
                        scope.launch {
                            val entry = PersonaEntry(
                                id = charId,
                                type = PersonaType.CHARACTER,
                                name = charName.trim(),
                                description = charDescription.trim(),
                                greetingMessage = charGreeting.trim().ifBlank { null },
                                avatarFileName = charAvatarFileName,
                            )
                            SettingsManager.saveEntry(entry)
                            SettingsManager.setSelectedCharacterId(entry.id)
                            SettingsManager.setOnboardingComplete()
                            onComplete()
                        }
                    }

                    Button(
                        onClick = {
                            haptics.tap()
                            val nameMissing = charName.isBlank()
                            val descMissing = charDescription.isBlank()

                            if (nameMissing || descMissing) {
                                haptics.reject()
                                if (nameMissing) {
                                    charNameError = true
                                    shake(charNameShake)
                                }
                                if (descMissing) {
                                    charDescError = true
                                    shake(charDescShake)
                                }
                                return@Button
                            }

                            if (charGreeting.isBlank()) {
                                showGreetingWarning = true
                                return@Button
                            }

                            finishOnboarding()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Get started")
                    }

                    if (showGreetingWarning) {
                        AlertDialog(
                            onDismissRequest = { showGreetingWarning = false },
                            title = { Text("Are you sure?") },
                            text = {
                                Text(
                                    "A greeting message helps set the tone and makes it much easier " +
                                        "to immerse yourself in the conversation — both for you and the AI.",
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showGreetingWarning = false
                                    finishOnboarding()
                                }) {
                                    Text("Continue anyway")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showGreetingWarning = false }) {
                                    Text("Take me back")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
