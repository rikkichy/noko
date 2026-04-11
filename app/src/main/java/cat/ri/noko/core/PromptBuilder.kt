package cat.ri.noko.core

import cat.ri.noko.model.ChatMessage
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PromptPreset
import cat.ri.noko.model.PromptSectionType
import cat.ri.noko.model.api.ChatRequestMessage

fun String.replaceTemplateVars(charName: String, userName: String): String =
    replace("{{char}}", charName, ignoreCase = true)
        .replace("{{user}}", userName, ignoreCase = true)
        .replace("{char}", charName, ignoreCase = true)
        .replace("{user}", userName, ignoreCase = true)

object PromptBuilder {

    fun buildMessages(
        preset: PromptPreset,
        persona: PersonaEntry?,
        character: PersonaEntry?,
        chatMessages: List<ChatMessage>,
        continueNudge: String? = null,
    ): List<ChatRequestMessage> {
        val userName = persona?.name ?: "User"
        val charName = character?.name ?: "Assistant"

        val enabled = preset.sections.filter { it.enabled }
        val chatHistoryIdx = enabled.indexOfFirst { it.type == PromptSectionType.CHAT_HISTORY }
        val systemSections = if (chatHistoryIdx >= 0) enabled.subList(0, chatHistoryIdx) else enabled

        return buildList {
            val systemParts = systemSections.mapNotNull { section ->
                when (section.type) {
                    PromptSectionType.MAIN_PROMPT -> {
                        section.content?.replaceTemplateVars(charName, userName)
                    }
                    PromptSectionType.PERSONA_DESCRIPTION -> {
                        persona?.let {
                            "${it.name}: ${it.description}".replaceTemplateVars(charName, userName)
                        }
                    }
                    PromptSectionType.CHARACTER_DESCRIPTION -> {
                        character?.let {
                            "${it.name}: ${it.description}".replaceTemplateVars(charName, userName)
                        }
                    }
                    PromptSectionType.CHAT_HISTORY -> null
                }
            }

            if (systemParts.isNotEmpty()) {
                add(ChatRequestMessage(role = "system", content = systemParts.joinToString("\n\n")))
            }

            if (chatHistoryIdx >= 0) {
                chatMessages.forEach { msg ->
                    add(
                        ChatRequestMessage(
                            role = when (msg.role) {
                                ChatMessage.Role.USER -> "user"
                                ChatMessage.Role.ASSISTANT -> "assistant"
                            },
                            content = msg.content,
                        ),
                    )
                }
            }

            if (continueNudge != null) {
                add(ChatRequestMessage(role = "user", content = continueNudge))
            }
        }
    }
}
