package com.velocity.impulse.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.JetBrainsMono
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.delay

@Immutable
private sealed interface Block {
    data class Text(val text: String) : Block
    data class Heading(val text: String, val level: Int) : Block

    /** A list item; [mark] is its number ("3."), or null for a bullet. */
    data class Item(val text: String, val mark: String?, val depth: Int) : Block
    data class Code(val text: String, val lang: String) : Block
    data class Quote(val text: String) : Block
    data object Rule : Block
}

private val Ordered = Regex("^(\\d{1,3})[.)]\\s+")
private val RuleLine = Regex("^(-{3,}|\\*{3,}|_{3,})$")
private val CiteInner = Regex("^\\d{1,2}(\\s*,\\s*\\d{1,2})*$")
private val CiteAny = Regex("\\s?\\[\\d{1,2}(\\s*,\\s*\\d{1,2})*]")

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
        val depth = ((line.length - t.length) / 2).coerceAtMost(3)
        val ordered = Ordered.find(t)
        val last = out.lastOrNull()
        when {
            t.startsWith("```") -> {
                flush()
                val lang = t.removePrefix("```").trim().substringBefore(' ')
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) code.appendLine(lines[i++])
                out += Block.Code(code.toString().trimEnd(), lang)
            }
            RuleLine.matches(t.trimEnd()) -> {
                flush()
                out += Block.Rule
            }
            t.startsWith("#") && t.trimStart('#').let { it.isEmpty() || it.startsWith(" ") } -> {
                flush()
                val level = t.takeWhile { it == '#' }.length
                out += Block.Heading(t.drop(level).trim(), level.coerceAtMost(4))
            }
            t.startsWith(">") -> {
                flush()
                val q = t.removePrefix(">").trim()
                val prev = lines.getOrNull(i - 1)?.trimStart()?.startsWith(">") == true
                if (prev && last is Block.Quote) out[out.lastIndex] = Block.Quote(last.text + "\n" + q) else out += Block.Quote(q)
            }
            t.startsWith("- ") || t.startsWith("* ") || t.startsWith("+ ") -> {
                flush()
                out += Block.Item(t.drop(2).trim(), null, depth)
            }
            ordered != null -> {
                flush()
                out += Block.Item(t.substring(ordered.range.last + 1).trim(), ordered.groupValues[1] + ".", depth)
            }
            t.isEmpty() -> flush()
            // A wrapped line of the item above it.
            last is Block.Item && para.isEmpty() && depth > 0 && lines.getOrNull(i - 1)?.isNotBlank() == true ->
                out[out.lastIndex] = last.copy(text = last.text + " " + t)
            else -> para.append(t).append('\n')
        }
        i++
    }
    flush()
    return out
}

private val Strong = SpanStyle(fontWeight = FontWeight.SemiBold, color = Ink.One)

