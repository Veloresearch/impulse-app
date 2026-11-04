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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

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
                withStyle(SpanStyle(fontWeight = FontWeight(650))) { append(s.substring(i + 2, end)) }
                i = end + 2
            }
            s[i] == '`' && s.indexOf('`', i + 1) > 0 -> {
                val end = s.indexOf('`', i + 1)
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Tone.Raise, color = Ink.One)) { append(s.substring(i + 1, end)) }
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

/** The subset of Markdown models actually write: paragraphs, headings, lists, code. */
@Composable
fun Markdown(text: String, modifier: Modifier = Modifier) {
    val parsed = remember(text) { blocks(text) }
    SelectionContainer(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            parsed.forEach { b ->
                when (b) {
                    is Block.Text -> BasicText(inline(b.text), style = Type.Body)
                    is Block.Heading -> BasicText(inline(b.text), style = if (b.level <= 2) Type.Title.copy(fontSize = Type.Section.fontSize) else Type.Section, modifier = Modifier.padding(top = Space.S2))
                    is Block.Item -> Row {
                        BasicText(b.mark, Modifier.width(22.dp), style = Type.Body.copy(color = Ink.Three))
                        BasicText(inline(b.text), style = Type.Body)
                    }
                    is Block.Code -> BasicText(
                        b.text,
                        Modifier.fillMaxWidth().background(Tone.Inset, Shapes.Tile).horizontalScroll(rememberScrollState()).padding(Space.S3),
                        style = Type.Mono,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
