package cat.ri.noko.core

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom

object AvatarStorage {

    suspend fun save(context: Context, bitmap: Bitmap, id: String): String =
        withContext(Dispatchers.IO) {
            context.filesDir.listFiles()
                ?.filter { it.name.startsWith("avatar_$id") && it.name.endsWith(".jpg") }
                ?.forEach { secureDelete(it) }
            val fileName = "avatar_${id}_${System.currentTimeMillis()}.jpg"
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            fileName
        }

    suspend fun saveBytes(context: Context, bytes: ByteArray, id: String): String =
        withContext(Dispatchers.IO) {
            context.filesDir.listFiles()
                ?.filter { it.name.startsWith("avatar_$id") && it.name.endsWith(".jpg") }
                ?.forEach { secureDelete(it) }
            val fileName = "avatar_${id}_${System.currentTimeMillis()}.jpg"
            context.openFileOutput(fileName, Context.MODE_PRIVATE).use { out ->
                out.write(bytes)
            }
            fileName
        }

    fun getFile(context: Context, fileName: String): File {
        requireSafeFileName(fileName)
        return File(context.filesDir, fileName)
    }

    suspend fun delete(context: Context, fileName: String) = withContext(Dispatchers.IO) {
        requireSafeFileName(fileName)
        secureDelete(File(context.filesDir, fileName))
    }

    private fun secureDelete(file: File) {
        if (!file.exists()) return
        try {
            val length = file.length()
            if (length > 0) {
                val random = SecureRandom()
                file.outputStream().use { out ->
                    val buf = ByteArray(4096)
                    var remaining = length
                    while (remaining > 0) {
                        random.nextBytes(buf)
                        val toWrite = minOf(buf.size.toLong(), remaining).toInt()
                        out.write(buf, 0, toWrite)
                        remaining -= toWrite
                    }
                }
            }
        } catch (_: Exception) {
        }
        file.delete()
    }

    private fun requireSafeFileName(fileName: String) {
        require(!fileName.contains("..") && !fileName.contains(File.separator)) {
            "Invalid avatar filename"
        }
    }
}
