package com.velocity.impulse.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.velocity.impulse.R
import com.velocity.impulse.data.Chat
import com.velocity.impulse.data.Message
import com.velocity.impulse.data.objects
import com.velocity.impulse.ui.Lang.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes [chat] as "pdf", "md", "json" or "txt" into the cache and opens the share sheet with it.
 * [failed] gets a sentence to show when it could not be written.
 */
fun exportChat(context: Context, chat: Chat, format: String, failed: (String) -> Unit = {}) {
    val ext = when (format.lowercase()) {
        "pdf" -> "pdf"
        "md", "markdown" -> "md"
        "json" -> "json"
        else -> "txt"
    }
    val mime = when (ext) {
        "pdf" -> "application/pdf"
        "md" -> "text/markdown"
        "json" -> "application/json"
        else -> "text/plain"
    }
    CoroutineScope(Dispatchers.Main).launch {
        try {
            shareFile(context, "${fileName(chat.title)}.$ext", mime) { f ->
                when (ext) {
                    "pdf" -> ChatPdf(context, chat).write(f)
                    "md" -> f.writeText(markdown(chat))
                    "json" -> f.writeText(chat.raw.toString(2))
                    else -> f.writeText(plain(chat))
                }
            }
        } catch (e: Exception) {
            failed(tr("Could not export the chat (${e.javaClass.simpleName})", "Nie udało się wyeksportować czatu (${e.javaClass.simpleName})"))
        }
    }
}

/** Puts a JPEG into the phone's Pictures/Impulse, where the gallery shows it. */
fun savePicture(context: Context, jpeg: ByteArray, name: String): Boolean {
    if (Build.VERSION.SDK_INT < 29 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        // shortcut: asks and reports this tap as not saved; the next tap saves. Upgrade to a result callback if Android 8-9 users complain.
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()
            ?.let { ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0) }
        return false
    }
    return try {
        val cr = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name.substringBeforeLast('.').ifBlank { "Impulse" } + ".jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Impulse")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        val wrote = cr.openOutputStream(uri)?.use { it.write(jpeg) } != null
        if (!wrote) {
            cr.delete(uri, null, null)
            return false
        }
        if (Build.VERSION.SDK_INT >= 29) cr.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        true
    } catch (e: Exception) {
        false
    }
}

/** Writes a file named [name] with [write] and offers it to other apps. */
suspend fun shareFile(context: Context, name: String, mime: String, write: (File) -> Unit) {
    val file = withContext(Dispatchers.IO) {
        // Under the provider's "shots" path, the one folder it serves.
        val dir = File(context.cacheDir, "shots/out").apply { mkdirs() }
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { if (now - it.lastModified() > 86_400_000L) it.delete() }
        File(dir, name.replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), "_")).also(write)
    }
    val uri = FileProvider.getUriForFile(context, "com.velocity.impulse.files", file)
    val send = Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(file.name, uri)
    context.startActivity(Intent.createChooser(send, null).apply { if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
}

private fun fileName(title: String) = title.replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), " ").trim().take(60).ifBlank { "Chat" }

private val WHEN = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.ENGLISH)

private fun stamp(at: Long) = if (at > 0) WHEN.format(Date(at)) else ""

private fun answerText(m: Message) = m.text.replace(Regex("(?s)<think>.*?</think>"), "").trim()

private fun turns(chat: Chat) = chat.messages.filter { (it.role == "user" || it.role == "assistant") && !it.raw.optBoolean("streaming") }

private fun modelOf(chat: Chat, m: Message) =
    m.meta?.optString("model")?.ifBlank { null } ?: chat.raw.optString("modelName").takeIf { it.isNotBlank() && it != "null" } ?: "Impulse"

private fun attached(m: Message) = m.raw.optJSONArray("files").objects()
    .joinToString(", ") { "${it.optString("name")} (${sizeOf(it.optLong("bytes"))})" }

private fun receiptLine(r: JSONObject?): String? {
    if (r == null) return null
    val rate = r.optDouble("decode_tok_s", 0.0).takeIf { it > 0 }?.let { "%.1f t/s".format(Locale.US, it) }
    val first = r.optDouble("ttft_ms", 0.0).takeIf { it > 0 }?.let { "first word %.2fs".format(Locale.US, it / 1000) }
    val total = r.optDouble("total_ms", 0.0).takeIf { it > 0 }?.let { "total %.2fs".format(Locale.US, it / 1000) }
    return listOfNotNull(rate, first, total).joinToString(" · ").ifEmpty { null }
}

