package com.velocity.impulse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.velocity.impulse.R

enum class Weight { Regular, Bold, Fill }

/**
 * The icon vocabulary of the Impulse panel (Phosphor), one glyph per meaning and drawn the same
 * way: places and states filled, actions as bold lines.
 */
enum class Glyph(private val regular: Int, private val bold: Int, private val fill: Int, val weight: Weight) {
    Home(R.drawable.ph_house_regular, R.drawable.ph_house_bold, R.drawable.ph_house_fill, Weight.Fill),
    Chat(R.drawable.ph_chat_teardrop_regular, R.drawable.ph_chat_teardrop_bold, R.drawable.ph_chat_teardrop_fill, Weight.Fill),
    Models(R.drawable.ph_cube_regular, R.drawable.ph_cube_bold, R.drawable.ph_cube_fill, Weight.Fill),
    Contexts(R.drawable.ph_stack_regular, R.drawable.ph_stack_bold, R.drawable.ph_stack_fill, Weight.Fill),
    More(R.drawable.ph_dots_three_regular, R.drawable.ph_dots_three_bold, R.drawable.ph_dots_three_fill, Weight.Bold),
    Sidebar(R.drawable.ph_sidebar_simple_regular, R.drawable.ph_sidebar_simple_bold, R.drawable.ph_sidebar_simple_fill, Weight.Bold),
    Settings(R.drawable.ph_faders_regular, R.drawable.ph_faders_bold, R.drawable.ph_faders_fill, Weight.Bold),
    Search(R.drawable.ph_magnifying_glass_regular, R.drawable.ph_magnifying_glass_bold, R.drawable.ph_magnifying_glass_fill, Weight.Bold),
    Plus(R.drawable.ph_plus_regular, R.drawable.ph_plus_bold, R.drawable.ph_plus_fill, Weight.Bold),
    Send(R.drawable.ph_arrow_up_regular, R.drawable.ph_arrow_up_bold, R.drawable.ph_arrow_up_fill, Weight.Bold),
    Stop(R.drawable.ph_stop_regular, R.drawable.ph_stop_bold, R.drawable.ph_stop_fill, Weight.Fill),
    Close(R.drawable.ph_x_regular, R.drawable.ph_x_bold, R.drawable.ph_x_fill, Weight.Bold),
    Check(R.drawable.ph_check_regular, R.drawable.ph_check_bold, R.drawable.ph_check_fill, Weight.Bold),
    Down(R.drawable.ph_caret_down_regular, R.drawable.ph_caret_down_bold, R.drawable.ph_caret_down_fill, Weight.Bold),
    Left(R.drawable.ph_caret_left_regular, R.drawable.ph_caret_left_bold, R.drawable.ph_caret_left_fill, Weight.Bold),
    Right(R.drawable.ph_caret_right_regular, R.drawable.ph_caret_right_bold, R.drawable.ph_caret_right_fill, Weight.Bold),
    Copy(R.drawable.ph_copy_regular, R.drawable.ph_copy_bold, R.drawable.ph_copy_fill, Weight.Fill),
    Refresh(R.drawable.ph_arrow_clockwise_regular, R.drawable.ph_arrow_clockwise_bold, R.drawable.ph_arrow_clockwise_fill, Weight.Fill),
    Trash(R.drawable.ph_trash_regular, R.drawable.ph_trash_bold, R.drawable.ph_trash_fill, Weight.Bold),
    Activity(R.drawable.ph_pulse_regular, R.drawable.ph_pulse_bold, R.drawable.ph_pulse_fill, Weight.Bold),
    Pin(R.drawable.ph_push_pin_regular, R.drawable.ph_push_pin_bold, R.drawable.ph_push_pin_fill, Weight.Fill),
    Brain(R.drawable.ph_brain_regular, R.drawable.ph_brain_bold, R.drawable.ph_brain_fill, Weight.Fill),
    Globe(R.drawable.ph_globe_regular, R.drawable.ph_globe_bold, R.drawable.ph_globe_fill, Weight.Bold),
    Phone(R.drawable.ph_device_mobile_regular, R.drawable.ph_device_mobile_bold, R.drawable.ph_device_mobile_fill, Weight.Fill),
    Lock(R.drawable.ph_lock_regular, R.drawable.ph_lock_bold, R.drawable.ph_lock_fill, Weight.Fill),
    Info(R.drawable.ph_info_regular, R.drawable.ph_info_bold, R.drawable.ph_info_fill, Weight.Fill),
    Alert(R.drawable.ph_warning_regular, R.drawable.ph_warning_bold, R.drawable.ph_warning_fill, Weight.Fill),
    Link(R.drawable.ph_link_regular, R.drawable.ph_link_bold, R.drawable.ph_link_fill, Weight.Bold),
    Sparkle(R.drawable.ph_sparkle_regular, R.drawable.ph_sparkle_bold, R.drawable.ph_sparkle_fill, Weight.Fill),
    QrCode(R.drawable.ph_qr_code_regular, R.drawable.ph_qr_code_bold, R.drawable.ph_qr_code_fill, Weight.Bold),
    SignOut(R.drawable.ph_sign_out_regular, R.drawable.ph_sign_out_bold, R.drawable.ph_sign_out_fill, Weight.Bold),
    Palette(R.drawable.ph_palette_regular, R.drawable.ph_palette_bold, R.drawable.ph_palette_fill, Weight.Fill),
    Wifi(R.drawable.ph_wifi_high_regular, R.drawable.ph_wifi_high_bold, R.drawable.ph_wifi_high_fill, Weight.Bold),
    Cloud(R.drawable.ph_cloud_regular, R.drawable.ph_cloud_bold, R.drawable.ph_cloud_fill, Weight.Fill),
    Shield(R.drawable.ph_shield_check_regular, R.drawable.ph_shield_check_bold, R.drawable.ph_shield_check_fill, Weight.Fill),
    Eye(R.drawable.ph_eye_regular, R.drawable.ph_eye_bold, R.drawable.ph_eye_fill, Weight.Fill),
    EyeOff(R.drawable.ph_eye_slash_regular, R.drawable.ph_eye_slash_bold, R.drawable.ph_eye_slash_fill, Weight.Fill),
    Users(R.drawable.ph_users_regular, R.drawable.ph_users_bold, R.drawable.ph_users_fill, Weight.Fill);

    fun res(w: Weight = weight) = when (w) {
        Weight.Regular -> regular
        Weight.Bold -> bold
        Weight.Fill -> fill
    }
}

@Composable
fun Icon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp, weight: Weight = glyph.weight) {
    Image(painterResource(glyph.res(weight)), null, modifier.size(size), colorFilter = ColorFilter.tint(tint))
}
