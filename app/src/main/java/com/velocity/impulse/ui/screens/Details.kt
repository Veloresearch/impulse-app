package com.velocity.impulse.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velocity.impulse.R
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.data.Chat
import com.velocity.impulse.data.Persona
import com.velocity.impulse.data.Personas
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.LocalShell
import com.velocity.impulse.ui.Shell
import com.velocity.impulse.ui.Surface
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Feel
import com.velocity.impulse.ui.components.Glyph
import com.velocity.impulse.ui.components.Icon
import com.velocity.impulse.ui.components.ModelFace
import com.velocity.impulse.ui.components.Roll
import com.velocity.impulse.ui.components.Segmented
import com.velocity.impulse.ui.components.Toggle
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.components.feel
import com.velocity.impulse.ui.components.pressable
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

private val TitleStyle = Type.BrandTitle
private val StatStyle = Type.brand(26.sp)

/** Answer length presets, as max_tokens; null leaves it to the computer. */
private val Lengths = listOf<Int?>(null, 1024, 4096, 8192, 16384)

/** Rows slide in from the right edge one after another as the panel opens, reversing as it closes. */
private fun Modifier.arrive(shell: Shell, slot: Int) = graphicsLayer {
    if (shell.calm) return@graphicsLayer
    val k = (-shell.drawer.value).coerceIn(0f, 1f)
    val i = slot.coerceIn(0, 11)
    val p = ((k - 0.035f * i) / (1f - 0.035f * i)).coerceIn(0f, 1f)
    translationX = (1f - p) * 40.dp.toPx()
    alpha = p
    compositingStrategy = CompositingStrategy.ModulateAlpha
}

/** What the panel shows about a chat, read once per version of it rather than on every frame. */
private class Facts(val asked: Int, val receipt: JSONObject?, val files: List<JSONObject>)

private fun factsOf(c: Chat): Facts {
    val msgs = c.messages
    return Facts(
        msgs.count { it.role == "user" },
        msgs.lastOrNull { it.role == "assistant" && it.receipt != null }?.receipt,
        msgs.flatMap { msg -> msg.raw.optJSONArray("files")?.let { a -> (0 until a.length()).mapNotNull(a::optJSONObject) }.orEmpty() },
    )
}

/**
 * The right side of the room: everything about the chat in front of you. Its name, how the last
 * answer ran, what was sent in it, how it answers, and pinning, copying or deleting it.
 */
@Composable
fun Details(m: AppModel, modifier: Modifier = Modifier) {
    val shell = LocalShell.current
    val c = m.chat
    val facts = remember(c) { c?.let(::factsOf) }
    Column(
        modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(Space.S5),
    ) {
        Header(m, c, facts, shell, Modifier.arrive(shell, 0))
        if (c != null && facts != null) {
            facts.receipt?.let { r -> Section(shell, 1, tr("Last answer", "Ostatnia odpowiedź")) { Stats(r, m.status?.model?.context) } }
            if (facts.files.isNotEmpty()) Section(shell, 2, tr("In this chat", "W tym czacie")) { Gallery(facts.files) }
            Section(shell, 3, tr("Chat settings", "Ustawienia czatu")) { ChatSettings(m, c) }
            Section(shell, 4, tr("Personality", "Osobowość")) { Voices(c.persona) { m.setPersona(it) } }
        }
        Section(shell, 5, tr("Context", "Kontekst")) { ContextRow(m, shell) }
        Section(shell, 6, tr("Model", "Model")) { ModelRow(m, shell) }
        if (c != null) Section(shell, 7, null) { ChatActions(m, c, shell) }
    }
}

