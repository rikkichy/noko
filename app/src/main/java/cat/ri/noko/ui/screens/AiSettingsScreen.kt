package cat.ri.noko.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import cat.ri.noko.ui.theme.NokoFieldShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import cat.ri.noko.ui.components.NokoDropdown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.ri.noko.R
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PromptPreset
import cat.ri.noko.model.PromptSection
import cat.ri.noko.model.PromptSectionType
import cat.ri.noko.model.builtInPresets
import cat.ri.noko.model.duplicate
import cat.ri.noko.ui.theme.nokoTopAppBarColors
import cat.ri.noko.ui.util.rememberNokoHaptics
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()

    val presets by SettingsManager.promptPresets.collectAsState(initial = builtInPresets())
    val selectedPresetId by SettingsManager.selectedPresetId.collectAsState(initial = "default")
    var presetIdOverride by remember { mutableStateOf<String?>(null) }
    val effectivePresetId = presetIdOverride ?: selectedPresetId
    val activePreset = remember(presets, effectivePresetId) {
        presets.find { it.id == effectivePresetId } ?: presets.first()
    }

    var sections by remember(activePreset) { mutableStateOf(activePreset.sections) }
    var temperature by remember(activePreset) { mutableStateOf(activePreset.temperature) }
    var topP by remember(activePreset) { mutableStateOf(activePreset.topP) }
    var topK by remember(activePreset) { mutableStateOf(activePreset.topK) }
    var maxTokens by remember(activePreset) { mutableStateOf(activePreset.maxTokens) }
    var frequencyPenalty by remember(activePreset) { mutableStateOf(activePreset.frequencyPenalty) }
    var presencePenalty by remember(activePreset) { mutableStateOf(activePreset.presencePenalty) }
    var continueNudgePrompt by remember(activePreset) { mutableStateOf(activePreset.continueNudgePrompt) }

    val allEntries by SettingsManager.allEntries.collectAsState(initial = emptyList())
    val selectedPersonaId by SettingsManager.selectedPersonaId.collectAsState(initial = null)
    val selectedCharacterId by SettingsManager.selectedCharacterId.collectAsState(initial = null)
    val activePersona = remember(allEntries, selectedPersonaId) {
        selectedPersonaId?.let { id -> allEntries.find { it.id == id } }
    }
    val activeCharacter = remember(allEntries, selectedCharacterId) {
        selectedCharacterId?.let { id -> allEntries.find { it.id == id } }
    }

    val isBuiltIn = activePreset.builtIn
    var showPresetDropdown by remember { mutableStateOf(false) }
    var showNewPresetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    fun currentEdits() = activePreset.copy(
        sections = sections,
        temperature = temperature,
        topP = topP,
        topK = topK,
        maxTokens = maxTokens,
        frequencyPenalty = frequencyPenalty,
        presencePenalty = presencePenalty,
        continueNudgePrompt = continueNudgePrompt,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (!isBuiltIn) {
                        IconButton(onClick = {
                            haptics.confirm()
                            scope.launch {
                                SettingsManager.savePreset(currentEdits())
                                onBack()
                            }
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.common_save))
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.ai_settings_preset), style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.tap()
                                        showPresetDropdown = true
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    activePreset.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            NokoDropdown(
                                expanded = showPresetDropdown,
                                onDismissRequest = { showPresetDropdown = false },
                            ) {
                                presets.forEach { preset ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    preset.name,
                                                    modifier = Modifier.weight(1f),
                                                )
                                                if (preset.builtIn) {
                                                    Spacer(Modifier.size(8.dp))
                                                    Text(
                                                        stringResource(R.string.ai_settings_built_in),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            haptics.tap()
                                            showPresetDropdown = false
                                            presetIdOverride = preset.id
                                            scope.launch {
                                                SettingsManager.setSelectedPresetId(preset.id)
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        IconButton(onClick = {
                            haptics.tap()
                            showNewPresetDialog = true
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.ai_settings_new_preset))
                        }
                        IconButton(onClick = {
                            haptics.tap()
                            val dup = activePreset.duplicate()
                            presetIdOverride = dup.id
                            scope.launch {
                                SettingsManager.savePreset(dup)
                                SettingsManager.setSelectedPresetId(dup.id)
                            }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.ai_settings_duplicate_preset))
                        }
                        if (!isBuiltIn) {
                            IconButton(onClick = {
                                haptics.tap()
                                showDeleteDialog = true
                            }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.ai_settings_delete_preset),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    if (isBuiltIn) {
                        Text(
                            stringResource(R.string.ai_settings_built_in_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .alpha(if (isBuiltIn) 0.6f else 1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.ai_settings_generation_params), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(4.dp))

                    ParamSlider(
                        label = stringResource(R.string.ai_settings_param_temperature),
                        value = temperature,
                        range = 0f..2f,
                        steps = 39,
                        format = { "%.2f".format(it) },
                        enabled = !isBuiltIn,
                        onToggle = { temperature = if (it) 1.0f else null },
                        onChange = { temperature = it },
                    )
                    ParamSlider(
                        label = stringResource(R.string.ai_settings_param_top_p),
                        value = topP,
                        range = 0f..1f,
                        steps = 19,
                        format = { "%.2f".format(it) },
                        enabled = !isBuiltIn,
                        onToggle = { topP = if (it) 1.0f else null },
                        onChange = { topP = it },
                    )
                    ParamIntSlider(
                        label = stringResource(R.string.ai_settings_param_top_k),
                        value = topK,
                        range = 0f..200f,
                        steps = 199,
                        enabled = !isBuiltIn,
                        onToggle = { topK = if (it) 50 else null },
                        onChange = { topK = it },
                    )
                    ParamIntSlider(
                        label = stringResource(R.string.ai_settings_param_max_tokens),
                        value = maxTokens,
                        range = 0f..32768f,
                        steps = 511,
                        enabled = !isBuiltIn,
                        onToggle = { maxTokens = if (it) 4096 else null },
                        onChange = { maxTokens = it },
                    )
                    ParamSlider(
                        label = stringResource(R.string.ai_settings_param_freq_penalty),
                        value = frequencyPenalty,
                        range = -2f..2f,
                        steps = 79,
                        format = { "%.2f".format(it) },
                        enabled = !isBuiltIn,
                        onToggle = { frequencyPenalty = if (it) 0f else null },
                        onChange = { frequencyPenalty = it },
                    )
                    ParamSlider(
                        label = stringResource(R.string.ai_settings_param_presence_penalty),
                        value = presencePenalty,
                        range = -2f..2f,
                        steps = 79,
                        format = { "%.2f".format(it) },
                        enabled = !isBuiltIn,
                        onToggle = { presencePenalty = if (it) 0f else null },
                        onChange = { presencePenalty = it },
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .alpha(if (isBuiltIn) 0.6f else 1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.ai_settings_advanced), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.ai_settings_continue_nudge),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.ai_settings_continue_nudge_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = continueNudgePrompt,
                        onValueChange = { continueNudgePrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBuiltIn,
                        minLines = 2,
                        shape = NokoFieldShape,
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .alpha(if (isBuiltIn) 0.6f else 1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.ai_settings_prompt_template), style = MaterialTheme.typography.titleMedium)

                    sections.forEachIndexed { index, section ->
                        PromptSectionCard(
                            section = section,
                            persona = activePersona,
                            character = activeCharacter,
                            enabled = !isBuiltIn,
                            onToggle = { enabled ->
                                if (enabled) haptics.toggleOn() else haptics.toggleOff()
                                sections = sections.toMutableList().also {
                                    it[index] = section.copy(enabled = enabled)
                                }
                            },
                            onTextChange = { text ->
                                sections = sections.toMutableList().also {
                                    it[index] = section.copy(content = text)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showNewPresetDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewPresetDialog = false },
            title = { Text(stringResource(R.string.ai_settings_new_preset_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text(stringResource(R.string.ai_settings_name)) },
                    singleLine = true,
                    shape = NokoFieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        haptics.confirm()
                        val preset = PromptPreset(
                            id = UUID.randomUUID().toString(),
                            name = name.trim(),
                        )
                        presetIdOverride = preset.id
                        scope.launch {
                            SettingsManager.savePreset(preset)
                            SettingsManager.setSelectedPresetId(preset.id)
                        }
                        showNewPresetDialog = false
                    },
                ) {
                    Text(stringResource(R.string.common_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPresetDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.ai_settings_delete_preset_title, activePreset.name)) },
            text = { Text(stringResource(R.string.common_undone)) },
            confirmButton = {
                TextButton(onClick = {
                    haptics.reject()
                    presetIdOverride = "default"
                    scope.launch {
                        SettingsManager.setSelectedPresetId("default")
                        SettingsManager.deletePreset(activePreset.id)
                    }
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ParamSlider(
    label: String,
    value: Float?,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onChange: (Float) -> Unit,
) {
    val active = value != null
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (active) {
                Text(
                    format(value!!),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(8.dp))
            }
            Switch(
                checked = active,
                onCheckedChange = onToggle,
                enabled = enabled,
            )
        }
        AnimatedVisibility(visible = active) {
            Slider(
                value = value ?: range.start,
                onValueChange = onChange,
                valueRange = range,
                steps = steps,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ParamIntSlider(
    label: String,
    value: Int?,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onChange: (Int) -> Unit,
) {
    val active = value != null
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (active) {
                Text(
                    value.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(8.dp))
            }
            Switch(
                checked = active,
                onCheckedChange = onToggle,
                enabled = enabled,
            )
        }
        AnimatedVisibility(visible = active) {
            Slider(
                value = value?.toFloat() ?: range.start,
                onValueChange = { onChange(it.toInt()) },
                valueRange = range,
                steps = steps,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PromptSectionCard(
    section: PromptSection,
    persona: PersonaEntry?,
    character: PersonaEntry?,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(
                    when (section.type) {
                        PromptSectionType.MAIN_PROMPT -> R.string.ai_settings_section_main_prompt
                        PromptSectionType.PERSONA_DESCRIPTION -> R.string.ai_settings_section_persona_description
                        PromptSectionType.CHARACTER_DESCRIPTION -> R.string.ai_settings_section_character_description
                        PromptSectionType.CHAT_HISTORY -> R.string.ai_settings_section_chat_history
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (section.type != PromptSectionType.CHAT_HISTORY) {
                Switch(
                    checked = section.enabled,
                    onCheckedChange = onToggle,
                    enabled = enabled,
                )
            }
        }

        when (section.type) {
            PromptSectionType.MAIN_PROMPT -> {
                OutlinedTextField(
                    value = section.content ?: "",
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = section.enabled && enabled,
                    minLines = 2,
                    shape = NokoFieldShape,
                )
            }
            PromptSectionType.PERSONA_DESCRIPTION -> {
                Text(
                    text = persona?.let { "${it.name}: ${it.description}" }
                        ?: stringResource(R.string.ai_settings_no_persona_selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PromptSectionType.CHARACTER_DESCRIPTION -> {
                Text(
                    text = character?.let { "${it.name}: ${it.description}" }
                        ?: stringResource(R.string.ai_settings_no_character_selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            PromptSectionType.CHAT_HISTORY -> {
                Text(
                    text = stringResource(R.string.ai_settings_chat_history_inserted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
