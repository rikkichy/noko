package cat.ri.noko.core

import android.content.Context
import android.content.SharedPreferences
import cat.ri.noko.BuildConfig
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import cat.ri.noko.model.ChatMessage
import cat.ri.noko.model.ChatSessionMeta
import cat.ri.noko.model.SwipeAlternative
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
import cat.ri.noko.model.PromptPreset
import cat.ri.noko.model.builtInPresets
import cat.ri.noko.model.defaultPromptPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URL

private val Context.dataStore by preferencesDataStore(name = "noko_settings")

private val json = Json { ignoreUnknownKeys = true }

private const val ENCRYPTED_PREFS_NAME = "noko_secure"
private const val KEY_API_KEY = "api_key"
private const val KEY_PERSONAS_JSON = "personas_json"
private const val KEY_PRESETS_JSON = "prompt_presets_json"

object SettingsManager {

    private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    private val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
    private val SHOW_AVATARS = booleanPreferencesKey("show_avatars")
    private val SHOW_NAMES = booleanPreferencesKey("show_names")
    private val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
    private val NOKO_GUARD = booleanPreferencesKey("noko_guard")
    private val NOKO_POLKIT = booleanPreferencesKey("noko_polkit")
    private val NOKO_POLKIT_TRIM_EMOJIS = booleanPreferencesKey("noko_polkit_trim_emojis")
    private val NOKO_POLKIT_STRUCTURE_ACTIONS = booleanPreferencesKey("noko_polkit_structure_actions")
    private val NOKO_POLKIT_STREAM_NOTIFICATIONS = booleanPreferencesKey("noko_polkit_stream_notifications")
    private val SCREEN_SECURITY = booleanPreferencesKey("screen_security")
    private val INCOGNITO_KEYBOARD = booleanPreferencesKey("incognito_keyboard")
    private val CLEAR_CLIPBOARD = booleanPreferencesKey("clear_clipboard")
    private val HIDE_FROM_RECENTS = booleanPreferencesKey("hide_from_recents")
    private val SELECTED_PERSONA_ID = stringPreferencesKey("selected_persona_id")
    private val SELECTED_CHARACTER_ID = stringPreferencesKey("selected_character_id")
    private val SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
    private val SELECTED_MODEL_NAME = stringPreferencesKey("selected_model_name")
    private val SELECTED_MODEL_CONTEXT_LENGTH = intPreferencesKey("selected_model_context_length")
    private val SELECTED_PRESET_ID = stringPreferencesKey("selected_preset_id")

    private val BIOMETRIC_AUTH = booleanPreferencesKey("biometric_auth")

    private val SELECTED_PROVIDER_ID = stringPreferencesKey("selected_provider_id")
    private val CUSTOM_PROVIDER_URL = stringPreferencesKey("custom_provider_url")
    private val CUSTOM_PROVIDER_AUTH = booleanPreferencesKey("custom_provider_auth")
    private val PROVIDER_URL_OVERRIDE_PREFIX = "provider_url_"

    private lateinit var appContext: Context
    private lateinit var securePrefs: SharedPreferences
    private val _apiKeyFlow = MutableStateFlow("")
    private val _personasFlow = MutableStateFlow<List<PersonaEntry>>(emptyList())
    private val _presetsFlow = MutableStateFlow<List<PromptPreset>>(listOf(defaultPromptPreset()))
    private var _amoledCached = false
    private var _secureInitDone = false

    fun initBasic(context: Context) {
        appContext = context.applicationContext
        runBlocking {
            appContext.dataStore.data.first().let { _amoledCached = it[AMOLED_MODE] ?: false }
        }
    }

