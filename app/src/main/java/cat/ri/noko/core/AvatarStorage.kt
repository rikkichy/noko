package cat.ri.noko.core

import android.content.Context
import android.graphics.Bitmap
import java.io.File

object AvatarStorage {

    fun save(context: Context, bitmap: Bitmap, id: String): String {
        val fileName = "avatar_$id.jpg"
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return fileName
    }

    fun getFile(context: Context, fileName: String): File =
        File(context.filesDir, fileName)

    fun delete(context: Context, fileName: String) {
        File(context.filesDir, fileName).delete()
    }
}