private fun markdown(chat: Chat) = buildString {
    append("# ${chat.title}\n*Created ${stamp(chat.raw.optLong("createdAt"))}*\n\n")
    chat.systemPrompt?.let { append("> **System prompt**:\n> ${it.replace("\n", "\n> ")}\n\n---\n\n") }
    for (m in turns(chat)) {
        if (m.role == "user") {
            append("### **You**\n\n${m.text}\n\n")
            if (m.raw.has("files")) append("*Attached: ${attached(m)}*\n\n")
        } else {
            append("### **${modelOf(chat, m)}**\n\n${answerText(m).ifEmpty { m.error.orEmpty() }}\n\n")
            m.sources.takeIf { it.isNotEmpty() }?.let { s -> append(s.mapIndexed { i, (t, u) -> "${i + 1}. [$t]($u)" }.joinToString("\n", "**Sources**\n\n", "\n\n")) }
            receiptLine(m.receipt)?.let { append("<small>*$it*</small>\n\n") }
        }
        append("---\n\n")
    }
}

private fun plain(chat: Chat) = buildString {
    append(chat.title).append('\n')
    append("Created ${stamp(chat.raw.optLong("createdAt"))}\n\n")
    chat.systemPrompt?.let { append("System prompt:\n$it\n\n") }
    for (m in turns(chat)) {
        val user = m.role == "user"
        append(if (user) "You" else modelOf(chat, m)).append(" · ").append(stamp(m.at)).append('\n')
        append(if (user) m.text else answerText(m).ifEmpty { m.error.orEmpty() }).append('\n')
        if (user && m.raw.has("files")) append("Attached: ${attached(m)}\n")
        if (!user) m.sources.forEachIndexed { i, (t, u) -> append("[${i + 1}] $t - $u\n") }
        append('\n')
    }
}

/** Weight, face and size changes inside a run of text. */
private class Run(private val axes: String, private val face: Typeface? = null, private val scale: Float = 1f) : MetricAffectingSpan() {
    override fun updateMeasureState(p: TextPaint) = apply(p)
    override fun updateDrawState(p: TextPaint) = apply(p)
    private fun apply(p: TextPaint) {
        face?.let { p.typeface = it }
        p.setFontVariationSettings(axes)
        p.textSize *= scale
    }
}

/**
 * The chat set as an A4 document, the way the panel's PDF reads: the title in the wordmark face,
 * then each turn under who spoke and when. Questions sit in a soft panel, answers are set from
 * their Markdown, code in mono on a tint.
 */
private class ChatPdf(ctx: Context, private val chat: Chat) {
    private val inter = ctx.resources.getFont(R.font.inter)
    private val mono = ctx.resources.getFont(R.font.jetbrains_mono)
    private val doc = PdfDocument()
    private var page: PdfDocument.Page? = null
    private lateinit var canvas: Canvas
    private var number = 0
    private var y = TOP

