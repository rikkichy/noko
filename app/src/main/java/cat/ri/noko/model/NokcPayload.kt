package cat.ri.noko.model

import kotlinx.serialization.Serializable

@Serializable
data class NokcPayload(
    val entry: PersonaEntry,
    val avatarBase64: String? = null,
)
