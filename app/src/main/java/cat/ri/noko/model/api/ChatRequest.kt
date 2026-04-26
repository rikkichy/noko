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
    @SerialName("top_p") val topP: Float? = null,
    @SerialName("top_k") val topK: Int? = null,
    @SerialName("frequency_penalty") val frequencyPenalty: Float? = null,
    @SerialName("presence_penalty") val presencePenalty: Float? = null,
    val reasoning: ReasoningParam? = null,
)

@Serializable
data class ReasoningParam(
    val enabled: Boolean = true,
)

@Serializable
data class ChatRequestMessage(
    val role: String,
    val content: String,
)
