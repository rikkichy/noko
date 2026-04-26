package cat.ri.noko.model.api

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
data class ChatChoice(
    val message: ChatResponseMessage? = null,
)

@Serializable
data class ChatResponseMessage(
    val role: String? = null,
    val content: String? = null,
)

@Serializable
data class StreamChunk(
    val choices: List<StreamChoice> = emptyList(),
    val error: StreamError? = null,
)

@Serializable
data class StreamError(
    val message: String? = null,
    val code: Int? = null,
)

@Serializable
data class StreamChoice(
    val delta: StreamDelta? = null,
)

@Serializable
data class StreamDelta(
    val content: String? = null,
    val role: String? = null,
    @kotlinx.serialization.SerialName("reasoning_content")
    val reasoningContent: String? = null,
    val reasoning: String? = null,
    val thinking: String? = null,
)
