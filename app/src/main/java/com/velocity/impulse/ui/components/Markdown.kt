package com.velocity.impulse.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.LruCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
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
import com.velocity.impulse.ui.theme.Theme
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

@Immutable
private sealed interface Block {
    data class Text(val text: String) : Block
    data class Heading(val text: String, val level: Int) : Block

    /** A list item; [mark] is its number ("3."), or null for a bullet. */
    data class Item(val text: String, val mark: String?, val depth: Int) : Block
    data class Code(val text: String, val lang: String) : Block
    data class Quote(val text: String) : Block

    /** [align] per column: -1 start, 0 centre, 1 end. */
    data class Table(val head: List<String>, val rows: List<List<String>>, val align: List<Int>) : Block
    data class Picture(val alt: String, val url: String) : Block
    data object Rule : Block
}

private val Ordered = Regex("^(\\d{1,3})[.)]\\s+")
private val Bullet = Regex("^[-*+]\\s+")
private val RuleLine = Regex("^(-{3,}|\\*{3,}|_{3,})$")
private val TableRule = Regex("^\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?$")
private val PictureLine = Regex("^!\\[([^\\]]*)]\\(\\s*(\\S+?)(?:\\s+\"[^\"]*\")?\\s*\\)$")
private val Fence = Regex("^(`{3,}|~{3,})\\s*([\\w#+.-]*)")
private val CiteInner = Regex("^\\d{1,2}(\\s*,\\s*\\d{1,2})*$")
private val CiteAny = Regex("\\s?\\[\\d{1,2}(\\s*,\\s*\\d{1,2})*]")

private fun indentOf(line: String): Int {
    var n = 0
    for (c in line) {
        if (c == ' ') n++ else if (c == '\t') n += 4 else break
    }
    return n
}

/** [line] without up to [cols] columns of its leading space, for code nested in a list. */
private fun dedent(line: String, cols: Int): String {
    var c = 0
    var i = 0
    while (i < line.length && c < cols && (line[i] == ' ' || line[i] == '\t')) {
        c += if (line[i] == '\t') 4 else 1
        i++
    }
    return line.substring(i)
}

/** A table row's cells; a pipe inside `code` or escaped as \| stays in the cell. */
private fun cells(line: String): List<String> {
    var s = line.trim()
    if (s.startsWith("|")) s = s.drop(1)
    if (s.endsWith("|") && !s.endsWith("\\|")) s = s.dropLast(1)
    val out = mutableListOf<String>()
    val cur = StringBuilder()
    var code = false
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            c == '\\' && i + 1 < s.length && s[i + 1] == '|' -> {
                cur.append('|')
                i++
            }
            c == '`' -> {
                code = !code
                cur.append(c)
            }
            c == '|' && !code -> {
                out += cur.toString().trim()
                cur.clear()
            }
            else -> cur.append(c)
        }
        i++
    }
    out += cur.toString().trim()
    return out
}

