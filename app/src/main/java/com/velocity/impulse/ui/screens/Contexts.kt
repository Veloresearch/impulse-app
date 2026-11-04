package com.velocity.impulse.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Attachment
import com.velocity.impulse.data.Building
import com.velocity.impulse.data.Context
import com.velocity.impulse.data.Entry
import com.velocity.impulse.data.Hit
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Pill
import com.velocity.impulse.ui.components.Empty
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Field
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Group
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.OnAccent
import com.velocity.impulse.ui.components.Segmented
import com.velocity.impulse.ui.components.Sheet
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Squircle
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The one context surface: pick what the chat draws on, see the check travel, and the sheet leaves. */
@Composable
fun ColumnScope.ContextSheetBody(m: AppModel) {
    val shell = LocalShell.current
    val scope = rememberCoroutineScope()
    BasicText(tr("Context", "Kontekst"), style = Type.Title)
    Spacer(Modifier.height(Space.S1))
    BasicText(
        tr(
            "Large collections of your documents that a chat can draw on. Pick one and every answer can use it.",
            "Duże zbiory Twoich dokumentów, z których czat może korzystać. Wybierz jeden, a każda odpowiedź może z niego czerpać.",
        ),
        style = Type.Meta,
    )
    Spacer(Modifier.height(Space.S4))
    if (m.contexts.isEmpty()) {
        Empty(
            tr("No contexts yet", "Nie ma jeszcze kontekstów"),
            tr("Make one in Contexts, from files on this phone or a folder on the computer.", "Utwórz go w Kontekstach, z plików na tym telefonie albo z folderu na komputerze."),
        )
        return
    }
    val chosen = m.chat?.contextName ?: m.defaultContext
    val pick = { name: String? ->
        m.useContext(name)
        scope.launch {
            delay(220)
            shell.done()
        }
        Unit
    }
    LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        item(key = 0) { ContextCard(null, chosen == null) { pick(null) } }
        items(m.contexts, key = { it.name }) { c -> ContextCard(c, chosen == c.name) { pick(c.name) } }
    }
}

@Composable
private fun ContextCard(c: Context?, on: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val fill by animateColorAsState(if (on) Tone.Raise else Tone.Panel, label = "card")
    val mark by animateFloatAsState(if (on) 1f else 0f, Motion.press(), label = "mark")
    Row(
        Modifier.fillMaxWidth().pressable(onClick, Shapes.Card, fill, Tone.Press, depth = 1f, feel = Feel.Tick).padding(Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(c?.name ?: tr("No context", "Bez kontekstu"), style = Type.Body.copy(fontWeight = FontWeight.SemiBold))
            BasicText(
                if (c == null) tr("Answers use only the chat itself", "Odpowiedzi biorą tylko z samego czatu")
                else listOfNotNull(c.tokens?.let { tokens(it) }, tr("${c.documents} documents", "dokumenty: ${c.documents}")).joinToString(" · "),
                style = Type.Meta,
            )
        }
        Spacer(Modifier.width(Space.S3))
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(22.dp).background(Tone.Hover, CircleShape))
            Box(
                Modifier.size(26.dp).graphicsLayer { scaleX = mark; scaleY = mark; alpha = mark }.background(accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Glyph.Check, OnAccent, size = 14.dp)
            }
        }
    }
}

/** A size a person chooses between, always in tokens; the name is only a name for it. */
private class Room(val tokens: Long, val label: String, val name: String, val note: String)

private fun rooms() = listOf(
    Room(1_000_000, "1M", tr("Small", "Mały"), tr("A few documents, or one small project", "Kilka dokumentów albo jeden mały projekt")),
    Room(10_000_000, "10M", tr("Large", "Duży"), tr("A large folder, or years of notes", "Duży folder albo lata notatek")),
    Room(MostRoom, "32M", tr("The most", "Największy"), tr("The most a context can hold", "Najwięcej, ile kontekst pomieści")),
)

private const val MostRoom = 32_000_000L

/** Contexts written before they had a size are read at the largest one, as the service does. */
private fun roomOf(c: Context) = c.capacity?.takeIf { it > 0 } ?: MostRoom

