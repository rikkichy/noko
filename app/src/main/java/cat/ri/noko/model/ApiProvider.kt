package cat.ri.noko.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val requiresAuth: Boolean = true,
    val isLocal: Boolean = false,
    val urlEditable: Boolean = true,
)

val builtInProviders = listOf(
    ApiProvider(
        id = "openrouter",
        name = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1/",
        requiresAuth = true,
    ),
    ApiProvider(
        id = "openai",
        name = "OpenAI",
        baseUrl = "https://api.openai.com/v1/",
        requiresAuth = true,
        urlEditable = false,
    ),
    ApiProvider(
        id = "ollama",
        name = "Ollama",
        baseUrl = "http://localhost:11434/v1/",
        requiresAuth = false,
        isLocal = true,
    ),
    ApiProvider(
        id = "lmstudio",
        name = "LM Studio",
        baseUrl = "http://localhost:1234/v1/",
        requiresAuth = false,
        isLocal = true,
    ),
    ApiProvider(
        id = "koboldcpp",
        name = "KoboldCPP",
        baseUrl = "http://localhost:5001/v1/",
        requiresAuth = false,
        isLocal = true,
    ),
)

fun getProviderById(id: String): ApiProvider? =
    builtInProviders.find { it.id == id }