    private fun paint(size: Float, color: Int, axes: String, face: Typeface = inter) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = face
        textSize = size
        this.color = color
        setFontVariationSettings(axes)
    }

    private val brand = paint(26f, INK, "'wght' 300, 'opsz' 32")
    private val body = paint(10.5f, INK, "'wght' 400, 'opsz' 14")
    private val quiet = paint(10.5f, GREY, "'wght' 400, 'opsz' 14")
    private val meta = paint(8.5f, GREY, "'wght' 400, 'opsz' 14")
    private val who = paint(9f, ACCENT, "'wght' 600, 'opsz' 14")
    private val h1 = paint(14f, INK, "'wght' 600, 'opsz' 22")
    private val h2 = paint(12f, INK, "'wght' 600, 'opsz' 14")
    private val code = paint(8.8f, INK, "'wght' 400", mono)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

    fun write(file: File) {
        try {
            turnPage()
            block(chat.title.ifBlank { "Chat" }, brand, after = 6f)
            val count = turns(chat).size
            block(listOfNotNull(modelName(), stamp(chat.raw.optLong("createdAt")), "$count messages").joinToString(" · "), meta, after = 14f)
            rule()
            chat.systemPrompt?.let {
                heading("System prompt")
                block(it, quiet, after = 6f, bar = true)
                rule()
            }
            for (m in turns(chat)) {
                y += 10f
                if (m.role == "user") {
                    heading("You · ${stamp(m.at)}")
                    if (m.text.isNotBlank()) block(m.text.trim(), body, after = 4f, tint = PANEL)
                    if (m.raw.has("files")) block("Attached: ${attached(m)}", meta, after = 4f)
                } else {
                    heading("${modelOf(chat, m)} · ${stamp(m.at)}")
                    val text = answerText(m)
                    if (text.isEmpty()) block(m.error ?: "No answer", quiet, after = 4f) else answer(text)
                    if (m.sources.isNotEmpty()) block(
                        m.sources.mapIndexed { i, (t, u) -> "${i + 1}. $t - $u" }.joinToString("\n"), meta, after = 4f,
                    )
                    receiptLine(m.receipt)?.let { block(it, meta, after = 4f) }
                }
            }
            page?.let(doc::finishPage)
            file.outputStream().use(doc::writeTo)
        } finally {
            doc.close()
        }
    }

    private fun modelName() = chat.raw.optString("modelName").takeIf { it.isNotBlank() && it != "null" }

    private fun turnPage() {
        page?.let(doc::finishPage)
        number++
        val p = doc.startPage(PdfDocument.PageInfo.Builder(W, H, number).create())
        page = p
        canvas = p.canvas
        y = TOP
        val title = chat.title.ifBlank { "Chat" }.let { if (it.length > 70) it.take(69) + "…" else it }
        canvas.drawText(title, LEFT, H - 34f, meta)
        val n = number.toString()
        canvas.drawText(n, RIGHT - meta.measureText(n), H - 34f, meta)
    }

    /** A speaker line, kept on the same page as the first lines under it. */
    private fun heading(text: String) {
        if (y + 64f > BOTTOM) turnPage()
        block(text, who, after = 5f)
    }

    private fun rule() {
        if (y + 16f > BOTTOM) turnPage()
        fill.color = LINE
        canvas.drawRect(LEFT, y, RIGHT, y + 0.6f, fill)
        y += 14f
    }

    private fun answer(text: String) {
        for (part in parts(text)) when (part) {
            is Part.Para -> block(inline(part.text), body, after = 7f)
            is Part.Head -> {
                if (y + 48f > BOTTOM) turnPage()
                y += 4f
                block(inline(part.text), if (part.level <= 2) h1 else h2, after = 4f)
            }
            is Part.Item -> {
                val mark = part.mark + "  "
                val s = SpannableStringBuilder(mark).append(inline(part.text))
                s.setSpan(LeadingMarginSpan.Standard(0, body.measureText(mark).toInt()), 0, s.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
                block(s, body, after = 3f, indent = 6f + 12f * part.depth)
            }
            is Part.Quote -> block(inline(part.text), quiet, after = 6f, indent = 4f, bar = true)
            is Part.Code -> block(part.text, code, after = 8f, tint = PANEL)
            Part.Rule -> rule()
        }
    }

    private fun inline(s: String): CharSequence {
        val out = SpannableStringBuilder()
        var i = 0
        for (m in INLINE.findAll(s)) {
            out.append(s, i, m.range.first)
            val start = out.length
            val g = m.groups
            when {
                g[1] != null || g[2] != null -> {
                    out.append((g[1] ?: g[2])!!.value)
                    out.setSpan(Run("'wght' 600, 'opsz' 14"), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                g[3] != null -> {
                    out.append(g[3]!!.value)
                    out.setSpan(Run("'wght' 400", mono, 0.88f), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    out.setSpan(BackgroundColorSpan(PANEL), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                else -> out.append(g[4]!!.value)
            }
            i = m.range.last + 1
        }
        return out.append(s, i, s.length)
    }

    /**
     * Sets [text] at the cursor and breaks it across pages line by line. [tint] lays a soft panel
     * under it, [bar] a thin line beside it.
     */
    private fun block(text: CharSequence, paint: TextPaint, after: Float, indent: Float = 0f, tint: Int? = null, bar: Boolean = false) {
        val pad = if (tint != null) 9f else 0f
        val inset = indent + pad + if (bar) 10f else 0f
        val width = (RIGHT - LEFT - inset - pad).toInt()
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(paint.textSize * 0.42f, 1f)
            .setIncludePad(false)
            .build()
        var line = 0
        while (line < layout.lineCount) {
            val from = layout.getLineTop(line)
            var end = line
            while (end < layout.lineCount && layout.getLineBottom(end) - from + 2 * pad <= BOTTOM - y) end++
            if (end == line) {
                if (y > TOP) {
                    turnPage()
                    continue
                }
                end = line + 1
            }
            val to = layout.getLineBottom(end - 1)
            val h = (to - from).toFloat()
            if (tint != null) {
                fill.color = tint
                canvas.drawRoundRect(LEFT + indent, y, RIGHT, y + h + 2 * pad, 8f, 8f, fill)
            }
            if (bar) {
                fill.color = LINE
                canvas.drawRect(LEFT + indent, y + 1f, LEFT + indent + 2f, y + h - 1f, fill)
            }
            canvas.save()
            canvas.translate(LEFT + inset, y + pad - from)
            canvas.clipRect(0, from, width, to)
            layout.draw(canvas)
            canvas.restore()
            y += h + 2 * pad
            line = end
            if (line < layout.lineCount) turnPage()
        }
        y += after
    }

    private companion object {
        const val W = 595
        const val H = 842
        const val LEFT = 56f
        const val RIGHT = W - 56f
        const val TOP = 64f
        const val BOTTOM = H - 64f
        const val INK = 0xFF1C1A1F.toInt()
        const val GREY = 0xFF716A79.toInt()
        const val ACCENT = 0xFF6E5A8C.toInt()
        const val PANEL = 0xFFF3F1F5.toInt()
        const val LINE = 0xFFDCD8E0.toInt()
        val INLINE = Regex("\\*\\*(.+?)\\*\\*|__(.+?)__|`([^`]+)`|\\[([^\\]]+)]\\([^)]+\\)")
    }
}

private sealed class Part {
    class Para(val text: String) : Part()
    class Head(val text: String, val level: Int) : Part()
    class Item(val text: String, val mark: String, val depth: Int) : Part()
    class Quote(val text: String) : Part()
    class Code(val text: String) : Part()
    data object Rule : Part()
}

private val HEAD = Regex("^(#{1,6})\\s+(.*)$")
private val ITEM = Regex("^(\\s*)([-*+]|\\d+[.)])\\s+(.*)$")
private val RULE = Regex("^(-{3,}|\\*{3,}|_{3,})$")

/** An answer's Markdown as the blocks the PDF sets; a table goes in as aligned mono text. */
private fun parts(text: String): List<Part> {
    val out = ArrayList<Part>()
    val para = StringBuilder()
    fun flush() {
        if (para.isNotBlank()) out += Part.Para(para.toString().trim())
        para.clear()
    }
    val lines = text.lines()
    var i = 0
    while (i < lines.size) {
        val l = lines[i]
        val t = l.trim()
        when {
            t.startsWith("```") -> {
                flush()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) code.append(lines[i++]).append('\n')
                out += Part.Code(code.toString().trimEnd())
            }
            t.isEmpty() -> flush()
            RULE.matches(t) -> {
                flush()
                out += Part.Rule
            }
            HEAD.matches(t) -> {
                flush()
                val m = HEAD.find(t)!!
                out += Part.Head(m.groupValues[2], m.groupValues[1].length)
            }
            ITEM.matches(l) -> {
                flush()
                val m = ITEM.find(l)!!
                val mark = m.groupValues[2].let { if (it[0].isDigit()) it else "•" }
                out += Part.Item(m.groupValues[3], mark, (m.groupValues[1].length / 2).coerceAtMost(3))
            }
            t.startsWith(">") -> {
                flush()
                out += Part.Quote(t.trimStart('>', ' '))
            }
            t.startsWith("|") -> {
                flush()
                val rows = ArrayList<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    val cells = lines[i].trim().trim('|').split('|').map { it.trim() }
                    if (!cells.all { it.matches(Regex(":?-{2,}:?")) }) rows += cells
                    i++
                }
                i--
                val widths = (0 until (rows.maxOfOrNull { it.size } ?: 0)).map { c -> rows.maxOf { it.getOrElse(c) { "" }.length }.coerceAtMost(28) }
                out += Part.Code(rows.joinToString("\n") { r -> r.mapIndexed { c, s -> s.take(28).padEnd(widths[c]) }.joinToString("  ").trimEnd() })
            }
            else -> para.append(l).append('\n')
        }
        i++
    }
    flush()
    return out
}
