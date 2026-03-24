package cat.ri.noko.model

import kotlinx.serialization.Serializable

@Serializable
data class NokcCharacter(
    val entry: PersonaEntry,
    val avatarBase64: String? = null,
)

@Serializable
data class NokcPayload(
    val characters: List<NokcCharacter>,
)

@Serializable
data class NokcPayloadLegacy(
    val entry: PersonaEntry,
    val avatarBase64: String? = null,
)
