package cat.ri.noko.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatRequestMessage>,
    val stream: Boolean = false,
    val temperature: Float? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
)

@Serializable
data class ChatRequestMessage(
    val role: String,
    val content: String,
)
