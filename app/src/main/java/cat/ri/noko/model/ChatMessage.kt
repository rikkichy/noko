package cat.ri.noko.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SwipeAlternative(
    val content: String,
    val stoppedByUser: Boolean = false,
    val guardBlocked: Boolean = false,
    val guardReason: String? = null,
    val emojisTrimmed: Boolean = false,
    val actionsStructured: Boolean = false,
)

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
    val alternatives: List<SwipeAlternative> = emptyList(),
    val activeIndex: Int = 0,
) {
    @Serializable
    enum class Role { USER, ASSISTANT }

    val swipeCount: Int get() = alternatives.size.coerceAtLeast(1)

    fun toAlternative() = SwipeAlternative(
        content = content,
        stoppedByUser = stoppedByUser,
        guardBlocked = guardBlocked,
        guardReason = guardReason,
        emojisTrimmed = emojisTrimmed,
        actionsStructured = actionsStructured,
    )

    fun applyAlternative(index: Int): ChatMessage {
        if (index < 0 || index >= alternatives.size) return this
        val alt = alternatives[index]
        return copy(
            content = alt.content,
            stoppedByUser = alt.stoppedByUser,
            guardBlocked = alt.guardBlocked,
            guardReason = alt.guardReason,
            emojisTrimmed = alt.emojisTrimmed,
            actionsStructured = alt.actionsStructured,
            activeIndex = index,
        )
    }
}
