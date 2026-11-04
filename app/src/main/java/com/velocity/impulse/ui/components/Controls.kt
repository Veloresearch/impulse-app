package com.velocity.impulse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.LocalReduceMotion
import com.velocity.impulse.ui.theme.Motion
import com.velocity.impulse.ui.theme.Shapes
import com.velocity.impulse.ui.theme.Signal
import com.velocity.impulse.ui.theme.Space
import com.velocity.impulse.ui.theme.Squircle
import com.velocity.impulse.ui.theme.Tone
import com.velocity.impulse.ui.theme.Type

enum class Variant { Primary, Standard, Quiet, Danger }

val OnAccent = Color(0xFF15131A)

@Composable
fun Btn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: Variant = Variant.Standard,
    icon: Glyph? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    blocked: (() -> Unit)? = null,
    description: String? = null,
) {
    val accent = LocalAccent.current
    val (rest, down, ink) = when (variant) {
        Variant.Primary -> Triple(accent, accent.copy(alpha = 0.8f), OnAccent)
        Variant.Standard -> Triple(Tone.Raise, Tone.Press, Ink.One)
        Variant.Quiet -> Triple(Color.Transparent, Tone.Raise, Ink.Two)
        Variant.Danger -> Triple(Tone.Raise, Tone.Press, Signal.Err)
    }
    Row(
        modifier
            .height(height)
            .semantics { contentDescription = description ?: text }
            .pressable(onClick, Squircle(height * 0.27f), rest, down, enabled = enabled, stretch = true, blocked = blocked)
            .padding(horizontal = Space.S4),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, ink, size = 18.dp)
            Spacer(Modifier.width(Space.S2))
        }
        BasicText(text, style = Type.Body.copy(color = ink, fontWeight = FontWeight.SemiBold))
    }
}

/**
 * A round button. Below 48dp the drawn circle keeps [size] while the touch area and what TalkBack
 * focuses grow to 48dp around it, without taking more room in the layout.
 */
@Composable
fun IconBtn(
    icon: Glyph,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ink.Two,
    fill: Color = Tone.Raise,
    size: Dp = 40.dp,
    enabled: Boolean = true,
    island: Boolean = false,
    liquid: Float = 1f,
    description: String = icon.spoken(),
) {
    Box(
        modifier.size(size).requiredSize(maxOf(size, Reach))
            .semantics { contentDescription = description }
            .pressable(
                onClick, Dot(size), fill, if (fill == Color.Transparent) Tone.Raise else Tone.Press,
                depth = 0.88f, enabled = enabled, stretch = true, liquid = liquid, feelOnDown = island,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, tint, size = size * 0.48f)
    }
}

private val Reach = 48.dp

/** A circle [diameter] across, centred in whatever box it is given. */
private class Dot(private val diameter: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = minOf(with(density) { diameter.toPx() }, size.minDimension) / 2f
        val c = size.center
        return Outline.Rounded(RoundRect(c.x - r, c.y - r, c.x + r, c.y + r, CornerRadius(r)))
    }

    override fun equals(other: Any?) = other is Dot && other.diameter == diameter
    override fun hashCode() = diameter.hashCode()
}

/** What TalkBack says for a round button that shows only [this] glyph. */
fun Glyph.spoken(): String = when (this) {
    Glyph.Home -> tr("Home", "Start")
    Glyph.Chat -> tr("Chat", "Czat")
    Glyph.Models -> tr("Models", "Modele")
    Glyph.Contexts -> tr("Memory", "Pamięć")
    Glyph.More -> tr("More", "Więcej")
    Glyph.Sidebar -> tr("Chats", "Czaty")
    Glyph.Settings -> tr("Settings", "Ustawienia")
    Glyph.Search -> tr("Search", "Szukaj")
    Glyph.Plus -> tr("Add", "Dodaj")
    Glyph.Send -> tr("Send", "Wyślij")
    Glyph.Stop -> tr("Stop", "Zatrzymaj")
    Glyph.Close -> tr("Close", "Zamknij")
    Glyph.Check -> tr("Done", "Gotowe")
    Glyph.Down -> tr("Scroll to the end", "Przewiń na koniec")
    Glyph.Left -> tr("Back", "Wstecz")
    Glyph.Right -> tr("Open", "Otwórz")
    Glyph.Copy -> tr("Copy", "Kopiuj")
    Glyph.Refresh -> tr("Try again", "Spróbuj ponownie")
    Glyph.Trash -> tr("Delete", "Usuń")
    Glyph.Activity -> tr("Activity", "Aktywność")
    Glyph.Pin -> tr("Pin", "Przypnij")
    Glyph.Brain -> tr("Thinking", "Myślenie")
    Glyph.Globe -> tr("Web search", "Wyszukiwanie w sieci")
    Glyph.Phone -> tr("Phone", "Telefon")
    Glyph.Lock -> tr("Locked", "Zablokowane")
    Glyph.Info -> tr("Details", "Szczegóły")
    Glyph.Alert -> tr("Warning", "Ostrzeżenie")
    Glyph.Link -> tr("Link", "Link")
    Glyph.Sparkle -> tr("Suggestions", "Podpowiedzi")
    Glyph.QrCode -> tr("Scan a code", "Zeskanuj kod")
    Glyph.SignOut -> tr("Sign out", "Wyloguj")
    Glyph.Palette -> tr("Appearance", "Wygląd")
    Glyph.Wifi -> tr("Network", "Sieć")
    Glyph.Cloud -> tr("Tunnel", "Tunel")
    Glyph.Shield -> tr("Private network", "Sieć prywatna")
    Glyph.Eye -> tr("Show", "Pokaż")
    Glyph.EyeOff -> tr("Hide", "Ukryj")
    Glyph.Camera -> tr("Camera", "Aparat")
    Glyph.File -> tr("Attach a file", "Dołącz plik")
    Glyph.Image -> tr("Attach a picture", "Dołącz obraz")
    Glyph.History -> tr("History", "Historia")
    Glyph.Users -> tr("Profiles", "Profile")
}

