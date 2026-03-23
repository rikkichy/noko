package cat.ri.noko.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelsResponse(
    val data: List<ModelInfo> = emptyList(),
)

@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("context_length") val contextLength: Int? = null,
    val pricing: ModelPricing? = null,
)

@Serializable
data class ModelPricing(
    val prompt: String? = null,
    val completion: String? = null,
)
