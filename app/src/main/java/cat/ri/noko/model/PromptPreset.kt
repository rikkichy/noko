package cat.ri.noko.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class PromptSectionType {
    MAIN_PROMPT,
    PERSONA_DESCRIPTION,
    CHARACTER_DESCRIPTION,
    CHAT_HISTORY,
}

@Serializable
data class PromptSection(
    val type: PromptSectionType,
    val enabled: Boolean = true,
    val content: String? = null,
)

@Serializable
data class PromptPreset(
    val id: String = "default",
    val name: String = "Default",
    val sections: List<PromptSection> = defaultSections(),
    val builtIn: Boolean = false,
    val temperature: Float? = null,
    @SerialName("top_p") val topP: Float? = null,
    @SerialName("top_k") val topK: Int? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("frequency_penalty") val frequencyPenalty: Float? = null,
    @SerialName("presence_penalty") val presencePenalty: Float? = null,
)

fun defaultSections(): List<PromptSection> = listOf(
    PromptSection(
        type = PromptSectionType.MAIN_PROMPT,
        content = "Write {{char}}'s next reply in a fictional chat between {{char}} and {{user}}.",
    ),
    PromptSection(type = PromptSectionType.PERSONA_DESCRIPTION),
    PromptSection(type = PromptSectionType.CHARACTER_DESCRIPTION),
    PromptSection(type = PromptSectionType.CHAT_HISTORY),
)

fun builtInPresets(): List<PromptPreset> = listOf(
    PromptPreset(
        id = "default",
        name = "Default",
        builtIn = true,
    ),
    PromptPreset(
        id = "balanced_rp",
        name = "Balanced RP",
        builtIn = true,
        temperature = 0.7f,
        topP = 0.92f,
        maxTokens = 300,
    ),
    PromptPreset(
        id = "creative_rp",
        name = "Creative RP",
        builtIn = true,
        temperature = 1.0f,
        topP = 0.95f,
        maxTokens = 350,
        frequencyPenalty = 0.3f,
        presencePenalty = 0.3f,
    ),
    PromptPreset(
        id = "expressive",
        name = "Expressive",
        builtIn = true,
        temperature = 1.3f,
        topP = 0.9f,
        topK = 60,
        maxTokens = 350,
        frequencyPenalty = 0.5f,
        presencePenalty = 0.2f,
    ),
    PromptPreset(
        id = "coherent",
        name = "Coherent",
        builtIn = true,
        temperature = 0.63f,
        topP = 0.98f,
        maxTokens = 200,
    ),
    PromptPreset(
        id = "deterministic",
        name = "Deterministic",
        builtIn = true,
        temperature = 0.0f,
        topP = 0.1f,
        topK = 1,
        frequencyPenalty = 0.0f,
        presencePenalty = 0.0f,
    ),
)

fun defaultPromptPreset(): PromptPreset = builtInPresets().first()

fun PromptPreset.duplicate(): PromptPreset = copy(
    id = UUID.randomUUID().toString(),
    name = "Copy of $name",
    builtIn = false,
)