private val NameRule = Regex("^[A-Za-z0-9][A-Za-z0-9 _-]{0,63}$")

/** Files from the phone on their way into a context; one batch at a time. */
@Stable
private class Filling {
    var into by mutableStateOf<String?>(null)
    var done by mutableIntStateOf(0)
    var total by mutableIntStateOf(0)
    var file by mutableStateOf("")
    var refused by mutableStateOf<List<String>>(emptyList())
    var added by mutableIntStateOf(0)
    var finished by mutableStateOf(false)

    /** Reads each file on the phone and writes its words into [name]; a file that cannot be read says why. */
    fun start(scope: CoroutineScope, m: AppModel, app: android.content.Context, name: String, uris: List<Uri>) {
        if (uris.isEmpty() || into != null) return
        into = name
        total = uris.size
        done = 0
        added = 0
        finished = false
        refused = emptyList()
        scope.launch {
            val why = mutableListOf<String>()
            for (uri in uris) {
                val a = Attachment.read(app, uri, vision = false) { why += it }
                val text = a?.text
                if (a != null && text != null) {
                    file = a.name
                    var failed: String? = null
                    m.ask({ writeContext(text, a.name, context = name) }, { failed = "${a.name}: $it" }) { added++ }.join()
                    failed?.let { why += it }
                }
                done++
            }
            refused = why
            finished = true
            into = null
            m.refreshAll()
        }
    }
}