@Composable
private fun Header(m: AppModel, c: Chat?, facts: Facts?, shell: Shell, modifier: Modifier) {
    Column(modifier.padding(start = 8.dp, top = 8.dp)) {
        BasicText(tr("This chat", "Ten czat"), style = Type.Label)
        Spacer(Modifier.size(6.dp))
        Title(m, c, shell)
        if (c != null && facts != null) {
            Spacer(Modifier.size(4.dp))
            BasicText(
                tr("${facts.asked} questions · started ", "pytania: ${facts.asked} · od ") + whenLabel(c.raw.optLong("createdAt")),
                style = Type.Meta,
            )
        }
    }
}

/**
 * The title is its own rename field: editing grows a well around the words where they stand,
 * so nothing moves; Done or the check saves, back or leaving the panel lets it go.
 */
@Composable
private fun Title(m: AppModel, c: Chat?, shell: Shell) {
    val view = LocalView.current
    val accent = LocalAccent.current
    val shown = c?.title?.ifBlank { null } ?: tr("New chat", "Nowy czat")
    var editing by remember(c?.id) { mutableStateOf(false) }
    var draft by remember(c?.id) { mutableStateOf(TextFieldValue(shown)) }
    val focus = remember { FocusRequester() }
    val well = animateFloatAsState(if (editing) 1f else 0f, if (shell.calm) tween(Motion.M2) else Motion.calmText(), label = "well")

    fun save() {
        if (!editing) return
        editing = false
        val t = draft.text.trim()
        if (c != null && t.isNotEmpty() && t != c.title) {
            m.rename(c.id, t)
            view.feel(Feel.Confirm)
        }
    }
    LaunchedEffect(editing) {
        if (!editing) return@LaunchedEffect
        draft = TextFieldValue(shown, TextRange(0, shown.length))
        focus.requestFocus()
    }
    LaunchedEffect(Unit) { snapshotFlow { shell.drawer.targetValue }.collect { if (it > -1f) editing = false } }
    BackHandler(editing) { editing = false }

    Row(verticalAlignment = Alignment.Top) {
        BasicTextField(
            value = if (editing) draft else TextFieldValue(shown),
            onValueChange = { draft = it.copy(text = it.text.replace('\n', ' ')) },
            modifier = Modifier.weight(1f)
                .focusRequester(focus)
                .onFocusChanged { if (!it.isFocused) save() }
                .drawBehind {
                    val p = well.value
                    if (p <= 0f) return@drawBehind
                    val gx = 8.dp.toPx() * p
                    val gy = 4.dp.toPx() * p
                    drawRoundRect(
                        Tone.Inset.copy(alpha = p),
                        Offset(-gx, -gy),
                        Size(size.width + 2 * gx, size.height + 2 * gy),
                        CornerRadius(12.dp.toPx()),
                    )
                },
            enabled = editing,
            textStyle = TitleStyle,
            cursorBrush = SolidColor(accent),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
        )
        if (c != null) {
            Spacer(Modifier.width(Space.S2))
            AnimatedContent(
                editing,
                Modifier.size(40.dp),
                transitionSpec = {
                    (fadeIn(tween(Motion.M2)) + scaleIn(spring(0.6f, 600f), 0.5f)) togetherWith
                        (fadeOut(tween(Motion.M1)) + scaleOut(tween(Motion.M1), 0.5f))
                },
                contentAlignment = Alignment.Center,
                label = "rename",
            ) { on ->
                Box(
                    Modifier.size(40.dp).pressable(
                        { if (on) save() else editing = true },
                        CircleShape, if (on) accent.copy(alpha = 0.18f) else Tone.Panel, Tone.Raise,
                        depth = 0.88f, stretch = true,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (on) Icon(Glyph.Check, accent, size = 18.dp)
                    else Image(painterResource(R.drawable.ph_pencil_simple_bold), tr("Rename", "Zmień nazwę"), Modifier.size(17.dp), colorFilter = ColorFilter.tint(Ink.Two))
                }
            }
        }
    }
}

/** How the last answer ran, from its receipt: speed, wait, tokens in and out, and how full the window is. */
@Composable
private fun Stats(r: JSONObject, window: Int?) {
    val accent = LocalAccent.current
    val speed = r.optDouble("decode_tok_s", 0.0)
    val prompt = r.optLong("prompt_tokens").takeIf { it > 0 } ?: r.optLong("working_tokens")
    val made = r.optLong("generated_tokens")
    val first = r.optDouble("ttft_ms", 0.0).takeIf { it > 0 } ?: r.optDouble("prefill_tok_s", 0.0).takeIf { it > 0 && prompt > 0 }?.let {
        r.optDouble("select_ms", 0.0) + r.optDouble("assemble_ms", 0.0) + prompt / it * 1000
    }
    val used = (r.optLong("working_tokens").takeIf { it > 0 } ?: prompt) + made
    val share = if (window != null && window > 0) (used.toFloat() / window).coerceIn(0f, 1f) else null
    val fill = animateFloatAsState(share ?: 0f, Motion.calmText(), label = "used")

    Column(Modifier.fillMaxWidth().background(Tone.Panel, Shapes.Card).padding(Space.S4), verticalArrangement = Arrangement.spacedBy(Space.S4)) {
        Row {
            Stat(if (speed > 0) "%.1f".format(Locale.US, speed) else "–", tr("tokens per second", "tokenów na sekundę"), Modifier.weight(1f))
            Stat(first?.let { "%.2f s".format(Locale.US, it / 1000) } ?: "–", tr("to the first token", "do pierwszego tokenu"), Modifier.weight(1f))
        }
        Row {
            Stat(count(prompt), tr("tokens read", "tokenów wczytanych"), Modifier.weight(1f))
            Stat(count(made), tr("tokens written", "tokenów napisanych"), Modifier.weight(1f))
        }
        if (share != null) Column {
            Row(verticalAlignment = Alignment.Bottom) {
                BasicText(tr("Context used", "Użyty kontekst"), Modifier.weight(1f), style = Type.Meta)
                Roll("${(share * 100).roundToInt()}%", Type.Meta.copy(color = Ink.One, fontWeight = FontWeight.Medium), digits = true)
                BasicText(tr(" of ", " z ") + tokens(window!!.toLong()), style = Type.Meta)
            }
            Spacer(Modifier.size(Space.S2))
            Box(
                Modifier.fillMaxWidth().height(6.dp).drawBehind {
                    val r6 = CornerRadius(size.height / 2)
                    drawRoundRect(Tone.Inset, cornerRadius = r6)
                    val w = size.width * fill.value
                    if (w > 0f) drawRoundRect(if (fill.value > 0.9f) Signal.Warn else accent, size = Size(w.coerceAtLeast(size.height), size.height), cornerRadius = r6)
                },
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Roll(value, StatStyle, digits = true)
        BasicText(label, style = Type.Label)
    }
}

private fun count(n: Long) = if (n > 0) "%,d".format(Locale.US, n) else "–"

/** Every picture and file sent in the chat; a tap grows the preview out of the tile. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Gallery(files: List<JSONObject>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        files.forEach { f ->
            val url = remember(f) { f.optString("thumb").takeIf { it.startsWith("data:image/") } }
            val thumb = remember(f) { url?.let(::bitmap) }
            val item = remember(f) { PreviewItem(f.optString("name"), f.optLong("bytes"), url, kept = true) }
            val tile = Modifier.size(72.dp).hiddenWhile(item).opens(item)
            if (thumb != null) {
                Image(thumb, f.optString("name"), tile.clip(Shapes.Tile), contentScale = ContentScale.Crop)
            } else {
                Column(
                    tile.background(Tone.Panel, Shapes.Tile).padding(6.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Glyph.File, Ink.Three, size = 22.dp)
                    Spacer(Modifier.size(4.dp))
                    BasicText(
                        f.optString("name"),
                        style = Type.Label.copy(fontSize = 10.sp, textAlign = TextAlign.Center),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun Modifier.opens(item: PreviewItem): Modifier {
    val at = remember { arrayOfNulls<LayoutCoordinates>(1) }
    return onPlaced { at[0] = it }.pressable(
        { at[0]?.takeIf { it.isAttached }?.let { PreviewState.open(item, it.boundsInRoot()) } },
        depth = 0.94f,
    )
}

private fun Modifier.hiddenWhile(item: PreviewItem) = graphicsLayer { alpha = if (PreviewState.item === item) 0f else 1f }

@Composable
private fun ChatSettings(m: AppModel, c: Chat) {
    Column(Modifier.fillMaxWidth().clip(Shapes.Card), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Block { Instructions(m, c) }
        Block { Creativity(c.temperature) { m.setTemperature(it) } }
        Block {
            BasicText(tr("Longest answer", "Najdłuższa odpowiedź"), style = Type.Body.copy(fontWeight = FontWeight.Medium))
            Spacer(Modifier.size(Space.S2))
            val options = if (c.maxTokens in Lengths) Lengths else Lengths + c.maxTokens
            Segmented(
                options.map { n -> if (n == null) tr("Auto", "Auto") else if (n % 1024 == 0) "${n / 1024}K" else "$n" },
                options.indexOf(c.maxTokens).coerceAtLeast(0),
                { m.setMaxTokens(options[it]) },
            )
        }
        if (m.status?.model?.thinks == true) Block {
            Row(verticalAlignment = Alignment.Bottom) {
                BasicText(tr("Thinking", "Myślenie"), Modifier.weight(1f), style = Type.Body.copy(fontWeight = FontWeight.Medium))
                BasicText(tr("All chats", "Wszystkie czaty"), style = Type.Meta)
            }
            Spacer(Modifier.size(Space.S2))
            Segmented(effortLabels(), Efforts.indexOf(m.effort ?: "medium").coerceAtLeast(0), { m.pickEffort(Efforts[it]) })
        }
        Switch(
            Glyph.Globe, tr("Search the web", "Szukaj w sieci"),
            if (c.web == null) tr("As in Settings", "Jak w Ustawieniach") else tr("For this chat", "W tym czacie"),
            c.web ?: m.webDefault,
        ) { m.setChatWeb(it) }
        Switch(
            Glyph.Link, tr("Plugins", "Wtyczki"),
            when (val n = m.plugins.size) {
                0 -> tr("None on your computer", "Brak na komputerze")
                1 -> tr("1 tool on your computer", "1 narzędzie na komputerze")
                else -> tr("$n tools on your computer", "Narzędzia na komputerze: $n")
            },
            c.plugins ?: m.pluginsDefault,
        ) { m.setChatPlugins(it) }
    }
}

@Composable
private fun Block(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Tone.Panel).padding(Space.S4), content = content)
}

@Composable
private fun Switch(glyph: Glyph, title: String, detail: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).background(Tone.Panel).padding(horizontal = Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, Ink.Two, size = 18.dp)
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = Type.Body.copy(fontWeight = FontWeight.Medium))
            BasicText(detail, style = Type.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Toggle(on, onChange)
    }
}

/** The chat's own instructions; kept a moment after typing stops, and when the field is left. */
@Composable
private fun Instructions(m: AppModel, c: Chat) {
    val accent = LocalAccent.current
    var draft by remember(c.id) { mutableStateOf(c.systemPrompt.orEmpty()) }
    fun keep() {
        if (m.current == c.id && draft.trim() != m.chat?.systemPrompt.orEmpty()) m.setSystemPrompt(draft)
    }
    LaunchedEffect(c.id, draft) {
        delay(800)
        keep()
    }
    BasicText(tr("Instructions", "Instrukcje"), style = Type.Body.copy(fontWeight = FontWeight.Medium))
    BasicText(tr("The model follows these in every answer here.", "Model stosuje je w każdej odpowiedzi tutaj."), style = Type.Meta)
    Spacer(Modifier.size(Space.S2))
    BasicTextField(
        draft,
        { draft = it },
        Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) keep() }
            .background(Tone.Inset, Shapes.ControlLg).padding(horizontal = Space.S3, vertical = Space.S3),
        textStyle = Type.Body,
        cursorBrush = SolidColor(accent),
        minLines = 3,
        maxLines = 10,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { inner ->
            Box {
                if (draft.isEmpty()) BasicText(tr("For example: answer briefly, in plain words.", "Na przykład: odpowiadaj krótko i prosto."), style = Type.Body.copy(color = Ink.Four))
                inner()
            }
        },
    )
}

/**
 * Temperature from 0 to 2 in tenths. The thumb follows the finger and ticks at every tenth; the
 * value is kept when the finger lifts, so a drag is one change, not twenty.
 */
@Composable
private fun Creativity(value: Double?, onSet: (Double?) -> Unit) {
    val accent = LocalAccent.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val set = rememberUpdatedState(onSet)
    val shown = value ?: 0.7
    val pos = remember { Animatable((shown / 2).toFloat()) }
    var held by remember { mutableStateOf(false) }
    var live by remember { mutableStateOf(shown) }
    val swell = animateFloatAsState(if (held) 1f else 0f, spring(0.6f, 600f), label = "swell")
    LaunchedEffect(shown) {
        if (held) return@LaunchedEffect
        live = shown
        pos.animateTo((shown / 2).toFloat(), Motion.calmText())
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(tr("Creativity", "Kreatywność"), Modifier.weight(1f), style = Type.Body.copy(fontWeight = FontWeight.Medium))
        val reset by animateColorAsState(if (value != null) Ink.Three else Color.Transparent, tween(Motion.M2), label = "reset")
        BasicText(
            tr("Reset", "Domyślnie"),
            Modifier.padding(end = Space.S2)
                .pressable({ if (value != null) set.value(null) }, Shapes.ControlSm, enabled = value != null, feel = Feel.Tick)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            style = Type.Meta.copy(color = reset),
        )
        Roll("%.1f".format(Locale.US, live), Type.Body.copy(fontWeight = FontWeight.SemiBold), digits = true)
    }
    Spacer(Modifier.size(Space.S1))
    Box(
        Modifier.fillMaxWidth().height(36.dp)
            .pointerInput(Unit) {
                val r = 11.dp.toPx()
                fun at(x: Float) = ((x - r) / (size.width - 2 * r).coerceAtLeast(1f)).coerceIn(0f, 1f)
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    held = true
                    var step = (at(down.position.x) * 20).roundToInt()
                    live = step / 10.0
                    view.feel(Feel.Tick)
                    scope.launch { pos.animateTo(at(down.position.x), spring(1f, 900f)) }
                    while (true) {
                        val e = awaitPointerEvent()
                        val ch = e.changes.firstOrNull { it.id == down.id } ?: break
                        if (!ch.pressed) break
                        ch.consume()
                        val f = at(ch.position.x)
                        scope.launch { pos.snapTo(f) }
                        val s = (f * 20).roundToInt()
                        if (s != step) {
                            step = s
                            live = s / 10.0
                            view.feel(Feel.Tick)
                        }
                    }
                    held = false
                    scope.launch { pos.animateTo(step / 20f, spring(0.9f, 700f)) }
                    set.value(step / 10.0)
                }
            }
            .drawBehind {
                val r = 11.dp.toPx()
                val h = 6.dp.toPx()
                val y = size.height / 2
                val x = r + pos.value * (size.width - 2 * r)
                drawRoundRect(Tone.Inset, Offset(0f, y - h / 2), Size(size.width, h), CornerRadius(h / 2))
                drawRoundRect(accent, Offset(0f, y - h / 2), Size(x, h), CornerRadius(h / 2))
                val k = 1f + 0.18f * swell.value
                drawCircle(Color.Black.copy(alpha = 0.28f), r * k + 2.dp.toPx(), Offset(x, y + 1.5.dp.toPx()))
                drawCircle(Color.White, r * k, Offset(x, y))
            },
    )
    Row {
        BasicText(tr("Precise", "Dokładnie"), Modifier.weight(1f), style = Type.Label)
        BasicText(tr("Inventive", "Pomysłowo"), style = Type.Label)
    }
}

