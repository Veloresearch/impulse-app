package com.velocity.impulse.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.velocity.impulse.BuildConfig
import com.velocity.impulse.ui.Lang.tr
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** A newer build on GitHub, with the one APK this phone should take. */
data class Release(val version: String, val tag: String, val file: String, val bytes: Long, val sha256: String, val notes: String)

/** Updates from the GitHub releases of the app's repository: checked, downloaded, verified and handed to the system installer. */
object Updater {
    enum class Step { Idle, Checking, Current, Found, Downloading, Ready, Failed }

    private const val Repo = "Veloresearch/impulse-app"
    private const val Day = 86_400_000L
    private const val Checked = "update_checked"

    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    var step by mutableStateOf(Step.Idle)
        private set
    var release by mutableStateOf<Release?>(null)
        private set
    var problem by mutableStateOf<String?>(null)
        private set

    /** True while the phone still has to let Impulse install apps. */
    var asking by mutableStateOf(false)
        private set

    /** 0..1 while downloading; read it while drawing so only the ring redraws. */
    var progress by mutableFloatStateOf(0f)
        private set

    /** Checks on start, at most once a day, without a word if it fails. */
    fun daily(ctx: Context) {
        val last = ctx.getSharedPreferences("impulse", Context.MODE_PRIVATE).getLong(Checked, 0L)
        if (System.currentTimeMillis() - last >= Day) check(ctx, quiet = true)
    }

