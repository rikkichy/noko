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
    val timestamp: Long = System.currentTimeMillis(),
) {
    @Serializable
    enum class Role { USER, ASSISTANT }
}