private fun blocks(src: String): List<Block> {
    val out = mutableListOf<Block>()
    val para = StringBuilder()
    // The indents of the open list levels, so 2, 3 or 4 spaces all nest one level each.
    val levels = mutableListOf<Int>()
    fun flush() {
        if (para.isNotBlank()) out += Block.Text(para.toString().trim())
        para.clear()
    }
    val lines = src.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val t = line.trimStart()
        val indent = indentOf(line)
        val ordered = Ordered.find(t)
        val last = out.lastOrNull()
        val fence = Fence.find(t)
        when {
            fence != null -> {
                flush()
                val mark = fence.groupValues[1]
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith(mark)) code.append(dedent(lines[i++], indent)).append('\n')
                out += Block.Code(code.toString().trimEnd(), fence.groupValues[2])
            }
            RuleLine.matches(t.trimEnd()) -> {
                flush()
                out += Block.Rule
            }
            t.contains('|') && lines.getOrNull(i + 1)?.trim()?.let { it.contains('|') && TableRule.matches(it) } == true -> {
                flush()
                val head = cells(t)
                val align = cells(lines[i + 1]).map { c ->
                    when {
                        c.startsWith(":") && c.endsWith(":") -> 0
                        c.endsWith(":") -> 1
                        else -> -1
                    }
                }
                val rows = mutableListOf<List<String>>()
                i += 2
                while (i < lines.size && lines[i].isNotBlank() && lines[i].contains('|')) rows += cells(lines[i++])
                out += Block.Table(head, rows, align)
                continue
            }
            PictureLine.matches(t.trimEnd()) -> {
                flush()
                val p = PictureLine.find(t.trimEnd())!!
                out += Block.Picture(p.groupValues[1], p.groupValues[2])
            }
            t.startsWith("#") && t.trimStart('#').let { it.isEmpty() || it.startsWith(" ") } -> {
                flush()
                levels.clear()
                val level = t.takeWhile { it == '#' }.length
                out += Block.Heading(t.drop(level).trim(), level.coerceAtMost(4))
            }
            t.startsWith(">") -> {
                flush()
                val q = t.trimStart('>', ' ')
                if (last is Block.Quote && lines.getOrNull(i - 1)?.trimStart()?.startsWith(">") == true) {
                    out[out.lastIndex] = Block.Quote(last.text + "\n" + q)
                } else {
                    out += Block.Quote(q)
                }
            }
            Bullet.containsMatchIn(t) || ordered != null -> {
                flush()
                while (levels.isNotEmpty() && levels.last() > indent) levels.removeAt(levels.lastIndex)
                if (levels.isEmpty() || levels.last() < indent) levels += indent
                val depth = (levels.size - 1).coerceAtMost(4)
                out += if (ordered != null) {
                    Block.Item(t.substring(ordered.range.last + 1).trim(), ordered.groupValues[1] + ".", depth)
                } else {
                    Block.Item(t.replaceFirst(Bullet, "").trim(), null, depth)
                }
            }
            t.isEmpty() -> flush()
            // A wrapped line of the item above it.
            last is Block.Item && para.isEmpty() && lines.getOrNull(i - 1)?.isNotBlank() == true ->
                out[out.lastIndex] = last.copy(text = last.text + " " + t)
            else -> {
                if (indent == 0) levels.clear()
                para.append(t).append('\n')
            }
        }
        i++
    }
    flush()
    return out
}

