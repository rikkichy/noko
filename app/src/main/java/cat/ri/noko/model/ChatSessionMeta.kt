package cat.ri.noko.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatSessionMeta(
    val id: String,
    val characterId: String,
    val characterName: String,
    val characterAvatarFileName: String? = null,
    val lastMessagePreview: String,
    val lastMessageRole: String,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val personaName: String? = null,
    val personaAvatarFileName: String? = null,
    val pinned: Boolean = false,
)