@Composable
private fun ModelRow(m: AppModel, shell: Shell) {
    val accent = LocalAccent.current
    val name = m.status?.model?.name
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .pressable({ shell.open(Surface.Model, "details-model") }, Shapes.Card, Tone.Panel, Tone.Raise, depth = 0.985f, tilt = 2f)
            .padding(horizontal = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (name != null) ModelFace(m, name, m.status?.model?.family.orEmpty(), 36.dp)
        else Box(Modifier.size(36.dp).background(Tone.Raise, Shapes.ControlSm), contentAlignment = Alignment.Center) { Icon(Glyph.Models, Ink.Three, size = 18.dp) }
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(name ?: tr("No model loaded", "Nie wczytano modelu"), style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(tr("Change", "Zmień"), style = Type.Meta.copy(color = accent))
        }
        Icon(Glyph.Right, Ink.Four, size = 16.dp)
    }
}

@Composable
private fun ContextRow(m: AppModel, shell: Shell) {
    val accent = LocalAccent.current
    val chosen = m.chat?.contextName ?: m.defaultContext
    val info = m.contexts.firstOrNull { it.name == chosen }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .pressable({ shell.open(Surface.Context, "details-context") }, Shapes.Card, Tone.Panel, Tone.Raise, depth = 0.985f, tilt = 2f)
            .padding(horizontal = Space.S3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(if (chosen != null) accent.copy(alpha = 0.16f) else Tone.Raise, Shapes.ControlSm), contentAlignment = Alignment.Center) {
            Icon(Glyph.Contexts, if (chosen != null) accent else Ink.Three, size = 18.dp)
        }
        Spacer(Modifier.width(Space.S3))
        Column(Modifier.weight(1f)) {
            BasicText(chosen ?: tr("No context", "Bez kontekstu"), style = Type.Body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(
                if (chosen == null) tr("Answers from the chat alone", "Odpowiedzi tylko z czatu")
                else info?.tokens?.let { tokens(it) } ?: tr("Change", "Zmień"),
                style = Type.Meta,
            )
        }
        Icon(Glyph.Right, Ink.Four, size = 16.dp)
    }
}