private val Strong get() = SpanStyle(fontWeight = FontWeight.SemiBold, color = Ink.One)

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
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Ink.Three)) { append(inline(s.substring(i + 2, end), accent, sources)) }
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
            // A picture inside a sentence: its words, opening the picture.
            c == '!' && s.startsWith("[", i + 1) && s.indexOf("](", i) > 0 && s.indexOf(')', s.indexOf("](", i)) > 0 -> {
                val mid = s.indexOf("](", i)
                val end = s.indexOf(')', mid)
                val alt = s.substring(i + 2, mid).ifBlank { tr("picture", "obraz") }
                withLink(LinkAnnotation.Url(s.substring(mid + 2, end).trim().substringBefore(' '), link)) { append(alt) }
                i = end + 1
            }
            c == '[' && s.indexOf(']', i) > 0 -> {
                val close = s.indexOf(']', i)
                val inner = s.substring(i + 1, close)
                val paren = if (s.startsWith("(", close + 1)) s.indexOf(')', close + 2) else -1
                when {
                    paren > 0 -> {
                        withLink(LinkAnnotation.Url(s.substring(close + 2, paren).trim(), link)) { append(inline(inner, accent, sources)) }
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

private fun flat(s: String) = inline(CiteAny.replace(s, ""), Color.Unspecified, emptyList()).text

/** The answer as plain text: no Markdown marks or citations, bullets as "•", code as written, tables as tab-separated rows. */
fun plainText(md: String): String = buildString {
    var prev: Block? = null
    for (b in blocks(md)) {
        if (prev != null) append(if (prev is Block.Item && b is Block.Item) "\n" else "\n\n")
        when (b) {
            is Block.Text -> append(flat(b.text))
            is Block.Heading -> append(flat(b.text))
            is Block.Item -> append("  ".repeat(b.depth)).append(b.mark ?: "•").append(' ').append(flat(b.text))
            is Block.Quote -> append(flat(b.text))
            is Block.Code -> append(b.text)
            is Block.Table -> append(tsv(b))
            is Block.Picture -> append(b.alt.ifBlank { b.url })
            Block.Rule -> append("—")
        }
        prev = b
    }
}

private fun tsv(b: Block.Table) = (listOf(b.head) + b.rows).joinToString("\n") { r -> r.joinToString("\t") { flat(it) } }

private val Body get() = Type.Body.copy(color = Ink.Two)

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

/** Opens a web page in a Chrome custom tab when there is one, the browser otherwise. Other schemes stay closed. */
fun openPage(ctx: Context, url: String) {
    val uri = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return
    if (uri.scheme?.lowercase() !in setOf("http", "https", "mailto", "tel")) return
    val tab = Intent(Intent.ACTION_VIEW, uri)
        .putExtras(Bundle().apply { putBinder("android.support.customtabs.extra.SESSION", null) })
        .putExtra("android.support.customtabs.extra.TOOLBAR_COLOR", Tone.App.toArgb())
    if (ctx !is Activity) tab.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(tab) }
}

private class Pages(private val ctx: Context) : UriHandler {
    override fun openUri(uri: String) = openPage(ctx, uri)
}

/**
 * The Markdown models actually write: paragraphs, headings, nested lists, quotes, tables, code,
 * pictures, rules, links and the [n] citations of an answer written from [sources]. Body text is
 * the softer ink; bold and headings are full ink. Text is selectable only while [selectable].
 * Links open in a custom tab.
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
    val ctx = LocalContext.current
    val pages = remember(ctx) { Pages(ctx) }
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
    CompositionLocalProvider(LocalUriHandler provides pages) {
        if (selectable) SelectionContainer { body() } else body()
    }
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
    val palette = Theme.palette
    val text = remember(b, accent, sources, palette) {
        when (b) {
            is Block.Text -> inline(b.text, accent, sources)
            is Block.Heading -> inline(b.text, accent, sources)
            is Block.Item -> inline(b.text, accent, sources)
            is Block.Quote -> inline(b.text, accent, sources)
            else -> AnnotatedString("")
        }
    }
    val tail = reveal?.let { Modifier.tail(it) } ?: Modifier
    val laid: ((TextLayoutResult) -> Unit)? = reveal?.let { r -> { r.ref[0] = it } }
    when (b) {
        is Block.Text -> BasicText(text, modifier.then(tail), style = Body, onTextLayout = laid)
        is Block.Heading -> BasicText(text, modifier.then(tail), style = heading(b.level), onTextLayout = laid)
        is Block.Item -> Row(modifier.padding(start = 20.dp * b.depth)) {
            val line = with(LocalDensity.current) { Body.lineHeight.toDp() }
            Box(Modifier.widthIn(min = 24.dp).height(line).padding(end = 6.dp), contentAlignment = Alignment.CenterStart) {
                when {
                    b.mark != null -> BasicText(b.mark, style = Body.copy(color = Ink.Three, fontFeatureSettings = "tnum"))
                    b.depth == 0 -> Box(Modifier.padding(start = 3.dp).size(6.dp).border(1.3.dp, Ink.Three, CircleShape))
                    else -> Box(Modifier.padding(start = 4.dp).size(5.dp).background(Ink.Four, CircleShape))
                }
            }
            BasicText(text, tail, style = Body, onTextLayout = laid)
        }
        is Block.Quote -> Row(modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(2.dp).fillMaxHeight().background(Tone.Hover, CircleShape))
            Spacer(Modifier.width(12.dp))
            BasicText(text, tail, style = Body.copy(color = Ink.Three), onTextLayout = laid)
        }
        is Block.Code -> CodeBlock(b, accent, tail, laid, modifier)
        is Block.Table -> TableBlock(b, accent, sources, modifier)
        is Block.Picture -> PictureBlock(b, modifier)
        Block.Rule -> Box(modifier.padding(vertical = 6.dp).fillMaxWidth().height(1.dp).background(Tone.Raise))
    }
}

/** The bar over a code block or a table: what it is, then its buttons. */
@Composable
private fun Head(label: String, buttons: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 2.dp, top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(label, Modifier.weight(1f), style = Type.Label.copy(color = Ink.Three), maxLines = 1, overflow = TextOverflow.Ellipsis)
        buttons()
    }
}

@Composable
private fun CodeBlock(b: Block.Code, accent: Color, tail: Modifier, laid: ((TextLayoutResult) -> Unit)?, modifier: Modifier) {
    var wrap by remember { mutableStateOf(false) }
    val ink = CodeInk(accent, Theme.palette.ok, Theme.palette.warn, Ink.Four)
    val text = remember(b, ink) { highlight(b.text, b.lang, ink) }
    Column(modifier.fillMaxWidth().background(Tone.Inset, Shapes.Tile)) {
        Head(b.lang.ifBlank { tr("code", "kod") }) {
            Box(
                Modifier.size(34.dp).pressable({ wrap = !wrap }, CircleShape, Color.Transparent, Tone.Raise, depth = 0.88f, stretch = true, feel = if (wrap) Feel.Off else Feel.On),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(18.dp).drawBehind { wrapMark(if (wrap) accent else Ink.Three) })
            }
            CopyButton({ b.text }, size = 34.dp)
        }
        val body = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp).then(tail)
        BasicText(
            text,
            if (wrap) body else Modifier.horizontalScroll(rememberScrollState()).then(body),
            style = Type.Mono,
            softWrap = wrap,
            onTextLayout = laid,
        )
    }
}

