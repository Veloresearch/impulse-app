package com.velocity.impulse.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.text.Html
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import java.util.zip.ZipInputStream
import kotlin.math.abs

/** What came out of a file: its words, or pictures of its pages for a scan, and the format's name for people. */
class Extracted(val text: String, val kind: String, val pages: List<String> = emptyList())

/** Documents read on the phone, the formats the panel reads (data/extract.ts), so only their text travels. */
object Extract {
    private val DOCUMENTS = setOf("pdf", "docx", "pptx", "xlsx", "odt", "odp", "ods", "rtf", "html", "htm")

    fun ext(name: String) = name.substringAfterLast('.', "").lowercase()

    fun isDocument(name: String, type: String) = ext(name) in DOCUMENTS || type == "application/pdf"

    /** [scanned]: how many pages of a PDF with no text layer to draw as pictures; 0 when the model cannot see. */
    fun document(ctx: Context, uri: Uri, name: String, type: String, scanned: Int): Extracted {
        val e = ext(name)
        val cr = ctx.contentResolver
        fun bytes() = cr.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        return when {
            e == "pdf" || type == "application/pdf" -> pdf(ctx, uri, ::bytes, scanned)
            e == "rtf" -> Extracted(rtf(String(bytes(), Charsets.ISO_8859_1)), "RTF")
            e == "html" || e == "htm" -> Extracted(html(String(bytes(), Charsets.UTF_8)), "HTML")
            else -> {
                val zip = HashMap<String, String>()
                cr.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { z ->
                        while (true) {
                            val entry = z.nextEntry ?: break
                            val n = entry.name
                            if (n == "word/document.xml" || n == "content.xml" || n == "xl/sharedStrings.xml" ||
                                (n.endsWith(".xml") && (n.startsWith("ppt/slides/slide") || n.startsWith("xl/worksheets/sheet")))
                            ) zip[n] = z.readBytes().toString(Charsets.UTF_8)
                        }
                    }
                }
                fun numbered(prefix: String) = zip.keys.filter { it.startsWith(prefix) }.sortedBy(::num)
                when (e) {
                    "docx" -> Extracted(xml(zip["word/document.xml"].orEmpty(), "w:p"), "Word")
                    "pptx" -> Extracted(numbered("ppt/slides/slide").mapIndexed { i, p -> "Slide ${i + 1}\n${xml(zip[p]!!, "a:p")}" }.joinToString("\n\n"), "PowerPoint")
                    "xlsx" -> Extracted(xlsx(zip, numbered("xl/worksheets/sheet")), "Excel")
                    else -> Extracted(xml(zip["content.xml"].orEmpty(), "text:p", "table:table-row"), "OpenDocument")
                }
            }
        }
    }

    private fun num(path: String) = Regex("(\\d+)\\.xml$").find(path)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    /** Words out of XML: a line break at the end of every paragraph (or row), tags dropped. */
    private fun xml(src: String, vararg blocks: String): String {
        var s = src
        for (b in blocks) s = s.replace("</$b>", "\n")
        s = s.replace(Regex("<w:tab/>|<text:tab/>|<a:tab/>"), "\t")
            .replace(Regex("<w:br/>|<text:line-break/>|<a:br/>"), "\n")
            .replace("</table:table-cell>", "\t")
            .replace(Regex("<[^>]+>"), "")
        return entities(s).lines().joinToString("\n") { it.trimEnd(' ', '\t') }.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun entities(s: String) = s
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
        .replace(Regex("&#(\\d+);")) { m -> codePoint(m.groupValues[1].toIntOrNull()) }
        .replace(Regex("&#x([0-9a-fA-F]+);")) { m -> codePoint(m.groupValues[1].toIntOrNull(16)) }
        .replace("&amp;", "&")

    private fun codePoint(n: Int?) = n?.let { runCatching { String(Character.toChars(it)) }.getOrNull() }.orEmpty()

    /** A workbook as tab-separated rows, sheet by sheet; cells that hold shared text are looked up. */
    private fun xlsx(zip: Map<String, String>, sheets: List<String>): String {
        val shared = zip["xl/sharedStrings.xml"].orEmpty().split("</si>").dropLast(1).map { entities(it.replace(Regex("<[^>]+>"), "")) }
        val cell = Regex("<c ([^>]*?)(?:/>|>([\\s\\S]*?)</c>)")
        return sheets.mapIndexed { i, p ->
            val rows = zip[p]!!.split("</row>").dropLast(1).map { row ->
                cell.findAll(row).joinToString("\t") { c ->
                    val body = c.groupValues[2]
                    val v = Regex("<v>([\\s\\S]*?)</v>").find(body)?.groupValues?.get(1) ?: Regex("<t[^>]*>([\\s\\S]*?)</t>").find(body)?.groupValues?.get(1) ?: ""
                    if (Regex("(^|\\s)t=\"s\"").containsMatchIn(c.groupValues[1])) shared.getOrElse(v.toIntOrNull() ?: -1) { "" } else entities(v)
                }.trimEnd('\t')
            }.filter { it.isNotBlank() }
            "Sheet ${i + 1}\n${rows.joinToString("\n")}"
        }.joinToString("\n\n")
    }

    private fun rtf(src: String) = src
        .replace(Regex("\\\\par[d]?"), "\n")
        .replace(Regex("\\\\'([0-9a-fA-F]{2})")) { it.groupValues[1].toInt(16).toChar().toString() }
        .replace(Regex("\\\\u(-?\\d+)\\??")) { ((it.groupValues[1].toInt() + 65536) % 65536).toChar().toString() }
        .replace(Regex("\\{\\\\\\*[^{}]*\\}|\\\\[a-zA-Z]+-?\\d* ?|[{}]"), "")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

    private fun html(src: String): String {
        val body = src.replace(Regex("(?is)<(script|style|noscript)\\b.*?</\\1>"), "")
        return Html.fromHtml(body, Html.FROM_HTML_MODE_LEGACY).toString().replace('\uFFFC', ' ').replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun pdf(ctx: Context, uri: Uri, bytes: () -> ByteArray, scanned: Int): Extracted {
        val text = (if (Build.VERSION.SDK_INT >= 35) runCatching { platformText(ctx, uri) }.getOrNull() else null)
            ?.takeIf { it.isNotBlank() } ?: PdfText(bytes()).read()
        if (text.isNotBlank() || scanned <= 0) return Extracted(text, "PDF")
        return Extracted("", "PDF", drawPages(ctx, uri, scanned))
    }

    @androidx.annotation.RequiresApi(35)
    private fun platformText(ctx: Context, uri: Uri): String {
        val fd = ctx.contentResolver.openFileDescriptor(uri, "r") ?: return ""
        return PdfRenderer(fd).use { r ->
            (0 until r.pageCount).mapNotNull { i ->
                r.openPage(i).use { p -> p.textContents.joinToString("\n") { it.text }.trim() }.takeIf { it.isNotEmpty() }?.let { "Page ${i + 1}\n$it" }
            }.joinToString("\n\n")
        }
    }

    /** A scan's first pages as JPEG, at a size a vision model reads well. */
    private fun drawPages(ctx: Context, uri: Uri, max: Int): List<String> {
        val fd = ctx.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
        return PdfRenderer(fd).use { r ->
            (0 until minOf(r.pageCount, max)).map { i ->
                r.openPage(i).use { p ->
                    val scale = minOf(2f, 1400f / maxOf(p.width, p.height))
                    val bmp = Bitmap.createBitmap((p.width * scale).toInt().coerceAtLeast(1), (p.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    bmp.eraseColor(Color.WHITE)
                    p.render(bmp, null, Matrix().apply { setScale(scale, scale) }, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val out = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 88, out)
                    bmp.recycle()
                    Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                }
            }
        }
    }
}

/**
 * The text layer of a PDF where Android has no reader for it (before 15): the page tree, Flate
 * streams, object streams and ToUnicode maps. Text inside form XObjects and encrypted files is not read.
 */
private class PdfText(bytes: ByteArray) {
    private val src = String(bytes, Charsets.ISO_8859_1)
    private val dicts = HashMap<Int, String>()
    private val streams = HashMap<Int, IntRange>()
    private val fonts = HashMap<Int, Font>()

    private class Font(val map: Map<Int, String>?, val width: Int)

    init {
        var at = 0
        while (true) {
            val m = OBJ.find(src, at) ?: break
            val n = m.groupValues[1].toInt()
            val body = m.range.last + 1
            val end = src.indexOf("endobj", body).let { if (it < 0) src.length else it }
            val kw = src.indexOf("stream", body)
            if (kw in body until end) {
                var start = kw + 6
                if (src.startsWith("\r\n", start)) start += 2 else if (src.startsWith("\n", start) || src.startsWith("\r", start)) start++
                val dict = src.substring(body, kw)
                val length = value(dict, "Length")?.trim()?.toIntOrNull()
                val stop = length?.takeIf { start + it <= src.length && src.indexOf("endstream", start + it).let { e -> e >= 0 && e - (start + it) < 4 } }?.let { start + it }
                    ?: src.indexOf("endstream", start).let { if (it < 0) src.length else it }
                dicts[n] = dict
                streams[n] = start until stop
                at = src.indexOf("endobj", stop).let { if (it < 0) src.length else it + 6 }
            } else {
                dicts[n] = src.substring(body, end)
                at = end + 6
            }
            if (at >= src.length) break
        }
        for ((n, d) in dicts.toList()) {
            if (!Regex("/Type\\s*/ObjStm").containsMatchIn(d)) continue
            val data = data(n) ?: continue
            val count = value(d, "N")?.trim()?.toIntOrNull() ?: continue
            val first = value(d, "First")?.trim()?.toIntOrNull() ?: continue
            if (first > data.length) continue
            val head = data.substring(0, first).trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }
            for (i in 0 until minOf(count, head.size / 2)) {
                val from = first + head[i * 2 + 1]
                val to = if (i + 1 < head.size / 2) first + head[i * 2 + 3] else data.length
                if (from in 0..to && to <= data.length) dicts.putIfAbsent(head[i * 2], data.substring(from, to))
            }
        }
    }

    fun read(): String {
        if (src.contains("/Encrypt")) return ""
        val text = pages().mapIndexedNotNull { i, (page, res) ->
            val content = contents(page).joinToString("\n") { data(it).orEmpty() }
            words(content, fontsOf(res)).takeIf { it.isNotBlank() }?.let { "Page ${i + 1}\n$it" }
        }.joinToString("\n\n")
        val letters = text.count { it.isLetterOrDigit() }
        return if (letters < text.length / 3) "" else text
    }

    /** Each page with the resources it uses (inherited from its parents when it has none). */
    private fun pages(): List<Pair<String, String?>> {
        val out = mutableListOf<Pair<String, String?>>()
        val seen = HashSet<Int>()
        fun walk(n: Int, inherited: String?) {
            if (!seen.add(n) || out.size > 5000) return
            val d = dicts[n] ?: return
            val res = value(d, "Resources") ?: inherited
            if (Regex("/Type\\s*/Pages\\b").containsMatchIn(d)) {
                value(d, "Kids")?.let { k -> REF.findAll(resolve(k)).forEach { walk(it.groupValues[1].toInt(), res) } }
            } else {
                out += d to res
            }
        }
        val catalog = dicts.values.firstOrNull { Regex("/Type\\s*/Catalog\\b").containsMatchIn(it) }
        catalog?.let { value(it, "Pages") }?.let { REF.find(it) }?.let { walk(it.groupValues[1].toInt(), null) }
        if (out.isEmpty()) dicts.keys.sorted().forEach { n -> dicts[n]?.takeIf { Regex("/Type\\s*/Page(?![a-zA-Z])").containsMatchIn(it) }?.let { out += it to value(it, "Resources") } }
        return out
    }

    private fun contents(page: String): List<Int> {
        val v = value(page, "Contents") ?: return emptyList()
        val direct = REF.matchEntire(v.trim())?.let { m -> m.groupValues[1].toInt() }
        val list = if (direct != null && streams[direct] == null) resolve(v) else v
        return if (direct != null && streams[direct] != null) listOf(direct) else REF.findAll(list).map { it.groupValues[1].toInt() }.toList()
    }

    private fun fontsOf(res: String?): Map<String, Font> {
        val r = res?.let(::resolve) ?: return emptyMap()
        val f = value(r, "Font")?.let(::resolve) ?: return emptyMap()
        return Regex("/([^\\s/<>\\[\\]()]+)\\s+(\\d+)\\s+\\d+\\s+R").findAll(f).associate { m -> m.groupValues[1] to font(m.groupValues[2].toInt()) }
    }

    private fun font(n: Int): Font = fonts.getOrPut(n) {
        val d = dicts[n].orEmpty()
        val wide = d.contains("Identity-H") || d.contains("Identity-V") || Regex("/Subtype\\s*/Type0").containsMatchIn(d)
        val cmap = value(d, "ToUnicode")?.let { REF.find(it) }?.let { data(it.groupValues[1].toInt()) }
        cmap?.let { cmap(it, if (wide) 2 else 1) } ?: Font(null, if (wide) 2 else 1)
    }

    private fun cmap(text: String, fallback: Int): Font {
        val width = Regex("begincodespacerange\\s*<([0-9A-Fa-f]+)>").find(text)?.groupValues?.get(1)?.length?.div(2) ?: fallback
        val map = HashMap<Int, String>()
        for (block in Regex("beginbfchar([\\s\\S]*?)endbfchar").findAll(text)) {
            for (p in Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]*)>").findAll(block.groupValues[1])) {
                p.groupValues[1].toIntOrNull(16)?.let { map[it] = utf16(p.groupValues[2]) }
            }
        }
        for (block in Regex("beginbfrange([\\s\\S]*?)endbfrange").findAll(text)) {
            for (r in Regex("<([0-9A-Fa-f]+)>\\s*<([0-9A-Fa-f]+)>\\s*(<[0-9A-Fa-f]*>|\\[[^\\]]*\\])").findAll(block.groupValues[1])) {
                val lo = r.groupValues[1].toIntOrNull(16) ?: continue
                val hi = (r.groupValues[2].toIntOrNull(16) ?: continue).coerceAtMost(lo + 65535)
                val dst = r.groupValues[3]
                if (dst.startsWith("[")) {
                    Regex("<([0-9A-Fa-f]*)>").findAll(dst).forEachIndexed { i, h -> if (lo + i <= hi) map[lo + i] = utf16(h.groupValues[1]) }
                } else {
                    val base = utf16(dst.trim('<', '>'))
                    if (base.isEmpty()) continue
                    for (c in lo..hi) map[c] = base.dropLast(1) + (base.last() + (c - lo))
                }
            }
        }
        return Font(map, width)
    }

    private fun utf16(hex: String): String {
        val h = if (hex.length % 4 == 0) hex else hex.padStart((hex.length + 3) / 4 * 4, '0')
        return String(ByteArray(h.length / 2) { h.substring(it * 2, it * 2 + 2).toInt(16).toByte() }, Charsets.UTF_16BE)
    }

    private fun data(n: Int): String? {
        val range = streams[n] ?: return null
        val d = dicts[n].orEmpty()
        val raw = src.substring(range.first, minOf(range.last + 1, src.length))
        return when {
            !d.contains("/Filter") -> raw
            d.contains("/FlateDecode") && !Regex("/(DCT|JPX|CCITTFax|JBIG2|LZW|ASCII85|ASCIIHex)Decode").containsMatchIn(d) -> inflate(raw)
            else -> null
        }
    }

    private fun inflate(raw: String): String {
        val inf = Inflater()
        inf.setInput(raw.toByteArray(Charsets.ISO_8859_1))
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16384)
        try {
            while (!inf.finished()) {
                val k = inf.inflate(buf)
                if (k == 0 && (inf.needsInput() || inf.needsDictionary())) break
                out.write(buf, 0, k)
            }
        } catch (_: Exception) {
        } finally {
            inf.end()
        }
        return out.toString("ISO-8859-1")
    }

    private fun resolve(v: String): String = REF.matchEntire(v.trim())?.let { dicts[it.groupValues[1].toInt()]?.trim() } ?: v

    /** One value of a dictionary, raw: a reference, a name, a number, a nested dictionary or an array. */
    private fun value(dict: String, key: String): String? {
        val m = Regex("/$key(?=[\\s/<\\[(])").find(dict) ?: return null
        var i = m.range.last + 1
        while (i < dict.length && dict[i].isWhitespace()) i++
        if (i >= dict.length) return null
        REF.find(dict, i)?.takeIf { it.range.first == i }?.let { return it.value }
        val start = i
        when {
            dict.startsWith("<<", i) -> {
                var depth = 0
                while (i < dict.length) {
                    if (dict.startsWith("<<", i)) { depth++; i += 2; continue }
                    if (dict.startsWith(">>", i)) { depth--; i += 2; if (depth == 0) break; continue }
                    i++
                }
            }
            dict[i] == '[' -> {
                var depth = 0
                while (i < dict.length) {
                    if (dict[i] == '[') depth++ else if (dict[i] == ']') { depth--; if (depth == 0) { i++; break } }
                    i++
                }
            }
            else -> {
                i++
                while (i < dict.length && !dict[i].isWhitespace() && dict[i] !in "/<>[]()") i++
            }
        }
        return dict.substring(start, i)
    }

    private sealed interface Op
    private class Str(val raw: String) : Op
    private class Num(val v: Double) : Op
    private class Name(val v: String) : Op
    private class Arr(val items: List<Op>) : Op
    private object Mark : Op

    /** A page's content stream read for its text: Tf, Tj, TJ, ' and " for the words, Td, T* and Tm for the lines. */
    private fun words(c: String, fonts: Map<String, Font>): String {
        val out = StringBuilder()
        val stack = ArrayList<Op>()
        var font: Font? = null
        var lastY: Double? = null
        fun line() { if (out.isNotEmpty() && out.last() != '\n') out.append('\n') }
        fun space() { if (out.isNotEmpty() && !out.last().isWhitespace()) out.append(' ') }
        fun show(s: Str) = out.append(decode(s.raw, font))
        var i = 0
        while (i < c.length) {
            val ch = c[i]
            when {
                ch.isWhitespace() -> i++
                ch == '%' -> while (i < c.length && c[i] != '\n' && c[i] != '\r') i++
                ch == '(' -> {
                    val sb = StringBuilder()
                    var depth = 1
                    i++
                    while (i < c.length && depth > 0) {
                        val x = c[i]
                        when {
                            x == '\\' && i + 1 < c.length -> {
                                val y = c[i + 1]
                                i += 2
                                when (y) {
                                    'n' -> sb.append('\n'); 'r' -> sb.append('\r'); 't' -> sb.append('\t')
                                    'b' -> sb.append('\b'); 'f' -> sb.append('\u000C')
                                    '\r', '\n' -> if (y == '\r' && i < c.length && c[i] == '\n') i++
                                    in '0'..'7' -> {
                                        var o = y - '0'
                                        var k = 0
                                        while (k < 2 && i < c.length && c[i] in '0'..'7') { o = o * 8 + (c[i] - '0'); i++; k++ }
                                        sb.append((o and 0xFF).toChar())
                                    }
                                    else -> sb.append(y)
                                }
                                continue
                            }
                            x == '(' -> depth++
                            x == ')' -> { depth--; if (depth == 0) { i++; break } }
                        }
                        sb.append(x)
                        i++
                    }
                    stack += Str(sb.toString())
                }
                ch == '<' && c.startsWith("<<", i) -> {
                    var depth = 0
                    while (i < c.length) {
                        if (c.startsWith("<<", i)) { depth++; i += 2; continue }
                        if (c.startsWith(">>", i)) { depth--; i += 2; if (depth == 0) break; continue }
                        i++
                    }
                    stack += Mark
                }
                ch == '<' -> {
                    val end = c.indexOf('>', i).let { if (it < 0) c.length else it }
                    val hex = c.substring(i + 1, end).filter { it.isLetterOrDigit() }.let { if (it.length % 2 == 1) it + "0" else it }
                    stack += Str(String(CharArray(hex.length / 2) { (hex.substring(it * 2, it * 2 + 2).toIntOrNull(16) ?: 0).toChar() }))
                    i = end + 1
                }
                ch == '[' -> { stack += Mark; i++ }
                ch == ']' -> {
                    val at = stack.indexOfLast { it === Mark }
                    val items = if (at >= 0) stack.subList(at + 1, stack.size).toList() else emptyList()
                    if (at >= 0) while (stack.size > at) stack.removeAt(stack.size - 1)
                    stack += Arr(items)
                    i++
                }
                ch == '/' -> {
                    var j = i + 1
                    while (j < c.length && !c[j].isWhitespace() && c[j] !in "/<>[]()%{}") j++
                    stack += Name(c.substring(i + 1, j))
                    i = j
                }
                ch.isDigit() || ch == '-' || ch == '+' || ch == '.' -> {
                    var j = i + 1
                    while (j < c.length && (c[j].isDigit() || c[j] == '.')) j++
                    stack += Num(c.substring(i, j).toDoubleOrNull() ?: 0.0)
                    i = j
                }
                else -> {
                    var j = i + 1
                    while (j < c.length && !c[j].isWhitespace() && c[j] !in "/<>[]()%") j++
                    val op = c.substring(i, j)
                    i = j
                    when (op) {
                        "Tf" -> (stack.getOrNull(stack.size - 2) as? Name)?.let { font = fonts[it.v] }
                        "Tj" -> (stack.lastOrNull() as? Str)?.let(::show)
                        "'", "\"" -> { line(); (stack.lastOrNull() as? Str)?.let(::show) }
                        "TJ" -> (stack.lastOrNull() as? Arr)?.items?.forEach { e ->
                            when (e) {
                                is Str -> show(e)
                                is Num -> { if (e.v < -180) space() }
                                else -> {}
                            }
                        }
                        "Td", "TD" -> {
                            val ty = (stack.lastOrNull() as? Num)?.v ?: 0.0
                            if (abs(ty) > 0.01) line() else space()
                        }
                        "T*" -> line()
                        "Tm" -> {
                            val y = (stack.lastOrNull() as? Num)?.v
                            if (y != null && lastY != null && abs(y - lastY!!) > 1) line()
                            lastY = y
                        }
                        "BI" -> {
                            val e = Regex("\\sEI\\s").find(c, i)
                            i = e?.range?.last ?: c.length
                        }
                    }
                    stack.clear()
                }
            }
        }
        return out.lines().joinToString("\n") { it.replace(Regex("[ \\t]+"), " ").trim() }.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun decode(raw: String, font: Font?): String {
        val map = font?.map ?: return if (font?.width == 2) "" else raw.filter { it >= ' ' || it == '\t' }
        val w = font.width.coerceIn(1, 4)
        val sb = StringBuilder()
        var k = 0
        while (k + w <= raw.length) {
            var code = 0
            for (b in 0 until w) code = (code shl 8) or (raw[k + b].code and 0xFF)
            sb.append(map[code] ?: if (w == 1 && raw[k] >= ' ') raw[k].toString() else "")
            k += w
        }
        return sb.toString()
    }

    private companion object {
        val OBJ = Regex("(\\d+)\\s+\\d+\\s+obj\\b")
        val REF = Regex("(\\d+)\\s+(\\d+)\\s+R\\b")
    }
}

