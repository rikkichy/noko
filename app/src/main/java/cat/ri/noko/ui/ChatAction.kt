package cat.ri.noko.ui

import cat.ri.noko.model.ChatSessionMeta

sealed interface ChatAction {
    data class NewChat(val isSecret: Boolean) : ChatAction
    data class OpenRecent(val meta: ChatSessionMeta) : ChatAction
}