    fun check(ctx: Context, quiet: Boolean = false) {
        if (job?.isActive == true) return
        val app = ctx.applicationContext
        step = Step.Checking
        problem = null
        job = scope.launch {
            try {
                val found = withContext(Dispatchers.IO) { latest() }
                app.getSharedPreferences("impulse", Context.MODE_PRIVATE).edit().putLong(Checked, System.currentTimeMillis()).apply()
                release = found
                step = when {
                    found == null -> Step.Current.also { withContext(Dispatchers.IO) { updates(app).deleteRecursively() } }
                    apk(app, found).let { it.isFile && it.length() == found.bytes } -> Step.Ready
                    else -> Step.Found
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (quiet) step = Step.Idle else fail(tr("Couldn't reach GitHub. Check the connection and try again.", "Brak połączenia z GitHubem. Sprawdź internet i spróbuj ponownie."))
            }
        }
    }

    fun download(ctx: Context) {
        val r = release ?: return
        if (job?.isActive == true) return
        val app = ctx.applicationContext
        step = Step.Downloading
        progress = 0f
        problem = null
        job = scope.launch {
            try {
                withContext(Dispatchers.IO) { fetch(app, r) }
                step = Step.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Mismatch) {
                fail(tr("The download didn't match its checksum, so it was thrown away. Try again.", "Pobrany plik nie zgadza się z sumą kontrolną, więc został usunięty. Spróbuj ponownie."))
            } catch (e: Exception) {
                fail(tr("The download stopped. Check the connection and try again.", "Pobieranie przerwane. Sprawdź internet i spróbuj ponownie."))
            }
        }
    }

    /** Hands the verified APK to the system installer, first sending the user to allow installs from Impulse if needed. */
    fun install(ctx: Context) {
        val r = release ?: return
        val file = apk(ctx, r)
        if (!file.isFile) {
            step = Step.Found
            return
        }
        try {
            if (!ctx.packageManager.canRequestPackageInstalls()) {
                asking = true
                ctx.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                return
            }
            asking = false
            val uri = FileProvider.getUriForFile(ctx, "com.velocity.impulse.files", file)
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: ActivityNotFoundException) {
            fail(tr("This phone has no installer for apps from outside the store.", "Ten telefon nie ma instalatora aplikacji spoza sklepu."))
        }
    }

    /** Back from the install permission screen: carry on if it was granted. */
    fun resume(ctx: Context) {
        if (asking && ctx.packageManager.canRequestPackageInstalls()) install(ctx)
    }

    private fun fail(message: String) {
        problem = message
        step = Step.Failed
    }

    private fun latest(): Release? {
        val manifest = JSONObject(get("https://github.com/$Repo/releases/latest/download/latest.json") ?: return null)
        val version = manifest.getString("version")
        if (compareVersions(version, BuildConfig.VERSION_NAME) <= 0) return null
        val apks = manifest.getJSONObject("apks")
        val apk = apks.optJSONObject(Build.SUPPORTED_ABIS.firstOrNull().orEmpty()) ?: apks.optJSONObject("universal") ?: return null
        val notes = runCatching { get("https://api.github.com/repos/$Repo/releases/latest")?.let { JSONObject(it).optString("body") } }.getOrNull()
        return Release(
            version,
            manifest.optString("tag").ifBlank { "v$version" },
            apk.getString("file"),
            apk.getLong("bytes"),
            apk.getString("sha256").lowercase(),
            notes.orEmpty().trim(),
        )
    }

    /** The body at [url], or null for a 404: nothing released yet, or the repository is private. */
    private fun get(url: String): String? = http.newCall(Request.Builder().url(url).build()).execute().use { r ->
        when {
            r.code == 404 -> null
            !r.isSuccessful -> throw IOException("HTTP ${r.code}")
            else -> r.body?.string().orEmpty()
        }
    }

    private fun updates(ctx: Context) = File(ctx.cacheDir, "updates")

    private fun apk(ctx: Context, r: Release) = File(updates(ctx), r.file)

    private class Mismatch : IOException()

    private suspend fun fetch(ctx: Context, r: Release) {
        val dir = updates(ctx)
        dir.deleteRecursively()
        dir.mkdirs()
        val part = File(dir, r.file + ".part")
        val sha = MessageDigest.getInstance("SHA-256")
        val url = "https://github.com/$Repo/releases/download/${r.tag}/${r.file}"
        try {
            download(url, r, part, sha)
        } catch (e: Throwable) {
            part.delete()
            throw e
        }
        val sum = sha.digest().joinToString("") { "%02x".format(it) }
        if (sum != r.sha256 || (r.bytes > 0 && part.length() != r.bytes)) {
            part.delete()
            throw Mismatch()
        }
        if (!part.renameTo(apk(ctx, r))) throw IOException("rename")
    }

    private suspend fun download(url: String, r: Release, part: File, sha: MessageDigest) {
        http.newCall(Request.Builder().url(url).build()).execute().use { res ->
            if (!res.isSuccessful) throw IOException("HTTP ${res.code}")
            val body = res.body ?: throw IOException("empty")
            val total = (if (r.bytes > 0) r.bytes else body.contentLength()).toFloat()
            body.byteStream().use { input ->
                part.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        sha.update(buf, 0, n)
                        done += n
                        if (total > 0) progress = (done / total).coerceIn(0f, 1f)
                    }
                }
            }
        }
    }
}

/** Semantic order of two versions; a leading v and build metadata are ignored, and 1.2.0-beta.2 comes before 1.2.0. */
fun compareVersions(a: String, b: String): Int {
    fun parts(v: String): Pair<List<Int>, List<String>> {
        val s = v.trim().removePrefix("v").substringBefore('+')
        val pre = s.substringAfter('-', "")
        return s.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 } to (if (pre.isEmpty()) emptyList() else pre.split('.'))
    }
    val (na, pa) = parts(a)
    val (nb, pb) = parts(b)
    for (i in 0 until maxOf(na.size, nb.size)) {
        val d = na.getOrElse(i) { 0 }.compareTo(nb.getOrElse(i) { 0 })
        if (d != 0) return d
    }
    if (pa.isEmpty() || pb.isEmpty()) return pb.size.compareTo(pa.size).coerceIn(-1, 1)
    for (i in 0 until minOf(pa.size, pb.size)) {
        val x = pa[i].toIntOrNull()
        val y = pb[i].toIntOrNull()
        val d = when {
            x != null && y != null -> x.compareTo(y)
            x != null -> -1
            y != null -> 1
            else -> pa[i].compareTo(pb[i])
        }
        if (d != 0) return d
    }
    return pa.size.compareTo(pb.size)
}
