package com.velocity.impulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Download
import com.velocity.impulse.data.HubFile
import com.velocity.impulse.data.HubModel
import com.velocity.impulse.data.HubRepo
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.Pill
import com.velocity.impulse.ui.components.Empty
import com.velocity.impulse.ui.components.Field
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Group
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.ModelFace
import com.velocity.impulse.ui.components.Roll
import com.velocity.impulse.ui.components.Sheet
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.delay

private val Sorts = listOf("downloads", "likes", "trending", "updated")

/** Search, results and the computer's downloads; kept while the Models page is open. */
@Stable
internal class HubState {
    var query by mutableStateOf("")
    var sort by mutableIntStateOf(0)
    var results by mutableStateOf<List<HubRepo>>(emptyList())
    var next by mutableStateOf<String?>(null)
    var searching by mutableStateOf(false)
    var searched by mutableStateOf(false)
    var problem by mutableStateOf<String?>(null)
    var downloads by mutableStateOf<List<Download>>(emptyList())
    var repo by mutableStateOf<HubRepo?>(null)
    var open by mutableStateOf(false)
    var cancel by mutableStateOf<Download?>(null)
    var asked = ""
        private set
    private var turn = 0

    /** Only the newest search lands; an older answer arriving late is dropped. */
    fun search(m: AppModel, more: Boolean = false) {
        val n = ++turn
        val q = query.trim()
        val by = Sorts[sort]
        val from = if (more) next.orEmpty() else ""
        asked = q
        searching = true
        problem = null
        m.ask({ hubSearch(q, by, cursor = from) }, {
            if (n == turn) {
                problem = it
                searching = false
            }
        }) { (list, cursor) ->
            if (n != turn) return@ask
            results = if (more) (results + list).distinctBy { it.id } else list
            next = cursor
            searching = false
            searched = true
        }
    }

    fun control(m: AppModel, d: Download, action: String) {
        m.ask({ controlDownload(d.name, action) }, { problem = it }) { downloads = it }
    }
}

/**
 * Follows the computer's downloads while the page is open: quickly while one moves, slowly
 * otherwise. A model that lands refreshes the library.
 */
@Composable
internal fun WatchDownloads(m: AppModel, hub: HubState) {
    LaunchedEffect(hub) {
        val seen = HashMap<String, String>()
        while (true) {
            m.ask({ downloads() }) { list ->
                val landed = seen.any { (name, was) -> was in Moving && list.none { it.name == name && it.state in Moving } }
                if (landed) m.refreshAll()
                seen.clear()
                list.forEach { seen[it.name] = it.state }
                hub.downloads = list
            }.join()
            delay(if (hub.downloads.any { it.state == "running" || it.state == "queued" || it.state == "verifying" }) 1200 else 5000)
        }
    }
}

@Composable
internal fun HubBody(m: AppModel, hub: HubState) {
    LaunchedEffect(Unit) { if (!hub.searched) hub.search(m) }
    LaunchedEffect(hub.query) {
        if (hub.query.trim() == hub.asked) return@LaunchedEffect
        delay(500)
        hub.search(m)
    }
    Transfers(m, hub)
    Field(hub.query, { hub.query = it }, tr("Search Hugging Face", "Szukaj na Hugging Face"), action = ImeAction.Search, onAction = { hub.search(m) })
    Chips(
        listOf(tr("Popular", "Popularne"), tr("Most liked", "Najbardziej lubiane"), tr("Trending", "Na czasie"), tr("Newest", "Najnowsze")),
        hub.sort,
        {
            if (it != hub.sort) {
                hub.sort = it
                hub.search(m)
            }
        },
    )
    val results = hub.results
    when {
        results.isEmpty() && hub.searching -> Row(Modifier.padding(start = Space.S1), verticalAlignment = Alignment.CenterVertically) {
            Spinner(Ink.Three, 14.dp)
            Spacer(Modifier.width(Space.S2))
            BasicText(tr("Searching Hugging Face", "Szukam na Hugging Face"), style = Type.Meta)
        }
        results.isEmpty() && hub.searched -> Empty(
            tr("Nothing found", "Nic nie znaleziono"),
            tr("Try the model's family name, like Qwen, Gemma or Llama.", "Spróbuj nazwy rodziny modelu, np. Qwen, Gemma albo Llama."),
        )
    }
    if (results.isNotEmpty()) {
        Group(tr("Models that run in Impulse", "Modele, które działają w Impulse")) {
            results.forEach { r -> key(r.id) { RepoRow(m, r) { hub.repo = r; hub.open = true } } }
        }
    }
    if (hub.next != null && results.isNotEmpty()) {
        Pill(
            if (hub.searching) tr("Loading more", "Wczytuję więcej") else tr("Show more", "Pokaż więcej"),
            { hub.search(m, more = true) },
            Modifier.fillMaxWidth(),
            fill = Tone.Hover,
            enabled = !hub.searching,
        )
    }
    Problem(hub.problem)
}

