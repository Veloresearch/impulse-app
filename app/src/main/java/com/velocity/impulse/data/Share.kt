package com.velocity.impulse.data

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/** Words and files another app shared, read and waiting for the composer of a new chat. */
data class Incoming(val text: String, val files: List<Attachment>)

/** What a share sheet handed over, before the files are read. */
data class Shared(val text: String, val uris: List<Uri>) {
    companion object {
        fun of(intent: Intent): Shared? {
            if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return null
            val body = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty().trim()
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().trim()
            val text = if (subject.isEmpty() || body.contains(subject)) body else "$subject\n$body".trim()
            val streams = if (intent.action == Intent.ACTION_SEND) {
                listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
            } else {
                IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
            }
            val clips = intent.clipData?.let { c -> (0 until c.itemCount).mapNotNull { c.getItemAt(it).uri } }.orEmpty()
            // Only content:// - a file:// path could point into Impulse's own private files.
            val uris = (streams + clips).distinct().filter { it.scheme == ContentResolver.SCHEME_CONTENT }
            return if (text.isEmpty() && uris.isEmpty()) null else Shared(text, uris)
        }
    }
}