class Doc(val name: String, val text: String)

/**
 * Files in a question, as the model reads them (the panel's data/chatDocs.ts): whole when they fit,
 * otherwise their beginning, an outline and the parts that match the question, inside a budget
 * sized from the model's window.
 */
object Docs {
    private const val CHARS_PER_TOKEN = 3.5
    private const val EARLIER_MIN = 1500
    private const val TOOLS_TOKENS = 1500

    const val FILE_RULE =
        "The user's latest message comes with attached files; this turn is about them, not about earlier topics. Answer from the files' content. If the message is short or vague (e.g. \"check this\", \"obczaj to\", \"what do you think\", \"summarise\"), give a concise summary of the file - what it is, its key points and numbers - and a short take on it. Never treat the user's words as a product or brand name. Do not search the web unless the user asks to."

    /** How many characters of files fit this turn: a share of what the window leaves after the conversation, the answer and the instructions. */
    fun budget(window: Int, history: List<String>, maxNew: Int, share: Double): Int {
        val w = if (window > 0) window else 8192
        val used = history.sumOf { it.length } / CHARS_PER_TOKEN
        val free = maxOf(0.0, w - used - minOf(maxNew.toDouble(), w / 3.0) - 1800)
        return (free * share * CHARS_PER_TOKEN).toInt()
    }