/** Its own scope, so the progress polled every second redraws the transfers and not the results. */
@Composable
private fun Transfers(m: AppModel, hub: HubState) {
    val live = hub.downloads.filter { it.state != "done" }
    if (live.isEmpty()) return
    Group(tr("Downloading to the computer", "Pobieranie na komputer")) {
        live.forEach { d -> key(d.name) { DownloadRow(m, hub, d, named = true, ground = Tone.Panel) } }
    }
}

@Composable
private fun RepoRow(m: AppModel, r: HubRepo, onClick: () -> Unit) {
    Item(
        r.name,
        listOf(r.author, tr("${count(r.downloads)} downloads", "pobrania: ${count(r.downloads)}"), tr("${r.builds} builds", "wersje: ${r.builds}")).joinToString(" · "),
        onClick,
        lines = 1,
        leading = { ModelFace(m, r.name, "", 40.dp) },
    ) {
        if (r.gated) Badge(tr("Access", "Dostęp"), Glyph.Lock) else Icon(Glyph.Right, Ink.Four, size = 16.dp)
    }
}

/** The repository sheet and the question before a download is thrown away. */
@Composable
internal fun HubSheets(m: AppModel, hub: HubState) {
    Sheet(hub.open, { hub.open = false }) {
        hub.repo?.let { r -> key(r.id) { RepoSheet(m, hub, r) } }
    }
    var last by remember { mutableStateOf<Download?>(null) }
    hub.cancel?.let { last = it }
    Ask(
        hub.cancel != null, { hub.cancel = null },
        tr("Cancel this download?", "Anulować to pobieranie?"),
        tr(
            "${last?.name.orEmpty()} stops and what has arrived so far (${gb(last?.have ?: 0)}) is deleted from the computer.",
            "${last?.name.orEmpty()} zostanie zatrzymany, a to, co już dotarło (${gb(last?.have ?: 0)}), zniknie z komputera.",
        ),
        tr("Cancel download", "Anuluj pobieranie"),
    ) { last?.let { hub.control(m, it, "cancel") } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RepoSheet(m: AppModel, hub: HubState, r: HubRepo) {
    var model by remember { mutableStateOf<HubModel?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    var starting by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(r.id) { m.ask({ hubModel(r.id) }, { problem = it }) { model = it } }
    val tall = LocalConfiguration.current.screenHeightDp.dp * 0.8f

    Column(Modifier.heightIn(max = tall).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.S4)) {
        Row(Modifier.padding(top = Space.S1), verticalAlignment = Alignment.CenterVertically) {
            ModelFace(m, r.name, "", 52.dp)
            Spacer(Modifier.width(Space.S3))
            Column(Modifier.weight(1f)) {
                BasicText(r.name, style = Type.BrandSection, maxLines = 2, overflow = TextOverflow.Ellipsis)
                BasicText(tr("by ${r.author} on Hugging Face", "od ${r.author} na Hugging Face"), style = Type.Meta, maxLines = 1)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Badge(tr("${count(r.downloads)} downloads", "pobrania: ${count(r.downloads)}"))
            Badge(tr("${count(r.likes)} likes", "polubienia: ${count(r.likes)}"))
            r.task?.let { Badge(it) }
        }
        if (r.gated) {
            Note(
                tr("Access first", "Najpierw dostęp"),
                tr("Its author asks people to request access on Hugging Face. A download fails until the computer's Hugging Face account has it.", "Autor prosi o dostęp na Hugging Face. Pobieranie się nie uda, dopóki konto Hugging Face na komputerze go nie ma."),
                Signal.Warn,
            )
        }
        val mdl = model
        if (mdl == null) {
            if (problem == null) Row(verticalAlignment = Alignment.CenterVertically) {
                Spinner(Ink.Three, 14.dp)
                Spacer(Modifier.width(Space.S2))
                BasicText(tr("Reading its builds and what each needs", "Czytam wersje i ich wymagania"), style = Type.Meta)
            }
        } else {
            BasicText(judgedFor(mdl), Modifier.padding(horizontal = Space.S1), style = Type.Meta)
            val files = remember(mdl) { mdl.files.filter { it.loadable }.sortedWith(compareByDescending<HubFile> { it.recommended }.thenBy { it.bytes }) }
            if (files.isEmpty()) {
                Empty(tr("No builds to run", "Brak wersji do uruchomienia"), tr("This repository has no model files Impulse can load.", "To repozytorium nie ma plików modelu, które Impulse wczyta."))
            } else {
                Group(tr("Builds", "Wersje")) {
                    files.forEach { f ->
                        key(f.path) {
                            FileRow(m, hub, r, mdl, f, f.path in starting) {
                                starting = starting + f.path
                                m.ask({ hubDownload(r.id, f.path, f.bytes) }, { problem = it; starting = starting - f.path }) {
                                    m.ask({ downloads() }) { list ->
                                        hub.downloads = list
                                        starting = starting - f.path
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Problem(problem)
    }
}

@Composable
private fun FileRow(m: AppModel, hub: HubState, r: HubRepo, h: HubModel, f: HubFile, starting: Boolean, onDownload: () -> Unit) {
    val accent = LocalAccent.current
    val file = f.path.substringAfterLast('/')
    val d = hub.downloads.firstOrNull { it.repo == r.id && (it.name == f.path || it.name == file) }
    val here = d?.state == "done" || m.library.any { it.path.replace('\\', '/').endsWith("/$file", ignoreCase = true) }
    val (word, tone) = verdict(f, h)
    val need = if (h.gpu && h.vramTotalMb > 0) f.vramNeeded else f.ramNeeded
    Column(Modifier.fillMaxWidth().background(Tone.Raise).padding(horizontal = Space.S4, vertical = Space.S3), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicText(f.quant.ifBlank { file }, style = Type.Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (f.recommended) {
                        Spacer(Modifier.width(Space.S2))
                        Badge(tr("Best pick", "Najlepszy wybór"), Glyph.Sparkle, accent)
                    }
                }
                BasicText(
                    tr("${gb(f.bytes)} download · needs about ${gb(need)}", "${gb(f.bytes)} do pobrania · potrzebuje ok. ${gb(need)}"),
                    style = Type.Meta, maxLines = 1,
                )
            }
            Spacer(Modifier.width(Space.S2))
            Badge(word, color = tone)
        }
        when {
            d != null && (d.state in Moving || d.state == "failed") -> DownloadRow(m, hub, d, named = false, ground = Color.Transparent, inset = false)
            here -> BasicText(tr("On the computer", "Na komputerze"), style = Type.Meta.copy(color = accent, fontWeight = FontWeight.SemiBold))
            else -> Pill(
                if (starting) tr("Starting", "Uruchamiam") else tr("Download", "Pobierz"),
                onDownload,
                Modifier.fillMaxWidth(),
                fill = Tone.Hover,
                enabled = !starting,
            )
        }
    }
}

/** One transfer: how far, how fast, and what can be done with it. */
@Composable
private fun DownloadRow(m: AppModel, hub: HubState, d: Download, named: Boolean, ground: Color, inset: Boolean = true) {
    val accent = LocalAccent.current
    val f = if (d.total > 0) d.have.toFloat() / d.total else (d.percent / 100).toFloat()
    val pad = if (inset) Modifier.padding(horizontal = Space.S4, vertical = Space.S3) else Modifier
    Column(Modifier.fillMaxWidth().background(ground).then(pad), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        if (named) BasicText(d.name.substringAfterLast('/'), style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(status(d), Modifier.weight(1f), style = Type.Meta.copy(color = if (d.state == "failed") Signal.Err else Ink.Three), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(Space.S2))
            Roll("${(f * 100).toInt().coerceIn(0, 100)}%", Type.Meta.copy(color = Ink.Two), digits = true)
        }
        Bar(f, when (d.state) {
            "failed" -> Signal.Err
            "paused" -> Ink.Three
            else -> accent
        })
        Row(horizontalArrangement = Arrangement.spacedBy(Space.S2)) {
            when (d.state) {
                "paused" -> Pill(tr("Resume", "Wznów"), { hub.control(m, d, "resume") }, Modifier.weight(1f), fill = Tone.Hover)
                "failed" -> Pill(tr("Try again", "Spróbuj ponownie"), { hub.control(m, d, "retry") }, Modifier.weight(1f), fill = Tone.Hover)
                else -> Pill(tr("Pause", "Wstrzymaj"), { hub.control(m, d, "pause") }, Modifier.weight(1f), fill = Tone.Hover, enabled = d.state != "verifying")
            }
            Pill(tr("Cancel", "Anuluj"), { hub.cancel = d }, Modifier.weight(1f), ink = Signal.Err, fill = Tone.Hover)
        }
    }
}

private fun status(d: Download): String = when (d.state) {
    "running" -> if (d.bytesPerSecond > 0) {
        "${gb(d.bytesPerSecond)}/s" + (if (d.etaSeconds > 0) " · " + tr("${eta(d.etaSeconds)} left", "zostało ${eta(d.etaSeconds)}") else "")
    } else tr("Starting", "Rozpoczynam")
    "queued" -> tr("Waiting for the one before it", "Czeka na poprzednie")
    "verifying" -> tr("Checking the file", "Sprawdzam plik")
    "paused" -> tr("Paused · ${gb(d.have)} of ${gb(d.total)}", "Wstrzymane · ${gb(d.have)} z ${gb(d.total)}")
    "failed" -> d.error.ifBlank { tr("It stopped before the end", "Zatrzymało się przed końcem") }
    else -> tr("On the computer", "Na komputerze")
}

private fun eta(s: Long): String = when {
    s < 60 -> tr("under a minute", "mniej niż minuta")
    s < 3600 -> tr("${s / 60} min", "${s / 60} min")
    else -> tr("${s / 3600} h ${s % 3600 / 60} min", "${s / 3600} h ${s % 3600 / 60} min")
}

/**
 * The fit in words. With a graphics card the service's verdict is the answer; without one it
 * judges a card that is not there, so the question becomes whether it fits in memory.
 */
private fun verdict(f: HubFile, h: HubModel): Pair<String, Color> {
    if (h.gpu && h.vramTotalMb > 0) {
        val v = f.verdict.lowercase()
        return when {
            v.startsWith("best") -> tr("Best fit", "Najlepiej pasuje") to Signal.Ok
            v.startsWith("good") -> tr("Runs well", "Działa dobrze") to Signal.Ok
            v.startsWith("tight") -> tr("Only just fits", "Ledwo się mieści") to Signal.Warn
            v.startsWith("partial") -> tr("Partly on the card", "Częściowo na karcie") to Signal.Warn
            v.startsWith("not") -> tr("Too big", "Za duży") to Signal.Err
            else -> f.verdict to Ink.Two
        }
    }
    val ram = h.ramTotalMb * 1_048_576L
    return when {
        ram <= 0 -> f.verdict.ifBlank { tr("Unknown", "Nieznane") } to Ink.Two
        f.ramNeeded > ram -> tr("Too big", "Za duży") to Signal.Err
        f.ramNeeded > ram * 0.8 -> tr("Only just fits", "Ledwo się mieści") to Signal.Warn
        else -> tr("Runs on the processor", "Działa na procesorze") to Ink.Two
    }
}

private fun judgedFor(h: HubModel): String {
    val window = "${h.ctx / 1024}K"
    return if (h.gpu && h.vramTotalMb > 0) {
        tr("Judged for this computer: ${mbGb(h.vramTotalMb)} graphics memory and ${mbGb(h.ramTotalMb)} memory, at a $window window.", "Ocenione dla tego komputera: ${mbGb(h.vramTotalMb)} pamięci karty i ${mbGb(h.ramTotalMb)} pamięci, przy oknie $window.")
    } else {
        tr("Judged for this computer: no graphics card, ${mbGb(h.ramTotalMb)} memory, at a $window window.", "Ocenione dla tego komputera: bez karty graficznej, ${mbGb(h.ramTotalMb)} pamięci, przy oknie $window.")
    }
}

private fun count(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1e6).replace(".0M", "M")
    n >= 1_000 -> "%.1fK".format(n / 1e3).replace(".0K", "K")
    else -> "$n"
}
