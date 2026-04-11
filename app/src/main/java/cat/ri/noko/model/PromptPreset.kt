package cat.ri.noko.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

const val DEFAULT_CONTINUE_NUDGE = "Continue your last message without repeating its original content."

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
    @SerialName("continue_nudge_prompt") val continueNudgePrompt: String = DEFAULT_CONTINUE_NUDGE,
)

fun defaultSections(): List<PromptSection> = listOf(
    PromptSection(
        type = PromptSectionType.MAIN_PROMPT,
        content = "Write {{char}}'s next reply in a fictional chat between {{char}} and {{user}}. Write one reply only. Do not write as {{user}}.",
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
        sections = listOf(
            PromptSection(
                type = PromptSectionType.MAIN_PROMPT,
                content = "Write {{char}}'s next reply in a fictional chat between {{char}} and {{user}}. Write one reply only. Do not write as {{user}}.",
            ),
            PromptSection(type = PromptSectionType.PERSONA_DESCRIPTION),
            PromptSection(type = PromptSectionType.CHARACTER_DESCRIPTION),
            PromptSection(type = PromptSectionType.CHAT_HISTORY),
        ),
    ),
    PromptPreset(
        id = "balanced_rp",
        name = "Balanced RP",
        builtIn = true,
        temperature = 0.75f,
        topP = 0.92f,
        maxTokens = 300,
        frequencyPenalty = 0.1f,
        sections = listOf(
            PromptSection(
                type = PromptSectionType.MAIN_PROMPT,
                content = "Write {{char}}'s next reply in a fictional roleplay between {{char}} and {{user}}. Write one reply only. Do not write as {{user}}. Be descriptive and creative with actions, using *asterisks* for actions and \"quotes\" for dialogue. Stay in character at all times.",
            ),
            PromptSection(type = PromptSectionType.PERSONA_DESCRIPTION),
            PromptSection(type = PromptSectionType.CHARACTER_DESCRIPTION),
            PromptSection(type = PromptSectionType.CHAT_HISTORY),
        ),
    ),
    PromptPreset(
        id = "creative_rp",
        name = "Creative RP",
        builtIn = true,
        temperature = 1.0f,
        topP = 0.95f,
        maxTokens = 400,
        frequencyPenalty = 0.3f,
        presencePenalty = 0.3f,
        sections = listOf(
            PromptSection(
                type = PromptSectionType.MAIN_PROMPT,
                content = "You are a skilled author collaborating with {{user}} on an immersive interactive story. Give voice to {{char}} with vivid, dynamic prose. Write one reply only, at least one paragraph up to four. Use *asterisks* for actions and narrative, \"quotes\" for dialogue. Describe {{char}}'s appearance, thoughts, and actions thoroughly. Never write as {{user}} or narrate their actions.",
            ),
            PromptSection(type = PromptSectionType.PERSONA_DESCRIPTION),
            PromptSection(type = PromptSectionType.CHARACTER_DESCRIPTION),
            PromptSection(type = PromptSectionType.CHAT_HISTORY),
        ),
    ),
    PromptPreset(
        id = "expressive",
        name = "Expressive",
        builtIn = true,
        temperature = 1.3f,
        topP = 0.9f,
        topK = 60,
        maxTokens = 500,
        frequencyPenalty = 0.5f,
        presencePenalty = 0.2f,
        sections = listOf(
            PromptSection(
                type = PromptSectionType.MAIN_PROMPT,
                content = "You are a masterful storyteller giving voice to {{char}} in a rich, immersive narrative with {{user}}. Write with literary flair \u2014 use evocative descriptions, varied sentence structure, and strong emotional undercurrents. Describe body language, micro-expressions, inner thoughts, and sensory details. Use *asterisks* for narrative and \"quotes\" for dialogue. Aim for two to four paragraphs per reply. Never write as {{user}}. End at natural story beats rather than with forced questions.",
            ),
            PromptSection(type = PromptSectionType.PERSONA_DESCRIPTION),
            PromptSection(type = PromptSectionType.CHARACTER_DESCRIPTION),
            PromptSection(type = PromptSectionType.CHAT_HISTORY),
        ),
    ),
    PromptPreset(
        id = "concise",
        name = "Concise",
        builtIn = true,
        temperature = 0.65f,
        topP = 0.9f,
        maxTokens = 200,
        sections = listOf(
            PromptSection(
                type = PromptSectionType.MAIN_PROMPT,
                content = "Write {{char}}'s next reply in a fictional chat between {{char}} and {{user}}. Write one reply only. Keep responses short and punchy \u2014 one to two paragraphs max. Focus on dialogue and brief actions. Use *asterisks* for actions and \"quotes\" for speech. Do not write as {{user}}. Stay in character.",
            ),
            PromptSection(type = PromptSectionType.PERSONA_DESCRIPTION),
            PromptSection(type = PromptSectionType.CHARACTER_DESCRIPTION),
            PromptSection(type = PromptSectionType.CHAT_HISTORY),
        ),
    ),
)

fun defaultPromptPreset(): PromptPreset = builtInPresets().first()

fun PromptPreset.duplicate(): PromptPreset = copy(
    id = UUID.randomUUID().toString(),
    name = "Copy of $name",
    builtIn = false,
)