@Composable
private fun TableBlock(b: Block.Table, accent: Color, sources: List<Pair<String, String>>, modifier: Modifier) {
    val cols = b.head.size.coerceAtLeast(1)
    val palette = Theme.palette
    val cells = remember(b, accent, sources, palette) {
        (listOf(b.head) + b.rows).flatMap { r -> List(cols) { k -> inline(r.getOrElse(k) { "" }, accent, sources) } }
    }
    val rule = Tone.Raise
    val bottoms = remember { arrayOf(IntArray(0)) }
    Column(modifier.fillMaxWidth().background(Tone.Inset, Shapes.Tile)) {
        Head(tr("Table", "Tabela")) { CopyButton({ tsv(b) }, size = 34.dp) }
        Layout(
            content = {
                cells.forEachIndexed { k, s ->
                    val a = b.align.getOrElse(k % cols) { -1 }
                    BasicText(
                        s,
                        Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        style = (if (k < cols) Body.copy(color = Ink.One, fontWeight = FontWeight.SemiBold) else Body).copy(
                            textAlign = when (a) {
                                0 -> TextAlign.Center
                                1 -> TextAlign.End
                                else -> TextAlign.Start
                            },
                        ),
                    )
                }
            },
            // The scroll keeps the minimum width, so a narrow table still spans the block.
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp).drawBehind {
                val ys = bottoms[0]
                if (ys.isEmpty()) return@drawBehind
                drawRect(rule, Offset.Zero, Size(size.width, ys[0].toFloat()))
                for (r in 1 until ys.size - 1) drawRect(rule, Offset(0f, ys[r] - 0.5f.dp.toPx()), Size(size.width, 1.dp.toPx()))
            },
        ) { ms, c ->
            val cap = 260.dp.roundToPx()
            val widths = IntArray(cols)
            ms.forEachIndexed { k, m -> widths[k % cols] = max(widths[k % cols], m.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(cap)) }
            val spare = c.minWidth - widths.sum()
            if (spare > 0) for (k in 0 until cols) widths[k] += spare / cols
            val ps = ms.mapIndexed { k, m -> m.measure(Constraints.fixedWidth(widths[k % cols])) }
            val rows = (ps.size + cols - 1) / cols
            val heights = IntArray(rows) { r -> (0 until cols).maxOf { ps.getOrNull(r * cols + it)?.height ?: 0 } }
            var acc = 0
            bottoms[0] = IntArray(rows) { r -> acc += heights[r]; acc }
            layout(max(widths.sum(), c.minWidth), heights.sum()) {
                var y = 0
                for (r in 0 until rows) {
                    var x = 0
                    for (k in 0 until cols) {
                        ps.getOrNull(r * cols + k)?.place(x, y)
                        x += widths[k]
                    }
                    y += heights[r]
                }
            }
        }
    }
}

