package cat.ri.noko.core

import android.content.Context
import android.graphics.Bitmap
import java.io.File

object AvatarStorage {

    fun save(context: Context, bitmap: Bitmap, id: String): String {
        context.filesDir.listFiles()
            ?.filter { it.name.startsWith("avatar_$id") && it.name.endsWith(".jpg") }
            ?.forEach { it.delete() }
        val fileName = "avatar_${id}_${System.currentTimeMillis()}.jpg"
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return fileName
    }

    fun saveBytes(context: Context, bytes: ByteArray, id: String): String {
        context.filesDir.listFiles()
            ?.filter { it.name.startsWith("avatar_$id") && it.name.endsWith(".jpg") }
            ?.forEach { it.delete() }
        val fileName = "avatar_${id}_${System.currentTimeMillis()}.jpg"
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use { out ->
            out.write(bytes)
        }
        return fileName
    }

    fun getFile(context: Context, fileName: String): File {
        requireSafeFileName(fileName)
        return File(context.filesDir, fileName)
    }

    fun delete(context: Context, fileName: String) {
        requireSafeFileName(fileName)
        File(context.filesDir, fileName).delete()
    }

    private fun requireSafeFileName(fileName: String) {
        require(!fileName.contains("..") && !fileName.contains(File.separator)) {
            "Invalid avatar filename"
        }
    }
}
