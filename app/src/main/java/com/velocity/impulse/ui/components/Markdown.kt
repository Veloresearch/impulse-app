package com.velocity.impulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

@Immutable
private sealed interface Block {
    data class Text(val text: String) : Block
    data class Heading(val text: String, val level: Int) : Block
    data class Item(val text: String, val mark: String) : Block
    data class Code(val text: String) : Block
}

private fun blocks(src: String): List<Block> {
    val out = mutableListOf<Block>()
    val para = StringBuilder()
    fun flush() {
        if (para.isNotBlank()) out += Block.Text(para.toString().trim())
        para.clear()
    }
    val lines = src.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val t = line.trimStart()
        when {
            t.startsWith("```") -> {
                flush()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) code.appendLine(lines[i++])
                out += Block.Code(code.toString().trimEnd())
            }
            t.startsWith("#") -> {
                flush()
                val level = t.takeWhile { it == '#' }.length
                out += Block.Heading(t.drop(level).trim(), level)
            }
            t.startsWith("- ") || t.startsWith("* ") -> {
                flush()
                out += Block.Item(t.drop(2), "•")
            }
            Regex("^\\d+[.)] ").containsMatchIn(t) -> {
                flush()
                val n = t.substringBefore(' ')
                out += Block.Item(t.substringAfter(' '), n)
            }
            t.isEmpty() -> flush()
            else -> para.append(line).append('\n')
        }
        i++
    }
    flush()
    return out
}

private fun inline(s: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < s.length) {
        when {
            s.startsWith("**", i) && s.indexOf("**", i + 2) > 0 -> {
                val end = s.indexOf("**", i + 2)
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(s.substring(i + 2, end)) }
                i = end + 2
            }
            s[i] == '`' && s.indexOf('`', i + 1) > 0 -> {
                val end = s.indexOf('`', i + 1)
                withStyle(SpanStyle(fontFamily = com.velocity.impulse.ui.theme.JetBrainsMono, background = Tone.Raise, color = Ink.One)) { append(s.substring(i + 1, end)) }
                i = end + 1
            }
            s[i] == '*' && i + 1 < s.length && s[i + 1] != ' ' && s.indexOf('*', i + 1) > 0 -> {
                val end = s.indexOf('*', i + 1)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(s.substring(i + 1, end)) }
                i = end + 1
            }
            else -> append(s[i++])
        }
    }
}

/**
 * The subset of Markdown models actually write: paragraphs, headings, lists, code.
 *
 * While an answer streams, [edge] says how much of the newest word is revealed (0..1). The last
 * block then fades that word in behind a caret that rides the end of the text; [caret] is the
 * caret's opacity.
 */
@Composable
fun Markdown(text: String, modifier: Modifier = Modifier, edge: (() -> Float)? = null, caret: () -> Float = { 1f }) {
    val parsed = remember(text) { blocks(text) }
    val ref = remember { arrayOfNulls<TextLayoutResult>(1) }
    val reveal = edge?.let { Reveal(it, caret, LocalAccent.current, ref) }
    SelectionContainer(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            parsed.forEachIndexed { i, b ->
                key(i) { Part(b, reveal?.takeIf { i == parsed.lastIndex }) }
            }
        }
    }
}

private class Reveal(val edge: () -> Float, val caret: () -> Float, val accent: Color, val ref: Array<TextLayoutResult?>)

@Composable
private fun Part(b: Block, reveal: Reveal?) {
    val text = remember(b) {
        when (b) {
            is Block.Text -> inline(b.text)
            is Block.Heading -> inline(b.text)
            is Block.Item -> inline(b.text)
            is Block.Code -> AnnotatedString(b.text)
        }
    }
    val tail = if (reveal == null) Modifier else Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        val l = reveal.ref[0] ?: return@drawWithContent
        val f = reveal.edge().coerceIn(0f, 1f)
        val end = l.layoutInput.text.length
        val line = l.getLineForOffset(end)
        val top = l.getLineTop(line)
        val bot = l.getLineBottom(line)
        val x = l.getHorizontalPosition(end, true)
        val w = 28.dp.toPx()
        drawRect(
            Brush.horizontalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 1f - f), startX = x - w, endX = x),
            Offset(x - w, top), Size(w, bot - top), blendMode = BlendMode.DstOut,
        )
        drawRect(reveal.accent, Offset(x + 2.dp.toPx(), top + 2.dp.toPx()), Size(2.dp.toPx(), bot - top - 4.dp.toPx()), alpha = reveal.caret().coerceIn(0f, 1f))
    }
    val laid: ((TextLayoutResult) -> Unit)? = reveal?.let { r -> { r.ref[0] = it } }
    when (b) {
        is Block.Text -> BasicText(text, tail, style = Type.Body, onTextLayout = laid)
        is Block.Heading -> BasicText(
            text,
            Modifier.padding(top = Space.S2).then(tail),
            style = if (b.level <= 2) Type.Title.copy(fontSize = Type.Section.fontSize) else Type.Section,
            onTextLayout = laid,
        )
        is Block.Item -> Row {
            BasicText(b.mark, Modifier.width(22.dp), style = Type.Body.copy(color = Ink.Three))
            BasicText(text, tail, style = Type.Body, onTextLayout = laid)
        }
        is Block.Code -> BasicText(
            text,
            Modifier.fillMaxWidth().background(Tone.Inset, Shapes.Tile).horizontalScroll(rememberScrollState()).padding(Space.S3).then(tail),
            style = Type.Mono,
            softWrap = false,
            onTextLayout = laid,
        )
    }
}
