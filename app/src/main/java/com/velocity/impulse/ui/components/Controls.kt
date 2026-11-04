package com.velocity.impulse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.Ink
import com.velocity.impulse.ui.theme.LocalAccent
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
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 48.dp,
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
            .pressable(onClick, Squircle(height * 0.27f), rest, down, enabled = enabled)
            .padding(horizontal = Space.S4),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(icon, null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(ink))
            Spacer(Modifier.width(Space.S2))
        }
        BasicText(text, style = Type.Body.copy(color = ink, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun IconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ink.Two,
    fill: Color = Tone.Raise,
    size: Dp = 40.dp,
    enabled: Boolean = true,
) {
    Box(
        modifier.size(size).pressable(onClick, CircleShape, fill, if (fill == Color.Transparent) Tone.Raise else Tone.Press, depth = 0.88f, enabled = enabled),
        contentAlignment = Alignment.Center,
    ) {
        Image(icon, null, Modifier.size(size * 0.5f), colorFilter = ColorFilter.tint(tint))
    }
}

@Composable
fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    val track by animateColorAsState(if (checked) accent else Tone.Press, label = "track")
    val knob by animateDpAsState(if (checked) 20.dp else 0.dp, Motion.travel(), label = "knob")
    Box(
        modifier
            .size(51.dp, 31.dp)
            .pressable({ onChange(!checked) }, depth = 0.92f, role = Role.Switch)
            .background(track, CircleShape)
            .padding(2.dp),
    ) {
        Box(Modifier.offset(x = knob).size(27.dp).background(Color.White, CircleShape))
    }
}

/** Equal-width segments; the thumb travels to the chosen one. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth().height(40.dp).background(Tone.Inset, Shapes.Control).padding(3.dp)) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected, Motion.travel(), label = "thumb")
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().background(Tone.Hover, Shapes.ControlSm))
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                val ink by animateColorAsState(if (i == selected) Ink.One else Ink.Three, label = "seg")
                Box(
                    Modifier.weight(1f).fillMaxHeight().pressable({ onSelect(i) }, depth = 0.95f),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(label, style = Type.Meta.copy(color = ink, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center))
                }
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
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(accent),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = action, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onAny = { onAction() }),
        modifier = modifier.fillMaxWidth().height(50.dp).background(Tone.Inset, Shapes.ControlLg).padding(horizontal = Space.S4),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) BasicText(placeholder, style = style.copy(color = Ink.Four))
                inner()
            }
        },
    )
}