    /** Tokens of a message as the service estimates them: bytes over 3.4, falling towards 1.8 as punctuation takes over, plus 8. */
    fun tokensOf(text: String): Int {
        val n = text.toByteArray().size
        if (n == 0) return 9
        val punct = text.count { !it.isLetterOrDigit() && !it.isWhitespace() }
        return kotlin.math.ceil(n / (3.4 - 1.6 * minOf(1.0, punct.toDouble() / n))).toInt() + 8
    }

    /** How long this turn's answer may be: the conversation comes first, the answer gets what it leaves, never less than [floor]. */
    fun answerRoom(window: Int, prompt: List<String>, tools: Boolean, want: Int, floor: Int): Int {
        if (window <= 0) return want
        val used = prompt.sumOf(::tokensOf) + if (tools) TOOLS_TOKENS else 0
        val left = (window - maxOf(window / 20, 512) - used - 256)
        return maxOf(minOf(floor, want), minOf(want, left))
    }

    private val STOP = ("the and for with this that what about are was were you your have has from into which there their then than them will would could should " +
        "a an of to in on at is it be or as by co jak jest czy nie na do za się sie że ze tak tam ten ta te tego tej tym mi mnie mam masz ma o od po przy dla jaki jaka jakie który która które i w z u").split(' ').toSet()

