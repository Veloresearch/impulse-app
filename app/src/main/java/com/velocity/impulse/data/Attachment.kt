package com.velocity.impulse.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.velocity.impulse.ui.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Something added to a question: a picture the model looks at ([image], base64 JPEG), the pages of
 * a scanned PDF drawn as pictures ([pages]), or a file's words ([text]). [thumb] is the small
 * picture the chat keeps, as the panel does; [kind] names a document's format.
 */
data class Attachment(
    val name: String,
    val bytes: Long,
    val image: String? = null,
    val thumb: String? = null,
    val text: String? = null,
    val pages: List<String> = emptyList(),
    val kind: String? = null,
) {
    /** Everything the model is shown with the question. */
    val pictures: List<String> get() = listOfNotNull(image) + pages

    fun record(): JSONObject = JSONObject().put("name", name).put("bytes", bytes).put("state", "kept").apply {
        if (thumb != null) put("thumb", thumb)
        if (pictures.isNotEmpty()) put("looked", pictures.size)
        if (pages.isNotEmpty()) put("note", "a scanned ${kind ?: "PDF"}, shown as pictures")
    }

    companion object {
        private const val TEXT_MAX = 8L * 1024 * 1024
        private const val CONTAINER_MAX = 50L * 1024 * 1024
        /** Pages of a scan drawn for a model that can see. */
        private const val SCANNED_PAGES = 4
        private val BINARY = setOf(
            "zip", "gz", "xz", "bz2", "7z", "rar", "tar", "exe", "dll", "so", "dylib", "bin", "gguf", "mfy", "safetensors", "onnx", "pt", "pth",
            "doc", "xls", "ppt", "sqlite", "db", "wasm", "class", "jar", "o", "a", "pyc", "apk",
        )

        /**
         * Reads a picked file. Null for one that cannot go to the model, with [why] given the whole
         * reason, e.g. "report.doc: this is a binary file, not text". [vision]: the loaded model can see.
         */
        suspend fun read(ctx: Context, uri: Uri, vision: Boolean = true, why: (String) -> Unit): Attachment? = withContext(Dispatchers.IO) {
            val cr = ctx.contentResolver
            var name = "file"
            var size = 0L
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    name = c.getString(0) ?: name
                    size = if (c.isNull(1)) 0L else c.getLong(1)
                }
            }
            val type = cr.getType(uri).orEmpty()
            val ext = Extract.ext(name)
            fun no(reason: String): Attachment? = null.also { why("$name: $reason") }
            val image = type.startsWith("image/")
            val document = Extract.isDocument(name, type)
            val max = if (image || document) CONTAINER_MAX else TEXT_MAX
            when {
                size == 0L && cr.openInputStream(uri)?.use { it.read() } == -1 -> return@withContext no(Lang.tr("the file is empty", "plik jest pusty"))
                size > max -> return@withContext no(Lang.tr("it is ${mb(size)} - larger than ${mb(max)}", "ma ${mb(size)} - więcej niż ${mb(max)}"))
                type.startsWith("audio/") || type.startsWith("video/") -> return@withContext no(Lang.tr("Impulse reads text, not sound or video", "Impulse czyta tekst, nie dźwięk ani wideo"))
                ext in BINARY -> return@withContext no(Lang.tr("this is a binary file, not text", "to plik binarny, nie tekst"))
            }
            try {
                when {
                    image -> {
                        if (!vision) return@withContext no(Lang.tr("this model reads text only - load one that can see pictures", "ten model czyta tylko tekst - wczytaj taki, który widzi zdjęcia"))
                        val bitmap = upright(ctx, uri)
                            ?: return@withContext no(Lang.tr("save it as JPEG or PNG first", "zapisz go najpierw jako JPEG lub PNG"))
                        Attachment(name, size, jpeg(bitmap, SEEN, 85), "data:image/jpeg;base64," + jpeg(bitmap, THUMB, 82))
                    }
                    document -> {
                        val got = Extract.document(ctx, uri, name, type, if (vision) SCANNED_PAGES else 0)
                        when {
                            got.text.isNotBlank() -> Attachment(name, size, text = got.text, kind = got.kind)
                            got.pages.isNotEmpty() -> Attachment(name, size, pages = got.pages, kind = got.kind)
                            got.kind == "PDF" && !vision -> no(Lang.tr("it is a scan with no text in it - load a model that can see to read it", "to skan bez tekstu - wczytaj model, który widzi, żeby go przeczytać"))
                            else -> no(Lang.tr("there is no text in it", "nie ma w nim tekstu"))
                        }
                    }
                    else -> {
                        val raw = cr.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext no(Lang.tr("it could not be opened", "nie dało się go otworzyć"))
                        val text = raw.toString(Charsets.UTF_8)
                        when {
                            text.contains('\u0000') -> no(Lang.tr("this is a binary file, not text", "to plik binarny, nie tekst"))
                            text.isBlank() -> no(Lang.tr("there is no text in it", "nie ma w nim tekstu"))
                            else -> Attachment(name, size.takeIf { it > 0 } ?: raw.size.toLong(), text = text, kind = ext.uppercase().ifEmpty { null })
                        }
                    }
                }
            } catch (e: SecurityException) {
                no(Lang.tr("it is protected with a password", "jest chroniony hasłem"))
            } catch (e: Exception) {
                no(Lang.tr("it could not be read (${e.javaClass.simpleName})", "nie dało się go odczytać (${e.javaClass.simpleName})"))
            } catch (e: OutOfMemoryError) {
                no(Lang.tr("it is too big to read on this phone", "jest za duży, by przeczytać go na tym telefonie"))
            }
        }

        private fun mb(n: Long) = if (n >= 1024 * 1024) "${n / (1024 * 1024)} MB" else "${n / 1024} KB"

        /** Longest side of the picture the model sees and the phone keeps. */
        private const val SEEN = 1536

        /** Longest side of the thumbnail the chat record carries to every device. */
        private const val THUMB = 384

        /**
         * The picture decoded no larger than it needs to be and turned the way it was taken: cameras
         * store the sensor's rows and say in EXIF how to rotate or mirror them.
         */
        private fun upright(ctx: Context, uri: Uri): Bitmap? {
            val cr = ctx.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SEEN) sample *= 2
            val b = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
            val turn = runCatching {
                cr.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
            val m = Matrix()
            when (turn) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> m.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.apply { setRotate(180f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSPOSE -> m.apply { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> m.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> m.apply { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> m.setRotate(-90f)
                else -> return b
            }
            return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
        }

        private fun jpeg(b: Bitmap, longest: Int, quality: Int): String {
            val scale = minOf(1f, longest.toFloat() / maxOf(b.width, b.height))
            val s = if (scale < 1f) Bitmap.createScaledBitmap(b, (b.width * scale).toInt().coerceAtLeast(1), (b.height * scale).toInt().coerceAtLeast(1), true) else b
            val out = ByteArrayOutputStream()
            s.compress(Bitmap.CompressFormat.JPEG, quality, out)
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }
    }
}

/**
 * What a question was sent with, kept on the phone by message id: the pictures the model looked at
 * and the files' words, so asking again, editing and later questions get them back. The chat record
 * keeps only names and thumbnails, as the panel's does.
 */
class Media(private val dir: File) {
    private fun of(id: String) = File(dir, id.replace(Regex("[^A-Za-z0-9_-]"), "_"))

    fun keep(id: String, pictures: List<String>, docs: List<Doc>) {
        if (pictures.isEmpty() && docs.isEmpty()) return
        val d = of(id).apply { mkdirs() }
        pictures.forEachIndexed { i, p -> File(d, "$i.jpg").writeBytes(Base64.decode(p, Base64.NO_WRAP)) }
        docs.forEachIndexed { i, doc -> File(d, "$i.txt").writeText(doc.name + "\n" + doc.text) }
    }

    /** The full picture [slot] of a question sent from this phone, null when it was sent elsewhere. */
    fun picture(id: String, slot: Int): File? = File(of(id), "$slot.jpg").takeIf { it.isFile }

    fun pictures(id: String): List<String> = of(id).listFiles { f -> f.name.endsWith(".jpg") }.orEmpty()
        .sortedBy { it.nameWithoutExtension.toIntOrNull() ?: 0 }.map { Base64.encodeToString(it.readBytes(), Base64.NO_WRAP) }

    fun docs(id: String): List<Doc> = of(id).listFiles { f -> f.name.endsWith(".txt") }.orEmpty()
        .sortedBy { it.nameWithoutExtension.toIntOrNull() ?: 0 }.map { f -> f.readText().let { Doc(it.substringBefore('\n'), it.substringAfter('\n')) } }

    fun copy(from: String, to: String) {
        val src = of(from)
        if (src.isDirectory) src.copyRecursively(of(to), overwrite = true)
    }

    fun drop(ids: Collection<String>) = ids.forEach { of(it).deleteRecursively() }

    fun clear() = dir.deleteRecursively()
}