@Composable
fun ContextsPage(m: AppModel) {
    val app = LocalContext.current
    val scope = rememberCoroutineScope()
    val filling = remember { Filling() }
    var building by remember { mutableStateOf<Building?>(null) }
    var picked by remember { mutableStateOf<String?>(null) }
    var sheet by remember { mutableStateOf(false) }
    var making by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        var was: Building? = null
        while (true) {
            m.ask({ building() }) { b ->
                if (was != null && b == null) m.refreshAll()
                was = b
                building = b
            }.join()
            delay(if (building != null) 1500 else 6000)
        }
    }

    PageFrame(
        tr("Contexts", "Konteksty"),
        overlay = {
            Sheet(sheet, { sheet = false }) {
                m.contexts.firstOrNull { it.name == picked }?.let { c ->
                    key(c.name) {
                        ContextSheet(m, c, filling, { uris -> filling.start(scope, m, app, c.name, uris) }) { sheet = false }
                    }
                }
            }
            Sheet(making, { making = false }) {
                NewContext(m, { name, uris -> filling.start(scope, m, app, name, uris) }) { making = false }
            }
        },
    ) {
        BasicText(
            tr(
                "Large collections of your documents that a chat can draw on. They live on the computer; the sizes are in the loaded model's tokens.",
                "Duże zbiory Twoich dokumentów, z których czat może korzystać. Są na komputerze; rozmiary w tokenach wczytanego modelu.",
            ),
            Modifier.rise(0),
            style = Type.Body.copy(color = Ink.Three),
        )
        Pill(tr("New context", "Nowy kontekst"), { making = true }, Modifier.fillMaxWidth().rise(1), ink = OnAccent, fill = LocalAccent.current, height = 50.dp)
        Reveal(building != null) { building?.let { BuildingCard(it) } }
        Reveal(filling.into != null || filling.finished) { FillingCard(filling) }
        if (m.contexts.isEmpty()) {
            Empty(
                tr("No contexts yet", "Nie ma jeszcze kontekstów"),
                tr("Start one empty, from files on this phone, or from a folder on the computer.", "Zacznij od pustego, z plików na tym telefonie albo z folderu na komputerze."),
                Modifier.rise(2),
            )
        } else {
            Group(tr("On the computer", "Na komputerze"), Modifier.rise(2)) {
                m.contexts.forEach { c ->
                    key(c.name) {
                        ContextRow(m, c) {
                            picked = c.name
                            sheet = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextRow(m: AppModel, c: Context, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Item(
        c.name,
        listOf(held(c), tr("${c.documents} documents", "dokumenty: ${c.documents}")).joinToString(" · "),
        onClick,
        leading = {
            Box(Modifier.size(40.dp).background(Tone.Hover, Squircle(40.dp * 0.22f)), contentAlignment = Alignment.Center) {
                Icon(Glyph.Contexts, Ink.Two, size = 20.dp)
            }
        },
    ) {
        if (m.defaultContext == c.name) Badge(tr("In use", "W użyciu"), Glyph.Check, accent)
        else Icon(Glyph.Right, Ink.Four, size = 16.dp)
    }
}

private fun held(c: Context) =
    c.tokens?.let { tr("${short(it)} of ${short(roomOf(c))} tokens", "${short(it)} z ${short(roomOf(c))} tokenów") }
        ?: tr("Not read yet · room for ${short(roomOf(c))} tokens", "Jeszcze nieprzeczytany · miejsce na ${short(roomOf(c))} tokenów")

@Composable
private fun BuildingCard(b: Building) {
    val accent = LocalAccent.current
    val f = if (b.total > 0) b.done.toFloat() / b.total else 0f
    Column(Modifier.fillMaxWidth().background(Tone.Panel, Shapes.Card).padding(Space.S4), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spinner(accent, 14.dp)
            Spacer(Modifier.width(Space.S2))
            BasicText(tr("Building ${b.name}", "Buduję ${b.name}"), style = Type.Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        BasicText(
            b.stage.ifBlank { if (b.phase == "tokens") tr("Counting it in the loaded model's tokens", "Liczę w tokenach wczytanego modelu") else tr("Reading the documents", "Czytam dokumenty") },
            style = Type.Meta,
        )
        Bar(f, accent)
        BasicText(
            listOfNotNull(
                "${(f * 100).toInt()}%",
                b.etaSeconds?.let { s -> if (s < 60) tr("under a minute left", "mniej niż minuta") else tr("about ${s / 60} min left", "ok. ${s / 60} min") },
            ).joinToString(" · "),
            style = Type.Meta,
        )
    }
}

@Composable
private fun FillingCard(f: Filling) {
    val accent = LocalAccent.current
    Column(Modifier.fillMaxWidth().background(Tone.Panel, Shapes.Card).padding(Space.S4), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
        val into = f.into
        if (into != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spinner(accent, 14.dp)
                Spacer(Modifier.width(Space.S2))
                BasicText(tr("Adding files to $into", "Dodaję pliki do $into"), style = Type.Body.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicText(tr("${f.done} of ${f.total}", "${f.done} z ${f.total}") + if (f.file.isNotEmpty()) " · ${f.file}" else "", style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Bar(if (f.total > 0) f.done.toFloat() / f.total else 0f, accent)
        } else {
            BasicText(
                if (f.added == 1) tr("1 file added", "Dodano 1 plik") else tr("${f.added} files added", "Dodano plików: ${f.added}"),
                style = Type.Body.copy(fontWeight = FontWeight.SemiBold),
            )
            f.refused.forEach { BasicText(it, style = Type.Meta.copy(color = Signal.Warn)) }
            Pill(tr("Done", "Gotowe"), { f.finished = false }, Modifier.fillMaxWidth(), fill = Tone.Hover)
        }
    }
}

@Composable
private fun ContextSheet(m: AppModel, c: Context, filling: Filling, onFiles: (List<Uri>) -> Unit, onClose: () -> Unit) {
    val accent = LocalAccent.current
    val room = roomOf(c)
    val bigger = rooms().filter { it.tokens > room }
    var entries by remember { mutableStateOf<List<Entry>>(emptyList()) }
    var documents by remember { mutableIntStateOf(c.documents) }
    var reading by remember { mutableStateOf(true) }
    var end by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<Hit>?>(null) }
    var finding by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var grow by remember { mutableStateOf(false) }
    var size by remember { mutableIntStateOf(0) }
    var growing by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { if (it.isNotEmpty()) onFiles(it) }

    fun page(from: Int) {
        reading = true
        m.ask({ contextEntries(c.name, from, 50) }, { problem = it; reading = false }) { (list, docs) ->
            entries = if (from == 0) list else entries + list
            documents = docs
            end = list.size < 50
            reading = false
        }
    }
    val adding = filling.into == c.name
    LaunchedEffect(c.name, adding) { if (!adding) page(0) }
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            hits = null
            finding = false
            return@LaunchedEffect
        }
        delay(400)
        finding = true
        m.ask({ contextFind(c.name, q) }, { problem = it; finding = false }) {
            if (q == query.trim()) {
                hits = it
                finding = false
            }
        }
    }
    val tall = LocalConfiguration.current.screenHeightDp.dp * 0.8f

    Column(Modifier.heightIn(max = tall).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.S4)) {
        Column(Modifier.padding(top = Space.S1), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            BasicText(c.name, style = Type.BrandTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            BasicText(listOf(held(c), tr("$documents documents", "dokumenty: $documents")).joinToString(" · "), style = Type.Meta)
            Bar((c.tokens ?: 0L).toFloat() / room, accent)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.S2)) {
            val inUse = m.defaultContext == c.name
            Pill(
                if (inUse) tr("Stop using", "Przestań używać") else tr("Use in chats", "Używaj w czatach"),
                { m.useContext(if (inUse) null else c.name) },
                Modifier.weight(1f),
                ink = if (inUse) Ink.One else OnAccent,
                fill = if (inUse) Tone.Hover else accent,
            )
            Pill(
                if (adding) tr("Adding ${filling.done}/${filling.total}", "Dodaję ${filling.done}/${filling.total}") else tr("Add files", "Dodaj pliki"),
                { pick.launch(arrayOf("*/*")) },
                Modifier.weight(1f),
                fill = Tone.Hover,
                enabled = filling.into == null,
            )
        }

        if (bigger.isNotEmpty()) {
            Group {
                Unfold(tr("More room", "Więcej miejsca"), grow, { grow = !grow }, detail = tr("Holds up to ${short(room)} tokens. Nothing inside moves.", "Mieści do ${short(room)} tokenów. Nic w środku się nie przesuwa.")) {
                    val at = size.coerceIn(0, bigger.lastIndex)
                    Chips(bigger.map { "${it.label} · ${it.name}" }, at, { size = it })
                    BasicText(bigger[at].note, style = Type.Meta)
                    Pill(
                        if (growing) tr("Giving it room", "Powiększam") else tr("Give it ${bigger[at].label} tokens", "Daj mu ${bigger[at].label} tokenów"),
                        {
                            growing = true
                            m.ask({ growContext(c.name, bigger[at].tokens) }, { problem = it; growing = false }) {
                                growing = false
                                grow = false
                                m.refreshAll()
                            }
                        },
                        Modifier.fillMaxWidth(),
                        ink = OnAccent,
                        fill = accent,
                        enabled = !growing,
                    )
                }
            }
        }

        Field(query, { query = it }, tr("Find in this context", "Szukaj w tym kontekście"))
        val found = hits
        when {
            finding && found == null -> Row(Modifier.padding(start = Space.S1), verticalAlignment = Alignment.CenterVertically) {
                Spinner(Ink.Three, 14.dp)
                Spacer(Modifier.width(Space.S2))
                BasicText(tr("Looking", "Szukam"), style = Type.Meta)
            }
            found != null -> if (found.isEmpty()) {
                BasicText(tr("Nothing matches “${query.trim()}”.", "Nic nie pasuje do „${query.trim()}”."), Modifier.padding(start = Space.S1), style = Type.Meta)
            } else {
                Group(if (found.size == 1) tr("1 place", "1 miejsce") else tr("${found.size} places", "Miejsca: ${found.size}")) {
                    found.forEachIndexed { i, h -> key(i) { Piece(sourceName(h.source, h.actor), h.at, null, marked(h.text, query.trim())) } }
                }
            }
            else -> {
                if (entries.isEmpty() && !reading) {
                    BasicText(tr("Nothing in it yet. Add files to fill it.", "Na razie pusty. Dodaj pliki, żeby go wypełnić."), Modifier.padding(start = Space.S1), style = Type.Meta)
                }
                if (entries.isNotEmpty()) {
                    Group(tr("What it holds, newest first", "Zawartość, od najnowszych")) {
                        entries.forEach { e -> key(e.position) { Piece(sourceName(e.source, e.actor), e.at, e.tokens, AnnotatedString(e.preview + if (e.truncated) "…" else "")) } }
                    }
                }
                if (!end && entries.isNotEmpty()) {
                    Pill(
                        if (reading) tr("Loading", "Wczytuję") else tr("Show more", "Pokaż więcej"),
                        { page(entries.size) },
                        Modifier.fillMaxWidth(),
                        fill = Tone.Hover,
                        enabled = !reading,
                    )
                }
            }
        }

        Reveal(!confirm) {
            Pill(
                if (removing) tr("Deleting", "Usuwam") else tr("Delete context", "Usuń kontekst"),
                { confirm = true },
                Modifier.fillMaxWidth(),
                ink = Signal.Err,
                fill = Tone.Hover,
                enabled = !removing,
            )
        }
        Reveal(confirm) {
            Column(Modifier.fillMaxWidth().background(Tone.Raise, Shapes.Tile).padding(Space.S4), verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                BasicText(tr("Delete ${c.name}?", "Usunąć ${c.name}?"), style = Type.Body.copy(fontWeight = FontWeight.SemiBold))
                BasicText(
                    tr("Everything in it is removed from the computer. The files it was made from stay where they are.", "Cała zawartość zniknie z komputera. Pliki, z których powstał, zostają na miejscu."),
                    style = Type.Meta,
                )
                Row(Modifier.padding(top = Space.S1), horizontalArrangement = Arrangement.spacedBy(Space.S2)) {
                    Pill(tr("Keep", "Zostaw"), { confirm = false }, Modifier.weight(1f), fill = Tone.Hover)
                    Pill(tr("Delete", "Usuń"), {
                        confirm = false
                        removing = true
                        m.ask({ removeContext(c.name) }, { problem = it; removing = false }) {
                            if (m.defaultContext == c.name && (m.chat?.contextName ?: c.name) == c.name) m.useContext(null)
                            m.refreshAll()
                            onClose()
                        }
                    }, Modifier.weight(1f), ink = Signal.Err, fill = Tone.Hover)
                }
            }
        }
        Problem(problem)
    }
}

/** One piece of a context: where it came from, when, and its words. */
@Composable
private fun Piece(source: String, at: Long, tokens: Long?, text: AnnotatedString) {
    Column(Modifier.fillMaxWidth().background(Tone.Raise).padding(horizontal = Space.S4, vertical = Space.S3), verticalArrangement = Arrangement.spacedBy(Space.S1)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(source, Modifier.weight(1f), style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(tokens?.let { tokens(it) }, whenLabel(at * 1000).ifEmpty { null }).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Spacer(Modifier.width(Space.S2))
                BasicText(meta, style = Type.Label)
            }
        }
        BasicText(text, style = Type.Meta.copy(color = Ink.Two), maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
}

/** A file's name from its path; a remembered conversation turn keeps its short label. */
private fun sourceName(source: String, actor: String) =
    source.substringAfterLast('/').substringAfterLast('\\').ifBlank { actor.ifBlank { tr("Untitled", "Bez nazwy") } }

/** [text] with every place [q] occurs drawn in the strong ink. */
private fun marked(text: String, q: String): AnnotatedString = buildAnnotatedString {
    append(text)
    if (q.isEmpty()) return@buildAnnotatedString
    var i = text.indexOf(q, ignoreCase = true)
    while (i >= 0) {
        addStyle(SpanStyle(color = Ink.One, fontWeight = FontWeight.SemiBold), i, i + q.length)
        i = text.indexOf(q, i + q.length, ignoreCase = true)
    }
}

/** Token counts the way the panel writes them: 850K, 1.2M, 32M. */
private fun short(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1e6).replace(".0M", "M")
    n >= 1_000 -> "%.0fK".format(n / 1e3)
    else -> "$n"
}

@Composable
private fun NewContext(m: AppModel, onFiles: (String, List<Uri>) -> Unit, onDone: () -> Unit) {
    val accent = LocalAccent.current
    val sizes = rooms()
    var kind by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var folder by remember { mutableStateOf("") }
    var room by remember { mutableIntStateOf(sizes.lastIndex) }
    var files by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { if (it.isNotEmpty()) files = it }
    val clean = name.trim()
    val nameProblem = when {
        clean.isEmpty() -> null
        !NameRule.matches(clean) -> tr("Use letters, numbers, spaces and dashes.", "Użyj liter, cyfr, spacji i myślników.")
        clean.startsWith("chat-", ignoreCase = true) || m.contexts.any { it.name.equals(clean, ignoreCase = true) } -> tr("That name is taken.", "Ta nazwa jest zajęta.")
        else -> null
    }
    val ready = clean.isNotEmpty() && nameProblem == null && !busy && when (kind) {
        1 -> files.isNotEmpty()
        2 -> folder.isNotBlank()
        else -> true
    }
    val tall = LocalConfiguration.current.screenHeightDp.dp * 0.8f

    Column(Modifier.heightIn(max = tall).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.S4)) {
        BasicText(tr("New context", "Nowy kontekst"), Modifier.padding(top = Space.S1), style = Type.BrandTitle)
        Segmented(
            listOf(tr("Empty", "Pusty"), tr("Phone files", "Pliki z telefonu"), tr("Computer folder", "Folder na PC")),
            kind,
            { kind = it },
        )
        Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            Field(name, { name = it }, tr("Name, e.g. Thesis notes", "Nazwa, np. Notatki do pracy"))
            Reveal(nameProblem != null) { BasicText(nameProblem.orEmpty(), Modifier.padding(start = Space.S1), style = Type.Meta.copy(color = Signal.Warn)) }
        }
        when (kind) {
            0 -> BasicText(tr("It starts with nothing in it; add files to it any time.", "Zaczyna pusty; pliki dodasz w każdej chwili."), Modifier.padding(start = Space.S1), style = Type.Meta)
            1 -> Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                Pill(
                    when (files.size) {
                        0 -> tr("Choose files", "Wybierz pliki")
                        1 -> tr("1 file chosen", "Wybrano 1 plik")
                        else -> tr("${files.size} files chosen", "Wybrano plików: ${files.size}")
                    },
                    { pick.launch(arrayOf("*/*")) },
                    Modifier.fillMaxWidth(),
                    fill = Tone.Hover,
                )
                BasicText(
                    tr("PDF, Word, PowerPoint, Excel, OpenDocument, RTF, HTML and text files are read on this phone and their words sent to the computer.", "PDF, Word, PowerPoint, Excel, OpenDocument, RTF, HTML i pliki tekstowe są czytane na telefonie, a ich treść trafia na komputer."),
                    Modifier.padding(start = Space.S1),
                    style = Type.Meta,
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
                Field(folder, { folder = it }, tr("Folder on the computer, e.g. D:\\Notes", "Folder na komputerze, np. D:\\Notatki"), style = Type.Mono)
                BasicText(tr("The computer reads every document inside it. This can take a while; progress shows on this page.", "Komputer przeczyta każdy dokument w środku. To może chwilę potrwać; postęp widać na tej stronie."), Modifier.padding(start = Space.S1), style = Type.Meta)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.S2)) {
            BasicText(tr("Room", "Miejsce"), Modifier.padding(start = Space.S1), style = Type.Label)
            Chips(sizes.map { "${it.label} · ${it.name}" }, room, { room = it })
            BasicText(tr("${sizes[room].note}. In tokens of the model that reads it.", "${sizes[room].note}. W tokenach modelu, który go czyta."), Modifier.padding(start = Space.S1), style = Type.Meta)
        }
        Pill(
            if (busy) tr("Creating", "Tworzę") else tr("Create", "Utwórz"),
            {
                busy = true
                problem = null
                val n = clean
                val cap = sizes[room].tokens
                val source = folder.trim().takeIf { kind == 2 }
                val picked = files.takeIf { kind == 1 }.orEmpty()
                m.ask({ createContext(n, cap, source) }, { problem = it; busy = false }) {
                    busy = false
                    if (picked.isNotEmpty()) onFiles(n, picked)
                    m.refreshAll()
                    onDone()
                }
            },
            Modifier.fillMaxWidth(),
            ink = OnAccent,
            fill = accent,
            enabled = ready,
            height = 50.dp,
        )
        Problem(problem)
    }
}