private fun inline(s: String, accent: Color, sources: List<Pair<String, String>>): AnnotatedString = buildAnnotatedString {
    val link = TextLinkStyles(SpanStyle(color = accent))
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            // An unclosed ** is a bold still being written: the rest is bold, and the marks never flash.
            s.startsWith("**", i) -> {
                val end = s.indexOf("**", i + 2).let { if (it < 0) s.length else it }
                withStyle(Strong) { append(inline(s.substring(i + 2, end), accent, sources)) }
                i = (end + 2).coerceAtMost(s.length)
            }
            s.startsWith("~~", i) && s.indexOf("~~", i + 2) > 0 -> {
                val end = s.indexOf("~~", i + 2)
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Ink.Three)) { append(s.substring(i + 2, end)) }
                i = end + 2
            }
            c == '`' && s.indexOf('`', i + 1) > 0 -> {
                val end = s.indexOf('`', i + 1)
                withStyle(SpanStyle(fontFamily = JetBrainsMono, fontSize = 0.88.em, background = Tone.Raise, color = Ink.One)) { append(s.substring(i + 1, end)) }
                i = end + 1
            }
            c == '*' && i + 1 < s.length && s[i + 1] != ' ' && s.indexOf('*', i + 1) > 0 -> {
                val end = s.indexOf('*', i + 1)
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(inline(s.substring(i + 1, end), accent, sources)) }
                i = end + 1
            }
            c == '[' && s.indexOf(']', i) > 0 -> {
                val close = s.indexOf(']', i)
                val inner = s.substring(i + 1, close)
                val paren = if (s.startsWith("(", close + 1)) s.indexOf(')', close + 2) else -1
                when {
                    paren > 0 -> {
                        withLink(LinkAnnotation.Url(s.substring(close + 2, paren), link)) { append(inline(inner, accent, sources)) }
                        i = paren + 1
                    }
                    CiteInner.matches(inner) -> {
                        inner.split(',').map { it.trim() }.forEachIndexed { k, n ->
                            if (k > 0) append(' ')
                            val url = sources.getOrNull(n.toInt() - 1)?.second
                            val cite = SpanStyle(color = accent, fontSize = 11.sp, fontWeight = FontWeight.Medium, baselineShift = BaselineShift(0.3f))
                            if (url != null) withLink(LinkAnnotation.Url(url, TextLinkStyles(cite))) { append(n) } else withStyle(cite) { append(n) }
                        }
                        i = close + 1
                    }
                    else -> {
                        append(c)
                        i++
                    }
                }
            }
            s.startsWith("https://", i) || s.startsWith("http://", i) -> {
                var end = i
                while (end < s.length && !s[end].isWhitespace() && s[end] != ')') end++
                while (end > i && s[end - 1] in ".,;:!?\"'") end--
                val url = s.substring(i, end)
                withLink(LinkAnnotation.Url(url, link)) { append(url) }
                i = end
            }
            else -> {
                append(c)
                i++
            }
        }
    }
}

/** The answer as plain text: no Markdown marks or citations, bullets as "•", code as written. */
fun plainText(md: String): String = buildString {
    var prev: Block? = null
    for (b in blocks(md)) {
        if (prev != null) append(if (prev is Block.Item && b is Block.Item) "\n" else "\n\n")
        fun flat(s: String) = inline(CiteAny.replace(s, ""), Color.Unspecified, emptyList()).text
        when (b) {
            is Block.Text -> append(flat(b.text))
            is Block.Heading -> append(flat(b.text))
            is Block.Item -> append("  ".repeat(b.depth)).append(b.mark ?: "•").append(' ').append(flat(b.text))
            is Block.Quote -> append(flat(b.text))
            is Block.Code -> append(b.text)
            Block.Rule -> append("—")
        }
        prev = b
    }
}

private val Body = Type.Body.copy(color = Ink.Two)

private fun heading(level: Int) = Type.Brand.copy(
    color = Ink.One,
    fontWeight = FontWeight.Normal,
    letterSpacing = (-0.018).em,
    fontSize = when (level) {
        1 -> 23.sp
        2 -> 20.sp
        else -> 17.sp
    },
    lineHeight = when (level) {
        1 -> 29.sp
        2 -> 26.sp
        else -> 23.sp
    },
)

private fun gap(prev: Block?, b: Block): Dp = when {
    prev == null -> 0.dp
    b is Block.Heading -> 22.dp
    prev is Block.Heading -> 8.dp
    prev is Block.Item && b is Block.Item -> 8.dp
    else -> 14.dp
}

/**
 * The Markdown models actually write: paragraphs, headings, lists, quotes, code, links and the
 * [n] citations of an answer written from [sources]. Body text is the softer ink; bold and
 * headings are full white. Text is selectable only while [selectable].
 *
 * While an answer streams, [edge] says how much of the newest word is revealed (0..1). The last
 * block then fades that word in behind a caret that rides the end of the text; [caret] is the
 * caret's opacity.
 */
@Composable
fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    sources: List<Pair<String, String>> = emptyList(),
    selectable: Boolean = false,
    edge: (() -> Float)? = null,
    caret: () -> Float = { 1f },
) {
    val parsed = remember(text) { blocks(text) }
    val accent = LocalAccent.current
    val ref = remember { arrayOfNulls<TextLayoutResult>(1) }
    val reveal = edge?.let { Reveal(it, caret, accent, ref) }
    val body = @Composable {
        Column(modifier) {
            parsed.forEachIndexed { i, b ->
                key(i) {
                    Part(b, accent, sources, reveal?.takeIf { i == parsed.lastIndex }, Modifier.padding(top = gap(parsed.getOrNull(i - 1), b)))
                }
            }
        }
    }
    if (selectable) SelectionContainer { body() } else body()
}