/** Pin, a copy to take elsewhere, and delete behind an island that asks first. */
@Composable
private fun ChatActions(m: AppModel, c: Chat, shell: Shell) {
    val view = LocalView.current
    var asking by remember(c.id) { mutableStateOf(false) }
    LaunchedEffect(c.id) { snapshotFlow { shell.drawer.targetValue }.collect { if (it > -1f) asking = false } }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Column(Modifier.fillMaxWidth().clip(Shapes.Card), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).background(Tone.Panel).padding(horizontal = Space.S4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Glyph.Pin, Ink.Two, size = 18.dp)
                Spacer(Modifier.width(Space.S3))
                BasicText(tr("Pinned", "Przypięty"), Modifier.weight(1f), style = Type.Body.copy(fontWeight = FontWeight.Medium))
                Toggle(c.pinned, { m.pin(c.id) })
            }
            Line(Glyph.Copy, tr("Duplicate", "Duplikuj"), Ink.One) {
                m.duplicate(c.id)?.let { shell.openChat(it, c.title) }
            }
        }
        Spacer(Modifier.size(Space.S2))
        AnimatedContent(
            asking,
            transitionSpec = {
                val enter = if (shell.calm) fadeIn(tween(Motion.M2))
                else fadeIn(tween(Motion.M2, delayMillis = 40)) + scaleIn(spring(0.85f, 520f), 0.92f)
                enter togetherWith fadeOut(tween(Motion.M1)) using SizeTransform(clip = false) { _, _ -> spring(1f, 520f, IntSize.VisibilityThreshold) }
            },
            label = "delete",
        ) { ask ->
            if (!ask) {
                Line(Glyph.Trash, tr("Delete this chat", "Usuń ten czat"), Signal.Err, Shapes.Card) {
                    view.feel(Feel.Edge)
                    asking = true
                }
            } else {
                Column(
                    Modifier.fillMaxWidth()
                        .shadow(14.dp, Shapes.Card, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.45f))
                        .background(Tone.Raise, Shapes.Card)
                        .padding(Space.S4),
                ) {
                    BasicText(tr("Delete this chat?", "Usunąć ten czat?"), style = Type.Section)
                    Spacer(Modifier.size(Space.S1))
                    BasicText(
                        tr("It goes from this phone and from your computer.", "Zniknie z telefonu i z komputera."),
                        style = Type.Meta,
                    )
                    Spacer(Modifier.size(Space.S4))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.S2)) {
                        Btn(tr("Keep", "Zostaw"), { asking = false }, Modifier.weight(1f), variant = Variant.Quiet, height = 44.dp)
                        Btn(tr("Delete", "Usuń"), {
                            view.feel(Feel.Confirm)
                            m.delete(c.id)
                            shell.closeDrawer()
                        }, Modifier.weight(1f), variant = Variant.Danger, height = 44.dp)
                    }
                }
            }
        }
    }
}

