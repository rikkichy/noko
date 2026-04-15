package cat.ri.noko.ui.components.streaming

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis

/**
 * Shared character-reveal hook. Returns the currently displayed substring,
 * advancing frame-by-frame with adaptive chunking.
 */
@Composable
fun rememberStreamingReveal(text: String): String {
    var displayed by remember { mutableStateOf("") }
    val targetText by rememberUpdatedState(text)

    LaunchedEffect(Unit) {
        var lastFrameMs = 0L
        while (true) {
            withFrameMillis { frameMs ->
                val target = targetText
                if (displayed.length < target.length) {
                    val behind = target.length - displayed.length
                    val dt = if (lastFrameMs == 0L) 16f else (frameMs - lastFrameMs).toFloat()
                    val scale = dt / 16f
                    val baseChunk = when {
                        behind > 200 -> 30
                        behind > 100 -> 15
                        behind > 50 -> 8
                        behind > 20 -> 4
                        else -> 1
                    }
                    val chunk = (baseChunk * scale).toInt().coerceAtLeast(1)
                    displayed = target.substring(
                        0,
                        (displayed.length + chunk).coerceAtMost(target.length),
                    )
                }
                lastFrameMs = frameMs
            }
        }
    }

    if (displayed.length > text.length) displayed = text

    return displayed
}

fun stripTrailingDelimiters(text: String): String {
    val trailing = listOf("***", "**", "~~", "*", "`")
    for (d in trailing) {
        if (text.endsWith(d)) return text.dropLast(d.length)
    }
    return text
}
