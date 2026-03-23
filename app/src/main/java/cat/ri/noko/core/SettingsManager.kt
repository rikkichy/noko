package cat.ri.noko.core

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
import cat.ri.noko.model.PromptPreset
import cat.ri.noko.model.defaultPromptPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "noko_settings")

private val json = Json { ignoreUnknownKeys = true }

private const val ENCRYPTED_PREFS_NAME = "noko_secure"
private const val KEY_API_KEY = "api_key"

object SettingsManager {

    private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    private val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
    private val NOKO_GUARD = booleanPreferencesKey("noko_guard")
    private val NOKO_POLKIT = booleanPreferencesKey("noko_polkit")
    private val NOKO_POLKIT_TRIM_EMOJIS = booleanPreferencesKey("noko_polkit_trim_emojis")
    private val NOKO_POLKIT_STRUCTURE_ACTIONS = booleanPreferencesKey("noko_polkit_structure_actions")
    private val PERSONAS_JSON = stringPreferencesKey("personas_json")
    private val SELECTED_PERSONA_ID = stringPreferencesKey("selected_persona_id")
    private val SELECTED_CHARACTER_ID = stringPreferencesKey("selected_character_id")
    private val SELECTED_MODEL_ID = stringPreferencesKey("selected_model_id")
    private val SELECTED_MODEL_NAME = stringPreferencesKey("selected_model_name")
    private val PROMPT_PRESETS_JSON = stringPreferencesKey("prompt_presets_json")
    private val SELECTED_PRESET_ID = stringPreferencesKey("selected_preset_id")

    private lateinit var appContext: Context
    private lateinit var securePrefs: SharedPreferences
    private val _apiKeyFlow = MutableStateFlow("")
    private var _amoledCached = false

    fun init(context: Context) {
        appContext = context.applicationContext
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        securePrefs = EncryptedSharedPreferences.create(
            ENCRYPTED_PREFS_NAME,
            masterKey,
            appContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        _apiKeyFlow.value = securePrefs.getString(KEY_API_KEY, "") ?: ""
        runBlocking {
            appContext.dataStore.data.first().let { _amoledCached = it[AMOLED_MODE] ?: false }
        }
        migrateApiKeyFromDataStore()
    }


    private fun migrateApiKeyFromDataStore() {
        val legacyKey = stringPreferencesKey("api_key")
        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch(Dispatchers.IO) {
            appContext.dataStore.edit { prefs ->
                val old = prefs[legacyKey]
                if (!old.isNullOrBlank()) {
                    securePrefs.edit().putString(KEY_API_KEY, old).apply()
                    _apiKeyFlow.value = old
                    prefs.remove(legacyKey)
                }
            }
        }
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

    val allEntries: Flow<List<PersonaEntry>>
        get() = appContext.dataStore.data.map { prefs ->
            prefs[PERSONAS_JSON]?.let { raw ->
                runCatching { json.decodeFromString<List<PersonaEntry>>(raw) }.getOrDefault(emptyList())
            } ?: emptyList()
        }

    val personas: Flow<List<PersonaEntry>>
        get() = allEntries.map { it.filter { e -> e.type == PersonaType.PERSONA } }

    val characters: Flow<List<PersonaEntry>>
        get() = allEntries.map { it.filter { e -> e.type == PersonaType.CHARACTER } }

    suspend fun saveEntry(entry: PersonaEntry) {
        appContext.dataStore.edit { prefs ->
            val current = prefs[PERSONAS_JSON]?.let {
                runCatching { json.decodeFromString<List<PersonaEntry>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            val updated = current.filterNot { it.id == entry.id } + entry
            prefs[PERSONAS_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun deleteEntry(id: String) {
        appContext.dataStore.edit { prefs ->
            val current = prefs[PERSONAS_JSON]?.let {
                runCatching { json.decodeFromString<List<PersonaEntry>>(it) }.getOrDefault(emptyList())
            } ?: emptyList()
            prefs[PERSONAS_JSON] = json.encodeToString(current.filterNot { it.id == id })
        }
    }

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

    suspend fun setApiKey(key: String) {
        securePrefs.edit().putString(KEY_API_KEY, key).apply()
        _apiKeyFlow.value = key
    }

    suspend fun clearApiKey() {
        securePrefs.edit().remove(KEY_API_KEY).apply()
        _apiKeyFlow.value = ""
    }

    val selectedModelId: Flow<String>
        get() = appContext.dataStore.data.map { it[SELECTED_MODEL_ID] ?: "" }

    val selectedModelName: Flow<String>
        get() = appContext.dataStore.data.map { it[SELECTED_MODEL_NAME] ?: "" }

    suspend fun setSelectedModel(id: String, name: String) {
        appContext.dataStore.edit { prefs ->
            prefs[SELECTED_MODEL_ID] = id
            prefs[SELECTED_MODEL_NAME] = name
        }
    }

    val promptPresets: Flow<List<PromptPreset>>
        get() = appContext.dataStore.data.map { prefs ->
            prefs[PROMPT_PRESETS_JSON]?.let { raw ->
                runCatching { json.decodeFromString<List<PromptPreset>>(raw) }
                    .getOrDefault(listOf(defaultPromptPreset()))
            } ?: listOf(defaultPromptPreset())
        }

    val selectedPresetId: Flow<String>
        get() = appContext.dataStore.data.map { it[SELECTED_PRESET_ID] ?: "default" }

    suspend fun savePreset(preset: PromptPreset) {
        appContext.dataStore.edit { prefs ->
            val current = prefs[PROMPT_PRESETS_JSON]?.let {
                runCatching { json.decodeFromString<List<PromptPreset>>(it) }
                    .getOrDefault(listOf(defaultPromptPreset()))
            } ?: listOf(defaultPromptPreset())
            val updated = current.filterNot { it.id == preset.id } + preset
            prefs[PROMPT_PRESETS_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun setSelectedPresetId(id: String) {
        appContext.dataStore.edit { it[SELECTED_PRESET_ID] = id }
    }
}