    fun initSecure() {
        if (_secureInitDone) return
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        securePrefs = EncryptedSharedPreferences.create(
            ENCRYPTED_PREFS_NAME,
            masterKey,
            appContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        val providerId = runBlocking {
            appContext.dataStore.data.first()[SELECTED_PROVIDER_ID] ?: "openrouter"
        }
        _apiKeyFlow.value = securePrefs.getString("${KEY_API_KEY}_$providerId", "") ?: ""
        _personasFlow.value = loadEncryptedList(KEY_PERSONAS_JSON)
        if (BuildConfig.DEBUG && _personasFlow.value.isEmpty()) {
            val seed = (1..6).flatMap { i ->
                listOf(
                    PersonaEntry(
                        type = PersonaType.PERSONA,
                        name = "Persona $i",
                        description = "[Name: {{user}};\nInfo: {{user}} is a persona card test;]",
                    ),
                    PersonaEntry(
                        type = PersonaType.CHARACTER,
                        name = "Character $i",
                        description = "[Name: {{char}};\nTask: {{char}} should greet {{user}} and ask generic questions. {{char}} will always use internet RP style to chat with {{user}};]",
                        greetingMessage = "*comes closer*\n\"Hello there, {{user}}! How are you doing today?\"",
                    ),
                )
            }
            securePrefs.edit().putString(KEY_PERSONAS_JSON, json.encodeToString(seed)).commit()
            _personasFlow.value = seed

            val userPresets = loadEncryptedList<PromptPreset>(KEY_PRESETS_JSON)
                .filterNot { it.builtIn }
            _presetsFlow.value = builtInPresets() + userPresets
            ChatStorage.init(appContext)

            val personas = seed.filter { it.type == PersonaType.PERSONA }
            val characters = seed.filter { it.type == PersonaType.CHARACTER }
            runBlocking {
                for (i in 0 until 5) {
                    val persona = personas[i]
                    val character = characters[i]
                    val chatId = java.util.UUID.randomUUID().toString()
                    val now = System.currentTimeMillis() - (4 - i) * 60_000L
                    val messages = listOf(
                        ChatMessage(
                            role = ChatMessage.Role.USER,
                            content = "*looks at you in surprise*\n\"Doing great! But where are we..\"",
                            senderName = persona.name,
                            senderAvatarFileName = persona.avatarFileName,
                            timestamp = now,
                        ),
                        ChatMessage(
                            role = ChatMessage.Role.ASSISTANT,
                            content = "\"We are in Debug chat! This is for testing Noko app, the best AI roleplay frontend!\"",
                            senderName = character.name,
                            senderAvatarFileName = character.avatarFileName,
                            timestamp = now + 1000L,
                            alternatives = listOf(
                                SwipeAlternative(
                                    content = "\"We are in Debug chat! This is for testing Noko app, the best AI roleplay frontend!\"",
                                ),
                                SwipeAlternative(
                                    content = "\"Oh! That's weird. Someone just decided to change what I'm saying!\"",
                                ),
                            ),
                        ),
                    )
                    val meta = ChatSessionMeta(
                        id = chatId,
                        characterId = character.id,
                        characterName = character.name,
                        characterAvatarFileName = character.avatarFileName,
                        lastMessagePreview = messages.last().content,
                        lastMessageRole = "ASSISTANT",
                        updatedAt = now + 1000L,
                        messageCount = messages.size,
                        personaName = persona.name,
                        personaAvatarFileName = persona.avatarFileName,
                    )
                    ChatStorage.saveChat(chatId, messages, meta)
                }
            }
            _secureInitDone = true
            return
        }
        val userPresets = loadEncryptedList<PromptPreset>(KEY_PRESETS_JSON)
            .filterNot { it.builtIn }
        _presetsFlow.value = builtInPresets() + userPresets
        ChatStorage.init(appContext)
        _secureInitDone = true
    }

    private inline fun <reified T> loadEncryptedList(key: String): List<T> {
        val raw = securePrefs.getString(key, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<T>>(raw) }.getOrDefault(emptyList())
    }

    val onboardingComplete: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }

    suspend fun setOnboardingComplete() {
        appContext.dataStore.edit { it[ONBOARDING_COMPLETE] = true }
    }

