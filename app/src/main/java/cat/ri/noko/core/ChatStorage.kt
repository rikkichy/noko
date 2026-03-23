package cat.ri.noko.core

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import cat.ri.noko.model.ChatMessage
import cat.ri.noko.model.ChatSessionMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object ChatStorage {

    private lateinit var appContext: Context
    private val json = Json { ignoreUnknownKeys = true }
    private val _recentChats = MutableStateFlow<List<ChatSessionMeta>>(emptyList())
    val recentChats: StateFlow<List<ChatSessionMeta>> = _recentChats.asStateFlow()

    private val masterKeyAlias by lazy {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        chatsDir().mkdirs()
        _recentChats.value = loadIndex()
    }

    private fun chatsDir(): File = File(appContext.filesDir, "chats")

    private fun indexFile(): File = File(chatsDir(), "index.enc")

    private fun chatFile(chatId: String): File = File(chatsDir(), "$chatId.enc")


    private fun loadIndex(): List<ChatSessionMeta> {
        val file = indexFile()
        if (!file.exists()) return emptyList()
        return try {
            val bytes = readEncrypted(file)
            if (bytes != null) {
                json.decodeFromString<List<ChatSessionMeta>>(bytes.decodeToString())
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveIndex(chats: List<ChatSessionMeta>) {
        val data = json.encodeToString(chats).toByteArray()
        writeEncrypted(indexFile(), data)
    }


    suspend fun saveChat(
        chatId: String,
        messages: List<ChatMessage>,
        meta: ChatSessionMeta,
    ) = withContext(Dispatchers.IO) {

        val data = json.encodeToString(messages).toByteArray()
        writeEncrypted(chatFile(chatId), data)


        val current = _recentChats.value.filterNot { it.id == meta.id }
        val updated = (current + meta)
            .sortedByDescending { it.updatedAt }
            .take(50)
        saveIndex(updated)
        _recentChats.value = updated
    }

    suspend fun loadChat(chatId: String): List<ChatMessage>? = withContext(Dispatchers.IO) {
        val file = chatFile(chatId)
        if (!file.exists()) return@withContext null
        try {
            val bytes = readEncrypted(file)
            if (bytes != null) {
                json.decodeFromString<List<ChatMessage>>(bytes.decodeToString())
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun deleteChat(chatId: String) = withContext(Dispatchers.IO) {
        chatFile(chatId).delete()
        val updated = _recentChats.value.filterNot { it.id == chatId }
        saveIndex(updated)
        _recentChats.value = updated
    }


    private fun encryptedFile(file: File): EncryptedFile =
        EncryptedFile.Builder(
            file,
            appContext,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build()

    private fun writeEncrypted(file: File, bytes: ByteArray) {

        if (file.exists()) file.delete()
        encryptedFile(file).openFileOutput().use { it.write(bytes) }
    }

    private fun readEncrypted(file: File): ByteArray? {
        if (!file.exists()) return null
        return encryptedFile(file).openFileInput().use { it.readBytes() }
    }
}
