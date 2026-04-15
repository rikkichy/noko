package cat.ri.noko.ui.util

import cat.ri.noko.core.HallucinationDetector

private val emojiInvisibles = setOf(
    0x200D,          // Zero-Width Joiner
    0xFE0E, 0xFE0F, // Variation Selectors 15-16
)

private fun isVisibleEmoji(codePoint: Int): Boolean =
    HallucinationDetector.isEmoji(codePoint) && codePoint !in emojiInvisibles

internal fun stripEmojis(text: String): String {
    val sb = StringBuilder()
    var prevWasSpace = false
    val codePoints = text.codePoints().toArray()
    var i = 0
    while (i < codePoints.size) {
        val cp = codePoints[i]
        if (isVisibleEmoji(cp)) {
            // Skip the emoji and any trailing invisible joiners/selectors
            i++
            while (i < codePoints.size && codePoints[i] in emojiInvisibles) i++
            continue
        }
        if (cp in emojiInvisibles) {
            // Orphan invisible — only skip if not adjacent to visible text
            i++
            continue
        }
        val ch = String(Character.toChars(cp))
        if (ch.isBlank()) {
            if (!prevWasSpace) {
                sb.append(ch)
                prevWasSpace = true
            }
        } else {
            sb.append(ch)
            prevWasSpace = false
        }
        i++
    }
    return sb.toString().trim()
}

private val actionPattern = Regex("(?<!\\*)\\*(?!\\*)((?:(?!\\*).)+?)\\*(?!\\*)")

internal fun structureActions(text: String): String {
    val result = actionPattern.replace(text) { match ->
        val inner = match.groupValues[1].trim()
        val wordCount = inner.split(Regex("\\s+")).size
        val lineStart = text.lastIndexOf('\n', match.range.first).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', match.range.last).let { if (it == -1) text.length else it }
        val textBefore = text.substring(lineStart, match.range.first).trim()
        val textAfter = text.substring(match.range.last + 1, lineEnd).trim()
        val inSentence = textBefore.isNotEmpty() || textAfter.isNotEmpty()
        if (wordCount <= 2 && inSentence) return@replace match.value
        val before = if (match.range.first > 0 && text[match.range.first - 1] != '\n') "\n" else ""
        val after = if (match.range.last < text.lastIndex && text[match.range.last + 1] != '\n') "\n" else ""
        "$before${match.value}$after"
    }
    return result.replace(Regex("\n{3,}"), "\n\n").trim()
}
