package cat.ri.noko.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.ri.noko.core.SettingsManager
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PromptPreset
import cat.ri.noko.model.PromptSection
import cat.ri.noko.model.PromptSectionType
import cat.ri.noko.model.defaultPromptPreset
import cat.ri.noko.ui.util.rememberNokoHaptics
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptBuilderScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptics = rememberNokoHaptics()

    val presets by SettingsManager.promptPresets.collectAsState(initial = listOf(defaultPromptPreset()))
    val selectedPresetId by SettingsManager.selectedPresetId.collectAsState(initial = "default")
    val activePreset = remember(presets, selectedPresetId) {
        presets.find { it.id == selectedPresetId } ?: presets.first()
    }

    var sections by remember(activePreset) { mutableStateOf(activePreset.sections) }

    val allEntries by SettingsManager.allEntries.collectAsState(initial = emptyList())
    val selectedPersonaId by SettingsManager.selectedPersonaId.collectAsState(initial = null)
    val selectedCharacterId by SettingsManager.selectedCharacterId.collectAsState(initial = null)
    val activePersona = remember(allEntries, selectedPersonaId) {
        selectedPersonaId?.let { id -> allEntries.find { it.id == id } }
    }
    val activeCharacter = remember(allEntries, selectedCharacterId) {
        selectedCharacterId?.let { id -> allEntries.find { it.id == id } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prompt Builder") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptics.confirm()
                        scope.launch {
                            SettingsManager.savePreset(activePreset.copy(sections = sections))
                            onBack()
                        }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            sections.forEachIndexed { index, section ->
                PromptSectionCard(
                    section = section,
                    persona = activePersona,
                    character = activeCharacter,
                    onToggle = { enabled ->
                        haptics.toggle()
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

@Composable
private fun PromptSectionCard(
    section: PromptSection,
    persona: PersonaEntry?,
    character: PersonaEntry?,
    onToggle: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = when (section.type) {
                        PromptSectionType.MAIN_PROMPT -> "Main Prompt"
                        PromptSectionType.PERSONA_DESCRIPTION -> "Persona Description"
                        PromptSectionType.CHARACTER_DESCRIPTION -> "Character Description"
                        PromptSectionType.CHAT_HISTORY -> "Chat History"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
                if (section.type != PromptSectionType.CHAT_HISTORY) {
                    Switch(checked = section.enabled, onCheckedChange = onToggle)
                }
            }

            when (section.type) {
                PromptSectionType.MAIN_PROMPT -> {
                    OutlinedTextField(
                        value = section.content ?: "",
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = section.enabled,
                        minLines = 2,
                    )
                }
                PromptSectionType.PERSONA_DESCRIPTION -> {
                    Text(
                        text = persona?.let { "${it.name}: ${it.description}" }
                            ?: "No persona selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PromptSectionType.CHARACTER_DESCRIPTION -> {
                    Text(
                        text = character?.let { "${it.name}: ${it.description}" }
                            ?: "No character selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                PromptSectionType.CHAT_HISTORY -> {
                    Text(
                        text = "Chat messages will be inserted here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
