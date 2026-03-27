package cat.ri.noko.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    val content: String,
    val isGreeting: Boolean = false,
    val stoppedByUser: Boolean = false,
    val guardBlocked: Boolean = false,
    val guardReason: String? = null,
    val emojisTrimmed: Boolean = false,
    val actionsStructured: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String? = null,
    val senderAvatarFileName: String? = null,
    val alternatives: List<ChatMessage> = emptyList(),
    val swipeIndex: Int = 0,
) {
    @Serializable
    enum class Role { USER, ASSISTANT }
}
