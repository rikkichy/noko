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

    val swipeCount: Int get() = if (alternatives.isEmpty()) 1 else alternatives.size

    fun toAlternative() = SwipeAlternative(
        content = content,
        stoppedByUser = stoppedByUser,
        guardBlocked = guardBlocked,
        guardReason = guardReason,
        emojisTrimmed = emojisTrimmed,
        actionsStructured = actionsStructured,
    )

    fun swipeTo(targetIndex: Int): ChatMessage {
        if (alternatives.isEmpty() || targetIndex == activeIndex) return this
        if (targetIndex !in 0 until alternatives.size) return this
        val updatedAlts = alternatives.toMutableList()
        if (activeIndex in updatedAlts.indices) {
            updatedAlts[activeIndex] = toAlternative()
        }
        val target = updatedAlts[targetIndex]
        return copy(
            content = target.content,
            stoppedByUser = target.stoppedByUser,
            guardBlocked = target.guardBlocked,
            guardReason = target.guardReason,
            emojisTrimmed = target.emojisTrimmed,
            actionsStructured = target.actionsStructured,
            activeIndex = targetIndex,
            alternatives = updatedAlts,
        )
    }

    fun syncActiveAlternative(): ChatMessage {
        if (alternatives.isEmpty() || activeIndex !in alternatives.indices) return this
        val updatedAlts = alternatives.toMutableList()
        updatedAlts[activeIndex] = toAlternative()
        return copy(alternatives = updatedAlts)
    }

    fun addRegeneration(): ChatMessage {
        val updatedAlts = alternatives.toMutableList()
        if (activeIndex in updatedAlts.indices) {
            updatedAlts[activeIndex] = toAlternative()
        } else {
            updatedAlts.add(toAlternative())
        }
        val newIndex = updatedAlts.size
        updatedAlts.add(SwipeAlternative(content = ""))
        return copy(
            content = "",
            activeIndex = newIndex,
            alternatives = updatedAlts,
        )
    }
}
