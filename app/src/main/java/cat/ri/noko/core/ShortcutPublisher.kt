package cat.ri.noko.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import cat.ri.noko.MainActivity
import cat.ri.noko.model.ChatSessionMeta
import cat.ri.noko.model.PersonaEntry
import cat.ri.noko.model.PersonaType

object ShortcutPublisher {

    const val EXTRA_CHARACTER_ID = "shortcut_character_id"

    fun update(context: Context, entries: List<PersonaEntry>, recentChats: List<ChatSessionMeta>) {
        val characters = entries.filter { it.type == PersonaType.CHARACTER }.associateBy { it.id }
        val topCharacterIds = recentChats
            .sortedByDescending { it.updatedAt }
            .map { it.characterId }
            .distinct()
            .filter { it in characters }
            .take(4)

        val shortcuts = topCharacterIds.mapIndexed { index, charId ->
            val character = characters[charId]!!
            ShortcutInfoCompat.Builder(context, "char_$charId")
                .setShortLabel(character.name)
                .setLongLabel("Chat with ${character.name}")
                .setIcon(loadIcon(context, character) ?: IconCompat.createWithResource(context, android.R.drawable.ic_menu_send))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra(EXTRA_CHARACTER_ID, charId)
                    }
                )
                .setRank(index)
                .build()
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    private fun loadIcon(context: Context, character: PersonaEntry): IconCompat? {
        val fileName = character.avatarFileName ?: return null
        val file = AvatarStorage.getFile(context, fileName)
        if (!file.exists()) return null
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val size = 108
        val cropped = circleCrop(bitmap, size)
        bitmap.recycle()
        return IconCompat.createWithAdaptiveBitmap(cropped)
    }

    private fun circleCrop(source: Bitmap, size: Int): Bitmap {
        val scaled = Bitmap.createScaledBitmap(source, size, size, true)
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, rect, rect, paint)
        if (scaled !== source) scaled.recycle()
        return output
    }
}
