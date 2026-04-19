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

    /**
     * When the current (blank) regeneration was cancelled, drop the empty branch and
     * fall back to the most recent non-blank alternative in the tree. Returns `null`
     * if there is no prior response to return to — callers should then remove the
     * message entirely rather than leaving an empty stopped bubble in the chat.
     */
    fun recoverFromBlankRegeneration(): ChatMessage? {
        if (content.isNotBlank()) return this
        val withoutCurrent = alternatives.filterIndexed { i, _ -> i != activeIndex }
        val fallbackIdx = withoutCurrent.indexOfLast { it.content.isNotBlank() }
        if (fallbackIdx < 0) return null
        val target = withoutCurrent[fallbackIdx]
        // Collapse back to the fresh no-branch shape when only one branch remains.
        val finalAlts = if (withoutCurrent.size == 1) emptyList() else withoutCurrent
        val finalIdx = if (finalAlts.isEmpty()) 0 else fallbackIdx
        return copy(
            content = target.content,
            stoppedByUser = target.stoppedByUser,
            guardBlocked = target.guardBlocked,
            guardReason = target.guardReason,
            emojisTrimmed = target.emojisTrimmed,
            actionsStructured = target.actionsStructured,
            activeIndex = finalIdx,
            alternatives = finalAlts,
        )
    }

    fun addRegeneration(): ChatMessage {
        val updatedAlts = alternatives.toMutableList()
        if (content.isNotBlank()) {
            if (activeIndex in updatedAlts.indices) {
                updatedAlts[activeIndex] = toAlternative()
            } else {
                updatedAlts.add(toAlternative())
            }
        } else if (activeIndex in updatedAlts.indices) {
            updatedAlts.removeAt(activeIndex)
        }
        val pruned = updatedAlts.filter { it.content.isNotBlank() }.toMutableList()
        val newIndex = pruned.size
        pruned.add(SwipeAlternative(content = ""))
        return copy(
            content = "",
            activeIndex = newIndex,
            alternatives = pruned,
        )
    }
}
