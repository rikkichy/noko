package cat.ri.noko.ui.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun parseMarkdown(text: String): AnnotatedString {
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val codeFg = MaterialTheme.colorScheme.onSurface
    val italicColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    return buildAnnotatedString {
        parse(text, codeBg, codeFg, italicColor)
    }
}

private fun AnnotatedString.Builder.parse(
    text: String,
    codeBg: Color,
    codeFg: Color,
    italicColor: Color,
) {
    var i = 0
    while (i < text.length) {
        when {
            // Bold+Italic ***text***
            text.startsWith("***", i) -> {
                val end = text.indexOf("***", i + 3)
                if (end >= 0) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = italicColor))
                    parse(text.substring(i + 3, end), codeBg, codeFg, italicColor)
                    pop()
                    i = end + 3
                } else {
                    append(text[i])
                    i++
                }
            }
            // Bold **text**
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end >= 0) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    parse(text.substring(i + 2, end), codeBg, codeFg, italicColor)
                    pop()
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Italic *text*
            text[i] == '*' && i + 1 < text.length && text[i + 1] != ' ' -> {
                val end = text.indexOf('*', i + 1)
                if (end >= 0 && text[end - 1] != ' ') {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = italicColor))
                    parse(text.substring(i + 1, end), codeBg, codeFg, italicColor)
                    pop()
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            // Strikethrough ~~text~~
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end >= 0) {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    parse(text.substring(i + 2, end), codeBg, codeFg, italicColor)
                    pop()
                    i = end + 2
                } else {
                    append(text[i])
                    i++
                }
            }
            // Inline code `text`
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end >= 0) {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg, color = codeFg))
                    append(text.substring(i + 1, end))
                    pop()
                    i = end + 1
                } else {
                    append(text[i])
                    i++
                }
            }
            else -> {
                append(text[i])
                i++
            }
        }
    }
}
