package com.velocity.impulse.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Something added to a question: a picture the model looks at ([image], base64 JPEG) or a text
 * file read into the question ([text]). [thumb] is the small picture the chat keeps, as the panel does.
 */
data class Attachment(val name: String, val bytes: Long, val image: String? = null, val thumb: String? = null, val text: String? = null) {
    fun record(): JSONObject = JSONObject().put("name", name).put("bytes", bytes).put("state", "kept").apply {
        if (thumb != null) put("thumb", thumb)
        if (image != null) put("looked", 1)
    }

    companion object {
        private const val TEXT_LIMIT = 200_000

        /** Reads a picked file. Null, with [why] set, for one that is neither a picture nor text. */
        suspend fun read(ctx: Context, uri: Uri, why: (String) -> Unit): Attachment? = withContext(Dispatchers.IO) {
            val cr = ctx.contentResolver
            var name = "file"
            var size = 0L
            cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    name = c.getString(0) ?: name
                    size = c.getLong(1)
                }
            }
            val type = cr.getType(uri).orEmpty()
            if (type.startsWith("image/")) {
                val bitmap = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return@withContext null.also { why(name) }
                Attachment(name, size, jpeg(bitmap, 1536, 85), "data:image/jpeg;base64," + jpeg(bitmap, 96, 70))
            } else {
                val raw = cr.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
                val text = raw.copyOf(minOf(raw.size, TEXT_LIMIT)).toString(Charsets.UTF_8)
                if (text.contains('\u0000')) {
                    why(name)
                    null
                } else {
                    Attachment(name, size.takeIf { it > 0 } ?: raw.size.toLong(), text = text)
                }
            }
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
