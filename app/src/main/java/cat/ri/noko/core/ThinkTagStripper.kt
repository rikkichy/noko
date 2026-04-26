package cat.ri.noko.core

/**
 * Streaming parser that splits incoming model text into reasoning (inside
 * `<think>` / `<thinking>` tags) and visible content. Tags may straddle
 * SSE chunk boundaries, so state is retained between [feed] calls.
 */
class ThinkTagStripper {

    data class Chunk(val content: String = "", val reasoning: String = "")

    private enum class State { OUTSIDE, INSIDE }

    private var state = State.OUTSIDE
    private var pending = StringBuilder()

    /**
     * Feed a chunk of streamed text. Returns the portion that can be
     * classified so far; any tail that could still be part of a tag
     * is held until the next call or [flush].
     */
    fun feed(input: String): Chunk {
        if (input.isEmpty()) return Chunk()
        pending.append(input)
        val content = StringBuilder()
        val reasoning = StringBuilder()
        var i = 0
        val buf = pending.toString()
        while (i < buf.length) {
            if (state == State.OUTSIDE) {
                val open = findAt(buf, i, OPEN_TAGS)
                if (open != null) {
                    state = State.INSIDE
                    i += open.length
                    continue
                }
                if (couldStartTag(buf, i)) break
                content.append(buf[i])
                i++
            } else {
                val close = findAt(buf, i, CLOSE_TAGS)
                if (close != null) {
                    state = State.OUTSIDE
                    i += close.length
                    continue
                }
                if (couldStartTag(buf, i)) break
                reasoning.append(buf[i])
                i++
            }
        }
        pending = StringBuilder(buf.substring(i))
        return Chunk(content.toString(), reasoning.toString())
    }

    /** Drain any buffered tail once the stream ends. */
    fun flush(): Chunk {
        if (pending.isEmpty()) return Chunk()
        val tail = pending.toString()
        pending = StringBuilder()
        return if (state == State.INSIDE) Chunk(reasoning = tail) else Chunk(content = tail)
    }

    private fun findAt(buf: String, idx: Int, tags: Array<String>): String? =
        tags.firstOrNull { buf.regionMatches(idx, it, 0, it.length, ignoreCase = true) }

    private fun couldStartTag(buf: String, idx: Int): Boolean {
        if (buf[idx] != '<') return false
        val remaining = buf.length - idx
        val longest = ALL_TAGS.maxOf { it.length }
        return remaining < longest
    }

    companion object {
        private val OPEN_TAGS = arrayOf("<think>", "<thinking>")
        private val CLOSE_TAGS = arrayOf("</think>", "</thinking>")
        private val ALL_TAGS = OPEN_TAGS + CLOSE_TAGS
    }
}
