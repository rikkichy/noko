package cat.ri.noko.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import cat.ri.noko.model.NokcPayload
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CharacterCodec {

    private val json = Json { ignoreUnknownKeys = true }

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )
    private val NOKC_MAGIC = byteArrayOf(0x4E, 0x4F, 0x4B, 0x43)
    private const val NOKC_VERSION: Byte = 0x01
    private const val PBKDF2_ITERATIONS = 200_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_BITS = 128

    enum class CharacterFormat { TAVERN_PNG, CHARACTER_AI_JSON, NOKC, UNKNOWN }

    sealed class ImportResult {
        data class Success(
            val name: String,
            val description: String,
            val greetingMessage: String?,
            val avatarBytes: ByteArray?,
        ) : ImportResult()

        data class Error(val message: String) : ImportResult()
    }

    fun detectFormat(context: Context, uri: Uri): CharacterFormat {
        val bytes = context.contentResolver.openInputStream(uri)?.use {
            it.readNBytes(8)
        } ?: return CharacterFormat.UNKNOWN

        if (bytes.size >= 8 && bytes.sliceArray(0..7).contentEquals(PNG_SIGNATURE)) {
            return CharacterFormat.TAVERN_PNG
        }
        if (bytes.size >= 4 && bytes.sliceArray(0..3).contentEquals(NOKC_MAGIC)) {
            return CharacterFormat.NOKC
        }

        return try {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            } ?: return CharacterFormat.UNKNOWN
            val obj = json.parseToJsonElement(text).jsonObject
            if (obj.containsKey("definition") || obj.containsKey("title")) {
                CharacterFormat.CHARACTER_AI_JSON
            } else {
                CharacterFormat.UNKNOWN
            }
        } catch (_: Exception) {
            CharacterFormat.UNKNOWN
        }
    }

    fun importFromPng(context: Context, uri: Uri): ImportResult {
        return try {
            val allBytes = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes()
            } ?: return ImportResult.Error("Could not read file")

            val charaJson = extractCharaFromPng(allBytes)
                ?: return ImportResult.Error("No character data found in PNG")

            val root = json.parseToJsonElement(charaJson).jsonObject

            val isV2 = root.containsKey("data") &&
                root["data"] is JsonObject &&
                root["data"]!!.jsonObject.containsKey("name")

            if (isV2) {
                val data = root["data"]!!.jsonObject
                parseTavernData(data, allBytes)
            } else {
                parseTavernData(root, allBytes)
            }
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse PNG card: ${e.message}")
        }
    }

    fun importFromCharacterAiJson(context: Context, uri: Uri): ImportResult {
        return try {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            } ?: return ImportResult.Error("Could not read file")

            val obj = json.parseToJsonElement(text).jsonObject

            val name = obj.str("name")
                ?: return ImportResult.Error("Character has no name")

            val parts = mutableListOf<String>()
            obj.str("description")?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
            obj.str("definition")?.takeIf { it.isNotBlank() }?.let {
                parts.add("Definition:\n$it")
            }

            ImportResult.Success(
                name = name,
                description = parts.joinToString("\n\n"),
                greetingMessage = obj.str("greeting"),
                avatarBytes = null,
            )
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse Character.AI JSON: ${e.message}")
        }
    }

    fun importFromNokc(context: Context, uri: Uri, passphrase: String): ImportResult {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes()
            } ?: return ImportResult.Error("Could not read file")

            if (bytes.size < 5 + SALT_LENGTH + IV_LENGTH + 1) {
                return ImportResult.Error("File is too small")
            }
            if (!bytes.sliceArray(0..3).contentEquals(NOKC_MAGIC)) {
                return ImportResult.Error("Not a valid .nokc file")
            }
            if (bytes[4] != NOKC_VERSION) {
                return ImportResult.Error("Unsupported .nokc version")
            }

            val salt = bytes.sliceArray(5 until 5 + SALT_LENGTH)
            val iv = bytes.sliceArray(5 + SALT_LENGTH until 5 + SALT_LENGTH + IV_LENGTH)
            val ciphertext = bytes.sliceArray(5 + SALT_LENGTH + IV_LENGTH until bytes.size)

            val decrypted = try {
                decrypt(passphrase, salt, iv, ciphertext)
            } catch (_: Exception) {
                return ImportResult.Error("Wrong passphrase")
            }

            val payload = json.decodeFromString<NokcPayload>(decrypted.decodeToString())

            val avatarBytes = payload.avatarBase64?.let {
                Base64.decode(it, Base64.NO_WRAP)
            }

            ImportResult.Success(
                name = payload.entry.name,
                description = payload.entry.description,
                greetingMessage = payload.entry.greetingMessage,
                avatarBytes = avatarBytes,
            )
        } catch (e: Exception) {
            ImportResult.Error("Failed to import .nokc file: ${e.message}")
        }
    }

    fun exportToNokc(context: Context, entry: PersonaEntry, passphrase: String, outputUri: Uri) {
        val avatarBase64 = entry.avatarFileName?.let { fileName ->
            val file = AvatarStorage.getFile(context, fileName)
            if (file.exists()) Base64.encodeToString(file.readBytes(), Base64.NO_WRAP) else null
        }

        val exportEntry = entry.copy(avatarFileName = null)
        val payload = NokcPayload(entry = exportEntry, avatarBase64 = avatarBase64)
        val plaintext = json.encodeToString(payload).toByteArray()

        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

        val ciphertext = encrypt(passphrase, salt, iv, plaintext)

        context.contentResolver.openOutputStream(outputUri)?.use { out ->
            out.write(NOKC_MAGIC)
            out.write(byteArrayOf(NOKC_VERSION))
            out.write(salt)
            out.write(iv)
            out.write(ciphertext)
        }
    }

    private fun parseTavernData(data: JsonObject, pngBytes: ByteArray): ImportResult {
        val name = data.str("name")
            ?: return ImportResult.Error("Character card has no name")

        val parts = mutableListOf<String>()
        data.str("description")?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        data.str("personality")?.takeIf { it.isNotBlank() }?.let {
            parts.add("Personality: $it")
        }
        data.str("scenario")?.takeIf { it.isNotBlank() }?.let {
            parts.add("Scenario: $it")
        }

        return ImportResult.Success(
            name = name,
            description = parts.joinToString("\n\n"),
            greetingMessage = data.str("first_mes"),
            avatarBytes = pngBytes,
        )
    }

    private fun extractCharaFromPng(bytes: ByteArray): String? {
        if (bytes.size < 8 || !bytes.sliceArray(0..7).contentEquals(PNG_SIGNATURE)) {
            return null
        }

        val input = DataInputStream(bytes.inputStream())
        input.skipBytes(8)

        while (input.available() > 0) {
            val length = input.readInt()
            val typeBytes = ByteArray(4)
            input.readFully(typeBytes)
            val type = String(typeBytes, Charsets.ISO_8859_1)

            if (type == "tEXt" && length > 0) {
                val data = ByteArray(length)
                input.readFully(data)
                input.skipBytes(4)

                val nullIndex = data.indexOf(0.toByte())
                if (nullIndex > 0) {
                    val keyword = String(data, 0, nullIndex, Charsets.ISO_8859_1)
                    if (keyword == "chara") {
                        val value = String(data, nullIndex + 1, data.size - nullIndex - 1, Charsets.ISO_8859_1)
                        return String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8)
                    }
                }
            } else {
                if (length > 0) input.skipBytes(length)
                input.skipBytes(4)
            }

            if (type == "IEND") break
        }
        return null
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encrypt(passphrase: String, salt: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray {
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(plaintext)
    }

    private fun decrypt(passphrase: String, salt: ByteArray, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
}