    private fun words(s: String) = Regex("[\\p{L}\\p{N}]+").findAll(s.lowercase()).map { it.value }.filter { it.length > 2 && it !in STOP }.toList()

    private fun stem(w: String) = when {
        w.length > 5 -> w.dropLast(2)
        w.length > 3 -> w.dropLast(1)
        else -> w
    }

    private val ALSO = mapOf(
        "cen" to listOf("price", "pricing", "cennik", "koszt", "zł", "pln", "eur", "usd"),
        "pric" to listOf("cena", "cennik", "koszt", "zł", "pln"),
        "cost" to listOf("cena", "cennik", "koszt", "price"),
        "kosz" to listOf("price", "cena", "cennik"),
        "harmonogr" to listOf("timeline", "schedule", "tydzień"),
        "timelin" to listOf("harmonogram", "week", "tydzień"),
        "kontakt" to listOf("contact", "email"),
        "contac" to listOf("kontakt", "email"),
    )

    private val HEADER = Regex("^(Slide|Page|Sheet) (\\d+)$")

    private fun outline(text: String, max: Int): String {
        val lines = text.split('\n')
        val out = mutableListOf<String>()
        for ((i, raw) in lines.withIndex()) {
            val l = raw.trim()
            if (HEADER.matches(l)) out += "$l: ${lines.getOrElse(i + 1) { "" }.trim().take(90)}"
            else if (Regex("^#{1,4} \\S.*").matches(l) || Regex("^\\d+(\\.\\d+)*[.)]? [A-ZĄĆĘŁŃÓŚŹŻ][^.!?]{2,70}$").matches(l) || Regex("^[A-ZĄĆĘŁŃÓŚŹŻ][A-ZĄĆĘŁŃÓŚŹŻ \\d&-]{3,60}$").matches(l)) out += l.take(90)
        }
        val sb = StringBuilder()
        for (l in out) {
            if (sb.length + l.length + 1 > max) break
            sb.append(l).append('\n')
        }
        return sb.toString().trim()
    }