/** Pictures an answer shows, kept while the app runs so scrolling never fetches one twice. */
private object Pictures {
    private val cache = LruCache<String, ImageBitmap>(24)

    fun kept(url: String): ImageBitmap? = cache.get(url)

    fun fetch(url: String): ImageBitmap? {
        val image = runCatching {
            val bytes = when {
                url.startsWith("data:image/") -> Base64.decode(url.substringAfter("base64,"), Base64.DEFAULT)
                url.startsWith("https://") -> (URL(url).openConnection() as HttpURLConnection).run {
                    connectTimeout = 8000
                    readTimeout = 8000
                    try {
                        if (responseCode !in 200..299 || contentLengthLong > MAX_BYTES) null else inputStream.use { it.readBytes() }
                    } finally {
                        disconnect()
                    }
                }
                else -> null
            }
            if (bytes == null || bytes.size > MAX_BYTES) {
                null
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (max(bounds.outWidth, bounds.outHeight) / sample > 1600) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }
        }.getOrNull()
        image?.let { cache.put(url, it) }
        return image
    }

    private const val MAX_BYTES = 12L * 1024 * 1024
}

@Composable
private fun PictureBlock(b: Block.Picture, modifier: Modifier) {
    val ctx = LocalContext.current
    val image by produceState(Pictures.kept(b.url), b.url) {
        if (value == null) value = withContext(Dispatchers.IO) { Pictures.fetch(b.url) }
    }
    val shown = image
    val label = b.alt.ifBlank { tr("Picture", "Obraz") }
    if (shown != null) {
        Image(
            shown,
            label,
            modifier.fillMaxWidth().heightIn(max = 360.dp).clip(Shapes.Tile)
                .pressable({ if (b.url.startsWith("http")) openPage(ctx, b.url) }, depth = 0.98f, feel = null),
            contentScale = ContentScale.Fit,
        )
    } else {
        Row(
            modifier.height(40.dp).pressable({ openPage(ctx, b.url) }, Shapes.ControlLg, Tone.Raise, Tone.Press, depth = 0.97f).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Glyph.Image, Ink.Three, size = 16.dp)
            Spacer(Modifier.width(8.dp))
            BasicText(label, Modifier.widthIn(max = 260.dp), style = Type.Meta.copy(color = Ink.Two), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The colours a code block is drawn in: keywords, strings, numbers, comments. */
@Immutable
private data class CodeInk(val keyword: Color, val string: Color, val number: Color, val comment: Color)

private class Lexicon(
    val keywords: Set<String>,
    val line: List<String>,
    val block: Pair<String, String>?,
    val quotes: String,
    val triple: Boolean = false,
    val anyCase: Boolean = false,
)

private fun words(s: String) = s.split(' ').filter { it.isNotEmpty() }.toSet()

private val CFamily = words(
    "if else for while do switch case default break continue return goto sizeof typedef struct union enum const static extern " +
        "volatile inline void char short int long float double signed unsigned bool true false nullptr NULL class public private protected " +
        "virtual override template typename namespace using new delete this throw try catch auto",
)
private val Kotlin = words(
    "fun val var if else when for while do return break continue class interface object data sealed enum open abstract override " +
        "private public internal protected companion import package in is as null true false this super try catch finally throw " +
        "suspend inline reified lateinit by get set init typealias const operator infix vararg it",
)
private val Java = words(
    "public private protected class interface enum extends implements static final void int long short byte char boolean float double " +
        "if else for while do switch case default break continue return new this super null true false try catch finally throw throws " +
        "import package abstract synchronized var record",
)
private val Script = words(
    "function return if else for while do switch case default break continue var let const new this class extends super import " +
        "export from as async await yield try catch finally throw typeof instanceof in of null undefined true false interface type " +
        "enum implements public private protected readonly static void number string boolean any never unknown",
)
private val Python = words(
    "def return if elif else for while break continue class import from as with try except finally raise pass lambda yield " +
        "global nonlocal in is not and or None True False async await self print",
)
private val Rust = words(
    "fn let mut const static if else match for while loop break continue return struct enum impl trait pub use mod crate self " +
        "Self super where as in ref move async await dyn true false Some None Ok Err unsafe type",
)
private val Go = words(
    "func package import var const type struct interface map chan go defer select if else for range switch case default break " +
        "continue return fallthrough goto nil true false",
)
private val Swift = words(
    "func let var if else guard for while repeat switch case default break continue return class struct enum protocol extension " +
        "import self Self init deinit nil true false try catch throw throws async await public private internal static",
)
private val Shell = words(
    "if then else elif fi for while do done case esac in function return export local echo cd exit set unset source sudo " +
        "param foreach begin end process",
)
private val Sql = words(
    "select from where and or not insert into values update set delete create table index view drop alter add join left right " +
        "inner outer on group by order having limit offset as distinct union all null is in like between case when then else end " +
        "primary key foreign references default exists count sum avg min max",
)
private val Literals = words("true false null yes no on off")

private fun lexicon(lang: String): Lexicon? {
    val slashes = listOf("//")
    val slash = "/*" to "*/"
    return when (lang.lowercase()) {
        "kotlin", "kt", "kts" -> Lexicon(Kotlin, slashes, slash, "\"'", triple = true)
        "java", "scala" -> Lexicon(Java, slashes, slash, "\"'")
        "js", "javascript", "jsx", "mjs", "ts", "typescript", "tsx" -> Lexicon(Script, slashes, slash, "\"'`")
        "py", "python", "python3" -> Lexicon(Python, listOf("#"), null, "\"'", triple = true)
        "rs", "rust" -> Lexicon(Rust, slashes, slash, "\"")
        "go", "golang" -> Lexicon(Go, slashes, slash, "\"'`")
        "c", "h", "cpp", "c++", "cc", "hpp", "cs", "csharp", "c#", "objc", "dart", "php" -> Lexicon(CFamily, slashes, slash, "\"'")
        "swift" -> Lexicon(Swift, slashes, slash, "\"")
        "sh", "bash", "zsh", "shell", "console", "ps1", "powershell", "pwsh", "fish" -> Lexicon(Shell, listOf("#"), null, "\"'", anyCase = true)
        "sql", "mysql", "postgres", "postgresql", "sqlite" -> Lexicon(Sql, listOf("--"), slash, "'\"", anyCase = true)
        "json", "jsonc", "json5" -> Lexicon(Literals, slashes, slash, "\"")
        "yaml", "yml", "toml", "ini", "conf", "dockerfile" -> Lexicon(Literals, listOf("#"), null, "\"'")
        "css", "scss", "less" -> Lexicon(emptySet(), emptyList(), slash, "\"'")
        "html", "xml", "svg", "vue" -> Lexicon(emptySet(), emptyList(), "<!--" to "-->", "\"")
        "rb", "ruby" -> Lexicon(Python + words("end do then unless until module require nil puts"), listOf("#"), null, "\"'")
        "lua" -> Lexicon(words("function local end if then else elseif for while do repeat until return break nil true false and or not in"), listOf("--"), null, "\"'")
        else -> null
    }
}

/** Code coloured the way editors do it: keywords, strings, numbers and comments, one pass, no parser. */
private fun highlight(code: String, lang: String, ink: CodeInk): AnnotatedString {
    val lex = lexicon(lang) ?: return AnnotatedString(code)
    val out = AnnotatedString.Builder(code)
    val n = code.length
    var i = 0
    fun paint(style: SpanStyle, from: Int, to: Int) = out.addStyle(style, from, to.coerceAtMost(n))
    val comment = SpanStyle(color = ink.comment, fontStyle = FontStyle.Italic)
    val string = SpanStyle(color = ink.string)
    val number = SpanStyle(color = ink.number)
    val keyword = SpanStyle(color = ink.keyword, fontWeight = FontWeight.Medium)
    while (i < n) {
        val ch = code[i]
        val block = lex.block
        when {
            block != null && code.startsWith(block.first, i) -> {
                val end = code.indexOf(block.second, i + block.first.length).let { if (it < 0) n else it + block.second.length }
                paint(comment, i, end)
                i = end
            }
            lex.line.any { code.startsWith(it, i) } && (ch != '#' || i == 0 || code[i - 1].isWhitespace()) -> {
                val end = code.indexOf('\n', i).let { if (it < 0) n else it }
                paint(comment, i, end)
                i = end
            }
            ch in lex.quotes -> {
                val triple = lex.triple && code.startsWith("$ch$ch$ch", i)
                var j = i + if (triple) 3 else 1
                while (j < n) {
                    val cj = code[j]
                    if (cj == '\\') {
                        j += 2
                        continue
                    }
                    if (triple && code.startsWith("$ch$ch$ch", j)) {
                        j += 3
                        break
                    }
                    if (!triple && cj == ch) {
                        j++
                        break
                    }
                    if (!triple && cj == '\n' && ch != '`') break
                    j++
                }
                paint(string, i, j)
                i = j
            }
            ch.isDigit() && (i == 0 || !(code[i - 1].isLetterOrDigit() || code[i - 1] == '_')) -> {
                var j = i + 1
                while (j < n && (code[j].isLetterOrDigit() || code[j] == '.' || code[j] == '_')) j++
                paint(number, i, j)
                i = j
            }
            ch.isLetter() || ch == '_' -> {
                var j = i + 1
                while (j < n && (code[j].isLetterOrDigit() || code[j] == '_')) j++
                val w = code.substring(i, j)
                if (w in lex.keywords || lex.anyCase && w.lowercase() in lex.keywords) paint(keyword, i, j)
                i = j
            }
            else -> i++
        }
    }
    return out.toAnnotatedString()
}

/** A 24-unit grid scaled to the box, stroked like the bold glyphs. */
internal fun DrawScope.strokeMark(c: Color, build: Path.(Float) -> Unit) {
    val u = size.minDimension / 24f
    drawPath(Path().apply { build(u) }, c, style = Stroke(2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Wrap: lines with the middle one turning back on itself. */
private fun DrawScope.wrapMark(c: Color) = strokeMark(c) { u ->
    moveTo(4f * u, 6f * u)
    lineTo(20f * u, 6f * u)
    moveTo(4f * u, 12f * u)
    lineTo(16.5f * u, 12f * u)
    cubicTo(19.5f * u, 12f * u, 19.5f * u, 18f * u, 16.5f * u, 18f * u)
    lineTo(12f * u, 18f * u)
    moveTo(14f * u, 16f * u)
    lineTo(12f * u, 18f * u)
    lineTo(14f * u, 20f * u)
    moveTo(4f * u, 18f * u)
    lineTo(8f * u, 18f * u)
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
