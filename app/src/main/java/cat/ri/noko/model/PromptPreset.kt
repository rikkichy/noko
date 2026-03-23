package cat.ri.noko.model

import kotlinx.serialization.Serializable

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

fun defaultPromptPreset(): PromptPreset = PromptPreset()
