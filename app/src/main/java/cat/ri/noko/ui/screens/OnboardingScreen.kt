package cat.ri.noko.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.FileOpen
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.AvatarStorage
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.core.api.ApiClient
import cat.ri.noko.core.api.humanizeException
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
import cat.ri.noko.model.builtInProviders
import cat.ri.noko.model.getProviderById
import cat.ri.noko.ui.components.ImageCropOverlay
import cat.ri.noko.ui.components.CustomProviderCard
import cat.ri.noko.ui.components.PersonaFormFields
import cat.ri.noko.ui.components.ProviderCard
import cat.ri.noko.ui.util.rememberNokoHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class OnboardingStep { Provider, ApiKey, Model, Persona, Character, ImportCharacter }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()

    var step by remember { mutableStateOf(OnboardingStep.Provider) }

    var selectedProviderId by remember { mutableStateOf("openrouter") }
    val customAuth by SettingsManager.customProviderAuth.collectAsState(initial = false)

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

    var importUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) pendingCropUri = uri }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            importUri = uri
            step = OnboardingStep.ImportCharacter
        }
    }

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

    fun requiresAuth(): Boolean {
        val provider = getProviderById(selectedProviderId)
        return provider?.requiresAuth ?: customAuth
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
                OnboardingStep.Provider -> {
                    TopAppBar(
                        title = { Text("Noko") },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
                OnboardingStep.ApiKey -> {
                    TopAppBar(
                        title = { Text("API Key") },
                        navigationIcon = {
                            IconButton(onClick = {
                                haptics.tap()
                                step = OnboardingStep.Provider
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
                OnboardingStep.Model -> {
                    TopAppBar(
                        title = { Text("Select a model") },
                        navigationIcon = {
                            IconButton(onClick = {
                                haptics.tap()
                                step = if (requiresAuth()) OnboardingStep.ApiKey else OnboardingStep.Provider
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            TextButton(onClick = {
                                haptics.tap()
                                step = OnboardingStep.Persona
                            }) {
                                Text("Skip")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
                OnboardingStep.Persona -> {
                    TopAppBar(
                        title = { Text("Create your persona") },
                        navigationIcon = {
                            IconButton(onClick = {
                                haptics.tap()
                                step = OnboardingStep.Model
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
                OnboardingStep.ImportCharacter -> {
                    TopAppBar(
                        title = { Text("Import character") },
                        navigationIcon = {
                            IconButton(onClick = {
                                haptics.tap()
                                step = OnboardingStep.Character
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
            }
        },
    ) { padding ->
        when (step) {
            OnboardingStep.Provider -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Welcome!",
                        style = MaterialTheme.typography.headlineMedium,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Choose where your AI models are hosted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(4.dp))

                    CustomProviderCard(
                        isSelected = selectedProviderId == "custom",
                        onSelect = {
                            haptics.tap()
                            selectedProviderId = "custom"
                        },
                    )

                    builtInProviders.forEach { provider ->
                        ProviderCard(
                            provider = provider,
                            isSelected = selectedProviderId == provider.id,
                            onSelect = {
                                haptics.tap()
                                selectedProviderId = provider.id
                            },
                        )
                    }

                    Button(
                        onClick = {
                            haptics.tap()
                            scope.launch {
                                SettingsManager.setSelectedProvider(selectedProviderId)
                                step = if (requiresAuth()) OnboardingStep.ApiKey else OnboardingStep.Model
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Next")
                    }
                }
            }

            OnboardingStep.ApiKey -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val provider = remember(selectedProviderId) { getProviderById(selectedProviderId) }
                    val providerName = provider?.name ?: "Custom"
                    val onboardingUrlOverride by SettingsManager.getProviderUrlOverride(selectedProviderId).collectAsState(initial = "")
                    val customUrl by SettingsManager.customProviderUrl.collectAsState(initial = "")
                    val providerBaseUrl = if (provider != null) onboardingUrlOverride.ifBlank { provider.baseUrl } else customUrl
                    val placeholder = when (selectedProviderId) {
                        "openrouter" -> "sk-or-v1-..."
                        "openai" -> "sk-..."
                        else -> "API key"
                    }

                    Text(
                        "Enter your $providerName API key to get started.",
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
                        placeholder = { Text(placeholder) },
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
                                    ApiClient.configure(apiKeyInput, providerBaseUrl, selectedProviderId)
                                    withContext(Dispatchers.IO) {
                                        ApiClient.validateConnection()
                                    }
                                }.onSuccess {
                                    SettingsManager.setApiKey(apiKeyInput)
                                    step = OnboardingStep.Model
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

                    if (selectedProviderId == "openrouter") {
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
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create OpenRouter API Key")
                        }
                    }
                }
            }

            OnboardingStep.Model -> {
                ModelListContent(
                    onModelSelected = { step = OnboardingStep.Persona },
                    modifier = Modifier.padding(padding),
                )
            }

            OnboardingStep.Persona -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "This is how the AI will know you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(4.dp))

                    PersonaFormFields(
                        type = PersonaType.PERSONA,
                        avatarFileName = avatarFileName,
                        onAvatarClick = {
                            haptics.tap()
                            cropForCharacter = false
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        name = personaName,
                        onNameChange = {
                            personaName = it
                            if (nameError) nameError = false
                        },
                        nameError = nameError,
                        nameShakeOffset = nameShake.value,
                        description = personaDescription,
                        onDescriptionChange = {
                            personaDescription = it
                            if (descError) descError = false
                        },
                        descError = descError,
                        descShakeOffset = descShake.value,
                        avatarSize = 100.dp,
                        fallbackIcon = Icons.Filled.Person,
                        namePlaceholder = "Your persona name...",
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
            }

            OnboardingStep.Character -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Create a character for the AI to play.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(4.dp))

                    PersonaFormFields(
                        type = PersonaType.CHARACTER,
                        avatarFileName = charAvatarFileName,
                        onAvatarClick = {
                            haptics.tap()
                            cropForCharacter = true
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        name = charName,
                        onNameChange = {
                            charName = it
                            if (charNameError) charNameError = false
                        },
                        nameError = charNameError,
                        nameShakeOffset = charNameShake.value,
                        description = charDescription,
                        onDescriptionChange = {
                            charDescription = it
                            if (charDescError) charDescError = false
                        },
                        descError = charDescError,
                        descShakeOffset = charDescShake.value,
                        greetingMessage = charGreeting,
                        onGreetingChange = { charGreeting = it },
                        avatarSize = 100.dp,
                        fallbackIcon = Icons.Filled.SmartToy,
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

                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            SettingsManager.suppressBiometricRelock = true
                            importLauncher.launch(
                                arrayOf("image/png", "application/json", "application/octet-stream"),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import character")
                    }

                    if (showGreetingWarning) {
                        AlertDialog(
                            onDismissRequest = { showGreetingWarning = false },
                            title = { Text("Are you sure?") },
                            text = {
                                Text(
                                    "A greeting message helps set the style and makes it much easier " +
                                        "to immerse yourself in the RP, both for you and your AI character.",
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showGreetingWarning = false
                                    finishOnboarding()
                                }) {
                                    Text("Skip")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showGreetingWarning = false }) {
                                    Text("Go back")
                                }
                            },
                        )
                    }
                }
            }

            OnboardingStep.ImportCharacter -> {
                val uri = importUri
                if (uri != null) {
                    CharacterImportContent(
                        uri = uri,
                        onComplete = {
                            scope.launch {
                                val chars = SettingsManager.characters.first()
                                val firstChar = chars.firstOrNull()
                                if (firstChar != null) {
                                    SettingsManager.setSelectedCharacterId(firstChar.id)
                                }
                                SettingsManager.setOnboardingComplete()
                                onComplete()
                            }
                        },
                        onBack = { step = OnboardingStep.Character },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}
