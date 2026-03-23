package cat.ri.noko.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class PersonaType { PERSONA, CHARACTER }

@Serializable
data class PersonaEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: PersonaType,
    val name: String,
    val description: String,
    val greetingMessage: String? = null,
    val avatarFileName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