    private fun sections(text: String): List<String> {
        val parts = text.split(Regex("\n(?=(?:Slide|Page|Sheet) \\d+\n)"))
        if (parts.size > 1) return parts
        val out = mutableListOf<String>()
        var cur = ""
        for (p in text.split(Regex("\n\\s*\n"))) {
            if (cur.isNotEmpty() && cur.length + p.length > 1500) {
                out += cur
                cur = ""
            }
            cur += (if (cur.isNotEmpty()) "\n\n" else "") + p
        }
        if (cur.isNotEmpty()) out += cur
        return out
    }

    /** One file cut to [budget] characters: whole when it fits; else its beginning, an outline, the section the question names and the ones sharing most words with it. */
    fun fit(text: String, budget: Int, question: String): String {
        if (text.length <= budget) return text
        val secs = sections(text)
        val head = text.take((budget * 0.3).toInt())
        val out = mutableListOf("$head\n[...]")
        var left = budget - head.length
        val toc = outline(text, minOf(1500, (budget * 0.15).toInt()))
        if (toc.isNotEmpty()) {
            out += "Outline:\n$toc"
            left -= toc.length + 10
        }
        val named = Regex("\\b(?:slide|slajd\\w*|page|strona|stronie|stron\\w*|sheet|arkusz\\w*)\\s*(?:nr\\.?\\s*)?(\\d+)", RegexOption.IGNORE_CASE)
            .findAll(question).mapNotNull { it.groupValues[1].toIntOrNull() }.toSet()
        val terms = HashSet<String>()
        for (w in words(question)) {
            terms += stem(w)
            for ((k, also) in ALSO) if (w.startsWith(k)) also.forEach { terms += stem(it) }
        }
        val scored = secs.mapIndexed { i, s ->
            val n = Regex("^(?:Slide|Page|Sheet) (\\d+)\n").find(s)?.groupValues?.get(1)?.toIntOrNull() ?: -1
            var score = if (n in named) 1000 else 0
            if (terms.isNotEmpty()) score += words(s).count { stem(it) in terms }
            Triple(s, i, score)
        }.filter { it.third > 0 && !head.contains(it.first.take(200)) }.sortedByDescending { it.third }
        val picked = mutableListOf<Pair<String, Int>>()
        for ((s, i) in scored) {
            val piece = if (s.length > left) s.take(maxOf(0, left)) else s
            if (piece.length < 200) break
            picked += piece to i
            left -= piece.length + 8
            if (left < 300) break
        }
        picked.sortBy { it.second }
        picked.forEach { out += it.first.trim() }
        if (left > 400 && picked.isEmpty()) out += text.substring(head.length, minOf(text.length, head.length + left)).trim()
        return out.joinToString("\n\n") + "\n[... the rest of this file is in this chat's memory]"
    }

