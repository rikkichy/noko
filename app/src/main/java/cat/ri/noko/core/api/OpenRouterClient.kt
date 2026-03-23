package cat.ri.noko.core.api

import cat.ri.noko.BuildConfig
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

private const val BASE_URL = "https://openrouter.ai/api/v1/"

private interface OpenRouterApi {
    @GET("models")
    suspend fun getModels(): ModelsResponse

    @GET("key")
    suspend fun validateKey(): ResponseBody

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

object OpenRouterClient {

    private var apiKey: String = ""
    private var api: OpenRouterApi? = null

    fun configure(key: String) {
        if (key == apiKey && api != null) return
        apiKey = key

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("X-Title", "Noko")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenRouterApi::class.java)
    }

    val isConfigured: Boolean get() = apiKey.isNotBlank() && api != null

    suspend fun getModels(): ModelsResponse {
        requireConfigured()
        return api!!.getModels()
    }


    suspend fun validateKey() {
        requireConfigured()
        api!!.validateKey()
    }

    fun streamChat(request: ChatRequest): Flow<String> = callbackFlow {
        requireConfigured()
        val streamRequest = request.copy(stream = true)

        val call = api!!.chatCompletionStream(streamRequest)

        launch(Dispatchers.IO) {
            try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    close(OpenRouterException(humanizeHttpError(response.code())))
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    close(OpenRouterException("No response from server"))
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
                                close(OpenRouterException(humanizeApiError(error.code, error.message)))
                                return@launch
                            }
                            val content = chunk.choices.firstOrNull()?.delta?.content
                            if (content != null) {
                                trySend(content)
                            }
                        } catch (e: OpenRouterException) {
                            close(e)
                            return@launch
                        } catch (_: Exception) {

                        }
                    }
                } finally {
                    body.close()
                }
                channel.close()
            } catch (_: java.io.IOException) {

                channel.close()
            }
        }

        awaitClose { call.cancel() }
    }

    private fun requireConfigured() {
        check(isConfigured) { "OpenRouterClient not configured. Set API key first." }
    }
}

private fun humanizeHttpError(code: Int): String = when (code) {
    401 -> "Invalid API key"
    402 -> "Insufficient credits on OpenRouter"
    403 -> "Access denied by OpenRouter"
    408 -> "Request timed out"
    429 -> "Too many requests — slow down"
    500, 502, 503 -> "OpenRouter is having issues, try again later"
    in 400..499 -> "Request error ($code)"
    in 500..599 -> "Server error ($code)"
    else -> "Unexpected error ($code)"
}

private fun humanizeApiError(code: Int?, message: String?): String = when {
    code == 401 || message?.contains("key", ignoreCase = true) == true -> "Invalid API key"
    code == 402 || message?.contains("credit", ignoreCase = true) == true -> "Insufficient credits on OpenRouter"
    code == 429 || message?.contains("rate", ignoreCase = true) == true -> "Too many requests — slow down"
    message?.contains("context", ignoreCase = true) == true -> "Message too long for this model"
    message?.contains("moderation", ignoreCase = true) == true -> "Content was flagged by moderation"
    else -> "OpenRouter error"
}

fun humanizeException(e: Throwable): String = when (e) {
    is OpenRouterException -> e.message ?: "OpenRouter error"
    is java.net.UnknownHostException -> "No internet connection"
    is java.net.ConnectException -> "Could not reach OpenRouter"
    is java.net.SocketTimeoutException -> "Connection timed out"
    is javax.net.ssl.SSLException -> "Secure connection failed"
    is java.io.IOException -> "Network error"
    else -> "Something went wrong"
}

class OpenRouterException(message: String) : Exception(message)
