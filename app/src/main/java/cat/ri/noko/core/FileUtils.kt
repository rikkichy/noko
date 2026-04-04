package cat.ri.noko.core

import java.io.File
import java.security.SecureRandom

internal fun secureDelete(file: File) {
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