    private fun blocks(docs: List<Doc>, budget: Int, question: String): String {
        val each = budget / docs.size
        return docs.joinToString("\n\n") { "<file name=\"${it.name.replace('"', '\'')}\">\n${fit(it.text, each, question)}\n</file>" }
    }

    /** The user's message with the files sent with it, in the message, the words last and as written. */
    fun withFiles(text: String, docs: List<Doc>, budget: Int): String {
        if (docs.isEmpty() || budget < 600) return text
        val ask = text.trim()
        return listOf(blocks(docs, budget, ask), "", ask.ifEmpty { "(No message - just the ${if (docs.size > 1) "files" else "file"}.)" }).joinToString("\n")
    }

    /** The files of earlier messages, for a follow-up: every one named as still attached, as many as fit with their text. */
    fun earlierBlock(docs: List<Doc>, budget: Int, question: String): String {
        if (docs.isEmpty()) return ""
        val names = docs.joinToString(", ") { "\"${it.name.replace('"', '\'')}\"" }
        val head = "Files the user attached earlier in this conversation are still attached: $names. When the user says \"the file\", \"the slides\", \"the price\", \"it\", they most likely mean these. Never say you do not have them."
        val fit = docs.take(budget / EARLIER_MIN)
        val memory = "The full text of every attached file is in this chat's memory; the parts that match the question come to you as memory excerpts."
        if (fit.isEmpty()) return "$head $memory"
        return "$head Answer from them when the question is about them; quote and name them.${if (fit.size < docs.size) " $memory" else ""}\n\n${blocks(fit, budget, question)}"
    }
}