@Composable
fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, description: String? = null) {
    val accent = LocalAccent.current
    val calm = LocalReduceMotion.current
    val track = animateColorAsState(if (checked) accent else Tone.Press, tween(Motion.M2), label = "track")
    val knob = animateFloatAsState(if (checked) 1f else 0f, Motion.travel(), label = "knob")
    val blob = rememberLiquid(0.6f)
    Box(
        modifier
            .size(51.dp, 31.dp)
            .semantics {
                toggleableState = ToggleableState(checked)
                if (description != null) contentDescription = description
            }
            .then(if (calm) Modifier else Modifier.liquidTouch(blob))
            .pressable({ onChange(!checked) }, depth = 1f, feel = if (checked) Feel.Off else Feel.On, role = Role.Switch)
            .drawBehind { drawRoundRect(track.value, cornerRadius = CornerRadius(size.height / 2f)) }
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .graphicsLayer { translationX = knob.value * 20.dp.toPx() }
                .size(27.dp)
                .liquidShape(blob, CircleShape) { Color.White },
        )
    }
}

/** Equal-width segments; the thumb travels to the chosen one and can be dragged between them. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    LiquidSlots(
        options.size, selected, onSelect, Tone.Hover, Shapes.ControlSm,
        modifier.fillMaxWidth().height(40.dp).background(Tone.Inset, Shapes.Control).padding(3.dp).selectableGroup(),
    ) {
        options.forEachIndexed { i, label ->
            val ink by animateColorAsState(if (i == selected) Ink.One else Ink.Three, label = "seg")
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .semantics { this.selected = i == selected }
                    .pressable({ onSelect(i) }, depth = 0.96f, role = Role.Tab),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    label,
                    Modifier.padding(horizontal = 2.dp),
                    style = (if (options.size >= 5) Type.Label else Type.Meta).copy(color = ink, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    secret: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    action: ImeAction = ImeAction.Done,
    onAction: () -> Unit = {},
    style: TextStyle = Type.Body,
) {
    val accent = LocalAccent.current
    var shown by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(accent),
        visualTransformation = if (secret && !shown) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret && shown) KeyboardType.Text else keyboard, imeAction = action, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onAny = { onAction() }),
        modifier = modifier.fillMaxWidth().height(50.dp).background(Tone.Inset, Shapes.ControlLg).padding(start = Space.S4, end = if (secret) Space.S1 else Space.S4),
        decorationBox = { inner ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) BasicText(placeholder, style = style.copy(color = Ink.Four))
                    inner()
                }
                if (secret) {
                    val flip by animateFloatAsState(if (shown) 180f else 0f, Motion.travel(), label = "eye")
                    Box(
                        Modifier.size(42.dp)
                            .semantics { contentDescription = if (shown) tr("Hide password", "Ukryj hasło") else tr("Show password", "Pokaż hasło") }
                            .pressable({ shown = !shown }, CircleShape, depth = 0.86f, stretch = true)
                            .graphicsLayer { rotationY = flip; cameraDistance = 12f * density },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (flip > 90f) Glyph.EyeOff else Glyph.Eye, if (shown) accent else Ink.Three, Modifier.graphicsLayer { rotationY = if (flip > 90f) 180f else 0f }, size = 20.dp)
                    }
                }
            }
        },
    )
}
