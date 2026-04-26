package cat.ri.noko.core.api

import cat.ri.noko.BuildConfig
import cat.ri.noko.core.ThinkTagStripper
import cat.ri.noko.model.api.ChatRequest
import cat.ri.noko.model.api.ModelsResponse
import cat.ri.noko.model.api.StreamChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming
import java.util.concurrent.TimeUnit

private interface ChatApi {
    @GET("models")
    suspend fun getModels(): ModelsResponse

    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: ChatRequest): cat.ri.noko.model.api.ChatResponse

    @Streaming
    @POST("chat/completions")
    fun chatCompletionStream(@Body request: ChatRequest): Call<ResponseBody>
}

private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

object ApiClient {

    private var apiKey: String = ""
    private var currentBaseUrl: String = ""
    private var currentProviderId: String = ""
    private var api: ChatApi? = null
    private var okHttpClient: OkHttpClient? = null

    fun configure(key: String, baseUrl: String, providerId: String) {
        if (key == apiKey && baseUrl == currentBaseUrl && providerId == currentProviderId && api != null) return
        apiKey = key
        currentBaseUrl = baseUrl
        currentProviderId = providerId

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                if (apiKey.isNotBlank()) {
                    builder.addHeader("Authorization", "Bearer $apiKey")
                }
                if (providerId == "openrouter") {
                    builder.addHeader("X-Title", "Noko")
                }
                chain.proceed(builder.build())
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        okHttpClient = client
        api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ChatApi::class.java)
    }

    fun evictConnectionPool() {
        okHttpClient?.connectionPool?.evictAll()
    }

    val isConfigured: Boolean get() = currentBaseUrl.isNotBlank() && api != null

    suspend fun getModels(): ModelsResponse {
        requireConfigured()
        return api!!.getModels()
    }

    suspend fun validateConnection() {
        requireConfigured()
        api!!.getModels()
    }

    fun streamChat(request: ChatRequest): Flow<StreamEvent> = callbackFlow {
        requireConfigured()
        val streamRequest = request.copy(stream = true)

        val call = api!!.chatCompletionStream(streamRequest)
        val stripper = ThinkTagStripper()

        launch(Dispatchers.IO) {
            try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    close(ApiException(humanizeHttpError(response.code())))
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    close(ApiException("No response from server"))
                    return@launch
                }

                val source = body.source()
                try {
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: continue
                        if (!line.startsWith("data: ")) continue
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        if (data.isEmpty()) continue

                        try {
                            val chunk = json.decodeFromString<StreamChunk>(data)
                            chunk.error?.let { error ->
                                close(ApiException(humanizeApiError(error.code, error.message)))
                                return@launch
                            }
                            val delta = chunk.choices.firstOrNull()?.delta ?: continue
                            val explicitReasoning = delta.reasoningContent
                                ?: delta.reasoning
                                ?: delta.thinking
                            if (!explicitReasoning.isNullOrEmpty()) {
                                trySend(StreamEvent.Reasoning(explicitReasoning))
                            }
                            val content = delta.content
                            if (!content.isNullOrEmpty()) {
                                val parsed = stripper.feed(content)
                                if (parsed.reasoning.isNotEmpty()) {
                                    trySend(StreamEvent.Reasoning(parsed.reasoning))
                                }
                                if (parsed.content.isNotEmpty()) {
                                    trySend(StreamEvent.Content(parsed.content))
                                }
                            }
                        } catch (e: ApiException) {
                            close(e)
                            return@launch
                        } catch (_: Exception) {

                        }
                    }
                    val tail = stripper.flush()
                    if (tail.reasoning.isNotEmpty()) trySend(StreamEvent.Reasoning(tail.reasoning))
                    if (tail.content.isNotEmpty()) trySend(StreamEvent.Content(tail.content))
                } finally {
                    body.close()
                }
                channel.close()
            } catch (e: java.io.IOException) {
                channel.close(e)
            }
        }

        awaitClose { call.cancel() }
    }

    private fun requireConfigured() {
        check(isConfigured) { "ApiClient not configured. Select a provider first." }
    }
}

private fun humanizeHttpError(code: Int): String = when (code) {
    401 -> "Invalid API key"
    402 -> "Insufficient credits"
    403 -> "Access denied"
    408 -> "Request timed out"
    429 -> "Too many requests — slow down"
    500, 502, 503 -> "Server is having issues, try again later"
    in 400..499 -> "Request error ($code)"
    in 500..599 -> "Server error ($code)"
    else -> "Unexpected error ($code)"
}

private fun humanizeApiError(code: Int?, message: String?): String = when {
    code == 401 || message?.contains("key", ignoreCase = true) == true -> "Invalid API key"
    code == 402 || message?.contains("credit", ignoreCase = true) == true -> "Insufficient credits"
    code == 429 || message?.contains("rate", ignoreCase = true) == true -> "Too many requests — slow down"
    message?.contains("context", ignoreCase = true) == true -> "Message too long for this model"
    message?.contains("moderation", ignoreCase = true) == true -> "Content was flagged by moderation"
    else -> "API error"
}

fun humanizeException(e: Throwable): String = when (e) {
    is ApiException -> e.message ?: "API error"
    is java.net.UnknownHostException -> "No internet connection"
    is java.net.ConnectException -> "Could not reach server"
    is java.net.SocketTimeoutException -> "Connection timed out"
    is javax.net.ssl.SSLException -> "Secure connection failed"
    is java.io.IOException -> "Network error"
    else -> "Something went wrong"
}

class ApiException(message: String) : Exception(message)

sealed class StreamEvent {
    data class Content(val text: String) : StreamEvent()
    data class Reasoning(val text: String) : StreamEvent()
}