    val amoledMode: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[AMOLED_MODE] ?: false }

    fun isAmoled(): Boolean = _amoledCached

    suspend fun setAmoledMode(enabled: Boolean) {
        _amoledCached = enabled
        appContext.dataStore.edit { it[AMOLED_MODE] = enabled }
    }

    val showAvatars: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[SHOW_AVATARS] ?: true }

    suspend fun setShowAvatars(enabled: Boolean) {
        appContext.dataStore.edit { it[SHOW_AVATARS] = enabled }
    }

    val showNames: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[SHOW_NAMES] ?: true }

    suspend fun setShowNames(enabled: Boolean) {
        appContext.dataStore.edit { it[SHOW_NAMES] = enabled }
    }

    val reduceMotion: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[REDUCE_MOTION] ?: false }

    suspend fun setReduceMotion(enabled: Boolean) {
        appContext.dataStore.edit { it[REDUCE_MOTION] = enabled }
    }

    val nokoGuard: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[NOKO_GUARD] ?: true }

    suspend fun setNokoGuard(enabled: Boolean) {
        appContext.dataStore.edit { it[NOKO_GUARD] = enabled }
    }

    val nokoPolkit: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[NOKO_POLKIT] ?: true }

    suspend fun setNokoPolkit(enabled: Boolean) {
        appContext.dataStore.edit { it[NOKO_POLKIT] = enabled }
    }

    val nokoPolkitTrimEmojis: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[NOKO_POLKIT_TRIM_EMOJIS] ?: true }

    suspend fun setNokoPolkitTrimEmojis(enabled: Boolean) {
        appContext.dataStore.edit { it[NOKO_POLKIT_TRIM_EMOJIS] = enabled }
    }

    val nokoPolkitStructureActions: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[NOKO_POLKIT_STRUCTURE_ACTIONS] ?: true }

    suspend fun setNokoPolkitStructureActions(enabled: Boolean) {
        appContext.dataStore.edit { it[NOKO_POLKIT_STRUCTURE_ACTIONS] = enabled }
    }

    val nokoPolkitStreamNotifications: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[NOKO_POLKIT_STREAM_NOTIFICATIONS] ?: false }

    suspend fun setNokoPolkitStreamNotifications(enabled: Boolean) {
        appContext.dataStore.edit { it[NOKO_POLKIT_STREAM_NOTIFICATIONS] = enabled }
    }

    val screenSecurity: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[SCREEN_SECURITY] ?: false }

    suspend fun setScreenSecurity(enabled: Boolean) {
        appContext.dataStore.edit { it[SCREEN_SECURITY] = enabled }
    }

    val incognitoKeyboard: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[INCOGNITO_KEYBOARD] ?: false }

    suspend fun setIncognitoKeyboard(enabled: Boolean) {
        appContext.dataStore.edit { it[INCOGNITO_KEYBOARD] = enabled }
    }

    val clearClipboard: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[CLEAR_CLIPBOARD] ?: false }

    suspend fun setClearClipboard(enabled: Boolean) {
        appContext.dataStore.edit { it[CLEAR_CLIPBOARD] = enabled }
    }

    val hideFromRecents: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[HIDE_FROM_RECENTS] ?: false }

    suspend fun setHideFromRecents(enabled: Boolean) {
        appContext.dataStore.edit { it[HIDE_FROM_RECENTS] = enabled }
    }

    @Volatile
    var suppressBiometricRelock = false

    val biometricAuth: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[BIOMETRIC_AUTH] ?: false }

    suspend fun setBiometricAuth(enabled: Boolean) {
        appContext.dataStore.edit { prefs ->
            prefs[BIOMETRIC_AUTH] = enabled
            if (enabled) prefs[NOKO_POLKIT_STREAM_NOTIFICATIONS] = false
        }
    }

    val allEntries: Flow<List<PersonaEntry>>
        get() = _personasFlow

    val personas: Flow<List<PersonaEntry>>
        get() = allEntries.map { it.filter { e -> e.type == PersonaType.PERSONA } }

    val characters: Flow<List<PersonaEntry>>
        get() = allEntries.map { it.filter { e -> e.type == PersonaType.CHARACTER } }

    suspend fun saveEntry(entry: PersonaEntry) = withContext(Dispatchers.IO) {
        _personasFlow.update { current ->
            val updated = current.filterNot { it.id == entry.id } + entry
            securePrefs.edit().putString(KEY_PERSONAS_JSON, json.encodeToString(updated)).commit()
            updated
        }
    }

    suspend fun deleteEntry(id: String) = withContext(Dispatchers.IO) {
        _personasFlow.update { current ->
            val updated = current.filterNot { it.id == id }
            securePrefs.edit().putString(KEY_PERSONAS_JSON, json.encodeToString(updated)).commit()
            updated
        }
    }

    fun getEntries(type: PersonaType): List<PersonaEntry> =
        _personasFlow.value.filter { it.type == type }

    fun getEntry(entries: List<PersonaEntry>, id: String): PersonaEntry? =
        entries.find { it.id == id }

    val selectedPersonaId: Flow<String?>
        get() = appContext.dataStore.data.map { it[SELECTED_PERSONA_ID] }

    val selectedCharacterId: Flow<String?>
        get() = appContext.dataStore.data.map { it[SELECTED_CHARACTER_ID] }

    suspend fun setSelectedPersonaId(id: String?) {
        appContext.dataStore.edit { prefs ->
            if (id == null) prefs.remove(SELECTED_PERSONA_ID) else prefs[SELECTED_PERSONA_ID] = id
        }
    }

    suspend fun setSelectedCharacterId(id: String?) {
        appContext.dataStore.edit { prefs ->
            if (id == null) prefs.remove(SELECTED_CHARACTER_ID) else prefs[SELECTED_CHARACTER_ID] = id
        }
    }

    val apiKey: Flow<String>
        get() = _apiKeyFlow

    fun hasApiKey(): Boolean = _apiKeyFlow.value.isNotBlank()

    fun getApiKeyForProvider(providerId: String): String =
        securePrefs.getString("${KEY_API_KEY}_$providerId", "") ?: ""

    suspend fun setApiKey(key: String) {
        val trimmed = key.filter { it.code >= 0x20 && it.code != 0x7f }.trim()
        val providerId = appContext.dataStore.data.first()[SELECTED_PROVIDER_ID] ?: "openrouter"
        withContext(Dispatchers.IO) {
            securePrefs.edit().putString("${KEY_API_KEY}_$providerId", trimmed).commit()
        }
        _apiKeyFlow.value = trimmed
    }

    suspend fun clearApiKey() {
        val providerId = appContext.dataStore.data.first()[SELECTED_PROVIDER_ID] ?: "openrouter"
        withContext(Dispatchers.IO) {
            securePrefs.edit().remove("${KEY_API_KEY}_$providerId").commit()
        }
        _apiKeyFlow.value = ""
    }

    val selectedProviderId: Flow<String>
        get() = appContext.dataStore.data.map { it[SELECTED_PROVIDER_ID] ?: "openrouter" }

    fun getSelectedProviderId(): String = runBlocking {
        appContext.dataStore.data.first()[SELECTED_PROVIDER_ID] ?: "openrouter"
    }

    val customProviderUrl: Flow<String>
        get() = appContext.dataStore.data.map { it[CUSTOM_PROVIDER_URL] ?: "" }

    val customProviderAuth: Flow<Boolean>
        get() = appContext.dataStore.data.map { it[CUSTOM_PROVIDER_AUTH] ?: false }

    suspend fun setSelectedProvider(providerId: String) {
        appContext.dataStore.edit { prefs ->
            prefs[SELECTED_PROVIDER_ID] = providerId
            prefs.remove(SELECTED_MODEL_ID)
            prefs.remove(SELECTED_MODEL_NAME)
            prefs.remove(SELECTED_MODEL_CONTEXT_LENGTH)
        }
        _apiKeyFlow.value = getApiKeyForProvider(providerId)
    }

    private const val MAX_URL_LENGTH = 2048

    fun validateProviderUrl(url: String): Boolean {
        if (url.isBlank() || url.length > MAX_URL_LENGTH) return false
        return try {
            val parsed = URL(url)
            val host = parsed.host?.lowercase() ?: return false
            val isLocal = host == "localhost" || host == "127.0.0.1" || isPrivateNetwork(host)
            when (parsed.protocol) {
                "https" -> true
                "http" -> isLocal
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isPrivateNetwork(host: String): Boolean {
        val parts = host.split('.').mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return host.endsWith(".local")
        return parts[0] == 10 ||
                (parts[0] == 172 && parts[1] in 16..31) ||
                (parts[0] == 192 && parts[1] == 168)
    }

    suspend fun setCustomProviderUrl(url: String) {
        appContext.dataStore.edit { it[CUSTOM_PROVIDER_URL] = url }
    }

    suspend fun setCustomProviderAuth(requiresAuth: Boolean) {
        appContext.dataStore.edit { it[CUSTOM_PROVIDER_AUTH] = requiresAuth }
    }

    fun getProviderUrlOverride(providerId: String): Flow<String> {
        val key = stringPreferencesKey("${PROVIDER_URL_OVERRIDE_PREFIX}$providerId")
        return appContext.dataStore.data.map { it[key] ?: "" }
    }

    fun getProviderUrlOverrideSync(providerId: String): String = runBlocking {
        val key = stringPreferencesKey("${PROVIDER_URL_OVERRIDE_PREFIX}$providerId")
        appContext.dataStore.data.first()[key] ?: ""
    }

    suspend fun setProviderUrlOverride(providerId: String, url: String) {
        val key = stringPreferencesKey("${PROVIDER_URL_OVERRIDE_PREFIX}$providerId")
        appContext.dataStore.edit { it[key] = url }
    }

    val selectedModelId: Flow<String>
        get() = appContext.dataStore.data.map { it[SELECTED_MODEL_ID] ?: "" }

    val selectedModelName: Flow<String>
        get() = appContext.dataStore.data.map { it[SELECTED_MODEL_NAME] ?: "" }

    val selectedModelContextLength: Flow<Int>
        get() = appContext.dataStore.data.map { it[SELECTED_MODEL_CONTEXT_LENGTH] ?: 0 }

    suspend fun setSelectedModel(id: String, name: String, contextLength: Int? = null) {
        appContext.dataStore.edit { prefs ->
            prefs[SELECTED_MODEL_ID] = id
            prefs[SELECTED_MODEL_NAME] = name
            if (contextLength != null && contextLength > 0) {
                prefs[SELECTED_MODEL_CONTEXT_LENGTH] = contextLength
            } else {
                prefs.remove(SELECTED_MODEL_CONTEXT_LENGTH)
            }
        }
    }

    val promptPresets: Flow<List<PromptPreset>>
        get() = _presetsFlow

    val selectedPresetId: Flow<String>
        get() = appContext.dataStore.data.map { it[SELECTED_PRESET_ID] ?: "default" }

    suspend fun savePreset(preset: PromptPreset) = withContext(Dispatchers.IO) {
        _presetsFlow.update { current ->
            val updated = current.filterNot { it.id == preset.id } + preset
            securePrefs.edit().putString(KEY_PRESETS_JSON, json.encodeToString(updated)).commit()
            updated
        }
    }

    suspend fun setSelectedPresetId(id: String) {
        appContext.dataStore.edit { it[SELECTED_PRESET_ID] = id }
    }

    suspend fun deletePreset(id: String) = withContext(Dispatchers.IO) {
        _presetsFlow.update { current ->
            if (current.find { it.id == id }?.builtIn == true) return@update current
            val updated = current.filterNot { it.id == id }
            securePrefs.edit().putString(KEY_PRESETS_JSON, json.encodeToString(updated)).commit()
            updated
        }
    }
}