/** Off first, then the panel's personalities as faces; the chosen one is lifted in the accent. */
@Composable
private fun Voices(chosen: String?, onPick: (String?) -> Unit) {
    val accent = LocalAccent.current
    val all = listOf<Persona?>(null) + Personas
    Column(Modifier.clip(Shapes.Card), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        all.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { p ->
                    val on = p?.id == chosen
                    val fill by animateColorAsState(if (on) accent.copy(alpha = 0.16f) else Tone.Panel, label = "voice")
                    val lift by animateFloatAsState(if (on) 1f else 0f, spring(0.55f, 480f), label = "lift")
                    Column(
                        Modifier.weight(1f).heightIn(min = 88.dp)
                            .pressable({ onPick(p?.id) }, RectangleShape, fill, Tone.Raise, depth = 0.95f, tilt = 6f)
                            .padding(vertical = Space.S3, horizontal = Space.S2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        BasicText(
                            p?.face ?: "🤖",
                            Modifier.graphicsLayer {
                                scaleX = 1f + 0.18f * lift
                                scaleY = scaleX
                                translationY = -3.dp.toPx() * lift
                            },
                            style = Type.Title.copy(color = Ink.One),
                        )
                        Spacer(Modifier.size(4.dp))
                        BasicText(
                            p?.let { tr(it.nameEn, it.namePl) } ?: tr("Off", "Wyłączona"),
                            style = Type.Label.copy(color = if (on) accent else Ink.Two, fontSize = 11.sp),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Section(shell: Shell, slot: Int, title: String?, content: @Composable () -> Unit) {
    Column(Modifier.arrive(shell, slot)) {
        if (title != null) BasicText(title, Modifier.padding(start = 8.dp, bottom = 8.dp), style = Type.Label)
        content()
    }
}

@Composable
private fun Line(glyph: Glyph, label: String, ink: Color, shape: Shape = RectangleShape, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .pressable(onClick, shape, Tone.Panel, Tone.Raise, depth = 0.985f)
            .padding(horizontal = Space.S4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(glyph, ink, size = 18.dp)
        Spacer(Modifier.width(Space.S3))
        BasicText(label, style = Type.Body.copy(color = ink, fontWeight = FontWeight.Medium))
    }
}
