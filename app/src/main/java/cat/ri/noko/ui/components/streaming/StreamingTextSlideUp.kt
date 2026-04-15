package cat.ri.noko.ui.components.streaming

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import cat.ri.noko.ui.util.parseMarkdown

@Composable
fun StreamingTextSlideUp(
    text: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle,
) {
    val displayed = rememberStreamingReveal(text)

    val renderText = if (isStreaming && displayed.length < text.length) {
        stripTrailingDelimiters(displayed)
    } else {
        displayed
    }

    val parsed = parseMarkdown(renderText)
    val hasFrontier = isStreaming && displayed.length < text.length && parsed.isNotEmpty()

    val styledText = if (hasFrontier) {
        val trailLen = 12
        val trailStart = (parsed.length - trailLen).coerceAtLeast(0)
        val trailChars = (parsed.length - trailStart).coerceAtLeast(1)
        val baseColor = MaterialTheme.colorScheme.onSurface
        buildAnnotatedString {
            append(parsed)
            for (i in trailStart until parsed.length) {
                val t = (i - trailStart).toFloat() / trailChars
                val alpha = 1f - t * 0.65f
                val shift = BaselineShift(t * -0.25f)
                addStyle(
                    SpanStyle(
                        color = baseColor.copy(alpha = alpha),
                        baselineShift = shift,
                    ),
                    start = i,
                    end = i + 1,
                )
            }
        }
    } else {
        parsed
    }

    Text(
        text = styledText,
        modifier = modifier,
        style = style,
    )
}