private class Reveal(val edge: () -> Float, val caret: () -> Float, val accent: Color, val ref: Array<TextLayoutResult?>)

private fun Modifier.tail(reveal: Reveal): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
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

@Composable
private fun Part(b: Block, accent: Color, sources: List<Pair<String, String>>, reveal: Reveal?, modifier: Modifier) {
    val text = remember(b, accent, sources) {
        when (b) {
            is Block.Text -> inline(b.text, accent, sources)
            is Block.Heading -> inline(b.text, accent, sources)
            is Block.Item -> inline(b.text, accent, sources)
            is Block.Quote -> inline(b.text, accent, sources)
            is Block.Code -> AnnotatedString(b.text)
            Block.Rule -> AnnotatedString("")
        }
    }
    val tail = reveal?.let { Modifier.tail(it) } ?: Modifier
    val laid: ((TextLayoutResult) -> Unit)? = reveal?.let { r -> { r.ref[0] = it } }
    when (b) {
        is Block.Text -> BasicText(text, modifier.then(tail), style = Body, onTextLayout = laid)
        is Block.Heading -> BasicText(text, modifier.then(tail), style = heading(b.level), onTextLayout = laid)
        is Block.Item -> Row(modifier.padding(start = 22.dp * b.depth)) {
            val line = with(LocalDensity.current) { Body.lineHeight.toDp() }
            Box(Modifier.widthIn(min = 24.dp).height(line).padding(end = 6.dp), contentAlignment = Alignment.CenterStart) {
                if (b.mark == null) {
                    Box(Modifier.padding(start = 3.dp).size(6.dp).border(1.3.dp, Ink.Three, CircleShape))
                } else {
                    BasicText(b.mark, style = Body.copy(color = Ink.Three, fontFeatureSettings = "tnum"))
                }
            }
            BasicText(text, tail, style = Body, onTextLayout = laid)
        }
        is Block.Quote -> Row(modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(Tone.Hover, CircleShape))
            Spacer(Modifier.width(12.dp))
            BasicText(text, tail, style = Body.copy(color = Ink.Three), onTextLayout = laid)
        }
        is Block.Code -> Column(modifier.fillMaxWidth().background(Tone.Inset, Shapes.Tile)) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 2.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicText(b.lang.ifBlank { tr("code", "kod") }, Modifier.weight(1f), style = Type.Label.copy(color = Ink.Three))
                CopyButton({ b.text }, size = 34.dp)
            }
            BasicText(
                text,
                Modifier.horizontalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, bottom = 14.dp).then(tail),
                style = Type.Mono,
                softWrap = false,
                onTextLayout = laid,
            )
        }
        Block.Rule -> Box(modifier.padding(vertical = 6.dp).fillMaxWidth().height(1.dp).background(Tone.Raise))
    }
}

/** Copies [text] and answers with a check that springs in, then settles back to the copy glyph. */
@Composable
fun CopyButton(text: () -> String, modifier: Modifier = Modifier, size: Dp = 36.dp, tint: Color = Ink.Three) {
    val clip = LocalClipboardManager.current
    val accent = LocalAccent.current
    val calm = LocalReduceMotion.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1400)
            copied = false
        }
    }
    Box(
        modifier.size(size).pressable(
            {
                clip.setText(AnnotatedString(text()))
                copied = true
            },
            CircleShape, Color.Transparent, Tone.Raise, depth = 0.88f, stretch = true,
        ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            copied,
            transitionSpec = {
                if (targetState && !calm) {
                    (scaleIn(spring(0.55f, 700f), 0.6f) + fadeIn(tween(90))) togetherWith (scaleOut(tween(90), targetScale = 0.6f) + fadeOut(tween(90)))
                } else {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                }
            },
            label = "copy",
        ) { done ->
            Icon(if (done) Glyph.Check else Glyph.Copy, if (done) accent else tint, size = 18.dp)
        }
    }
}
