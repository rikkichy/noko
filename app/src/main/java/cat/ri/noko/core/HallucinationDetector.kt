package cat.ri.noko.core

import cat.ri.noko.model.ChatMessage

object HallucinationDetector {

    enum class Violation(val label: String, val code: String) {
        PERSONA_IMPERSONATION("Impersonation", "NOK_IMP"),
        REPETITION_LOOP("Repetition loop", "NOK_REP"),
        EXCESSIVE_EMOJIS("Excessive emojis", "NOK_EMJ"),
        LANGUAGE_SWITCH("Language switch", "NOK_LNG"),
    }

    fun scan(
        content: String,
        userName: String?,
        previousMessages: List<ChatMessage>,
    ): Violation? {
        checkPersonaImpersonation(content, userName)?.let { return it }
        checkRepetitionLoop(content)?.let { return it }
        checkExcessiveEmojis(content)?.let { return it }
        checkLanguageSwitch(content, previousMessages)?.let { return it }
        return null
    }

    private fun checkPersonaImpersonation(content: String, userName: String?): Violation? {
        if (userName.isNullOrBlank()) return null
        val escaped = Regex.escape(userName)
        val md = "[*_~]{0,3}"
        val dialoguePattern = Regex("(?m)^\\s*${md}${escaped}${md}\\s*:", RegexOption.IGNORE_CASE)
        val actionPattern = Regex("\\*\\s*$escaped\\s+", RegexOption.IGNORE_CASE)
        val inlineDialogue = Regex("${md}${escaped}${md}\\s*:", RegexOption.IGNORE_CASE)
        val inlineMatches = inlineDialogue.findAll(content).count()
        if (dialoguePattern.containsMatchIn(content) ||
            actionPattern.containsMatchIn(content) ||
            inlineMatches >= 2
        ) {
            return Violation.PERSONA_IMPERSONATION
        }
        return null
    }

    private fun checkRepetitionLoop(content: String): Violation? {
        val words = content.split(Regex("[\\s.,!?;:()\\[\\]{}\"']+")).filter { it.length > 2 }
        if (words.size < 10) return null

        val freq = mutableMapOf<String, Int>()
        for (w in words) {
            val key = w.lowercase()
            freq[key] = (freq[key] ?: 0) + 1
        }
        val topCount = freq.values.maxOrNull() ?: 0
        if (topCount >= 5 && topCount.toFloat() / words.size > 0.15f) {
            return Violation.REPETITION_LOOP
        }

        if (words.size >= 4) {
            val bigrams = mutableMapOf<String, Int>()
            for (i in 0 until words.size - 1) {
                val key = "${words[i].lowercase()} ${words[i + 1].lowercase()}"
                bigrams[key] = (bigrams[key] ?: 0) + 1
            }
            val topBigram = bigrams.values.maxOrNull() ?: 0
            if (topBigram >= 4 && topBigram.toFloat() / (words.size - 1) > 0.05f) return Violation.REPETITION_LOOP
        }

        val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size >= 3) {
            val lineFreq = mutableMapOf<String, Int>()
            for (line in lines) {
                val key = line.lowercase()
                lineFreq[key] = (lineFreq[key] ?: 0) + 1
            }
            val topLine = lineFreq.values.maxOrNull() ?: 0
            if (topLine >= 3) return Violation.REPETITION_LOOP
        }

        return null
    }

    private fun checkExcessiveEmojis(content: String): Violation? {
        if (content.length < 20) return null
        val codePoints = content.codePoints().toArray()
        var emojiCount = 0
        for (cp in codePoints) {
            if (isEmoji(cp)) emojiCount++
        }
        if (emojiCount >= 10) return Violation.EXCESSIVE_EMOJIS
        if (emojiCount >= 5) {
            val ratio = emojiCount.toFloat() / codePoints.size.toFloat()
            if (ratio > 0.08f) return Violation.EXCESSIVE_EMOJIS
        }
        return null
    }

    internal fun isEmoji(codePoint: Int): Boolean {
        return codePoint in 0x1F600..0x1F64F ||
                codePoint in 0x1F300..0x1F5FF ||
                codePoint in 0x1F680..0x1F6FF ||
                codePoint in 0x1F900..0x1F9FF ||
                codePoint in 0x1FA00..0x1FA6F ||
                codePoint in 0x1FA70..0x1FAFF ||
                codePoint in 0x2600..0x26FF ||
                codePoint in 0x2700..0x27BF ||
                codePoint in 0xFE00..0xFE0F ||
                codePoint in 0x200D..0x200D ||
                codePoint in 0x23E9..0x23F3 ||
                codePoint in 0x231A..0x231B
    }

    private fun checkLanguageSwitch(
        content: String,
        previousMessages: List<ChatMessage>,
    ): Violation? {
        val userMessages = previousMessages.filter { it.role == ChatMessage.Role.USER }
        if (userMessages.size < 2) return null

        val baselineText = userMessages.takeLast(3).joinToString(" ") { it.content }
        val baselineScript = dominantScript(baselineText) ?: return null
        val responseScript = dominantScript(content) ?: return null

        if (baselineScript != responseScript) {
            val responseLetters = content.codePoints().toArray()
                .filter { Character.isLetter(it) }
            if (responseLetters.isEmpty()) return null
            val foreignCount = responseLetters.count { scriptOf(it) != baselineScript }
            val foreignRatio = foreignCount.toFloat() / responseLetters.size.toFloat()
            if (foreignRatio > 0.4f) return Violation.LANGUAGE_SWITCH
        }
        return null
    }

    private enum class ScriptFamily { LATIN, CYRILLIC, CJK, ARABIC, DEVANAGARI, OTHER }

    private fun dominantScript(text: String): ScriptFamily? {
        val counts = mutableMapOf<ScriptFamily, Int>()
        text.codePoints().forEach { cp ->
            if (Character.isLetter(cp)) {
                val script = scriptOf(cp)
                counts[script] = (counts[script] ?: 0) + 1
            }
        }
        return counts.maxByOrNull { it.value }?.key
    }

    private fun scriptOf(codePoint: Int): ScriptFamily {
        val block = Character.UnicodeBlock.of(codePoint)
        return when {
            block == Character.UnicodeBlock.BASIC_LATIN ||
            block == Character.UnicodeBlock.LATIN_1_SUPPLEMENT ||
            block == Character.UnicodeBlock.LATIN_EXTENDED_A ||
            block == Character.UnicodeBlock.LATIN_EXTENDED_B ||
            block == Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL -> ScriptFamily.LATIN

            block == Character.UnicodeBlock.CYRILLIC ||
            block == Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY -> ScriptFamily.CYRILLIC

            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
            block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
            block == Character.UnicodeBlock.HIRAGANA ||
            block == Character.UnicodeBlock.KATAKANA ||
            block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
            block == Character.UnicodeBlock.HANGUL_JAMO -> ScriptFamily.CJK

            block == Character.UnicodeBlock.ARABIC ||
            block == Character.UnicodeBlock.ARABIC_SUPPLEMENT -> ScriptFamily.ARABIC

            block == Character.UnicodeBlock.DEVANAGARI -> ScriptFamily.DEVANAGARI

            else -> ScriptFamily.OTHER
        }
    }
}
