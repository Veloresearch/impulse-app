package com.velocity.impulse.ui.components

import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants.CLOCK_TICK
import android.view.HapticFeedbackConstants.CONFIRM
import android.view.HapticFeedbackConstants.CONTEXT_CLICK
import android.view.HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
import android.view.HapticFeedbackConstants.GESTURE_THRESHOLD_DEACTIVATE
import android.view.HapticFeedbackConstants.KEYBOARD_TAP
import android.view.HapticFeedbackConstants.LONG_PRESS
import android.view.HapticFeedbackConstants.REJECT
import android.view.HapticFeedbackConstants.SEGMENT_TICK
import android.view.HapticFeedbackConstants.TOGGLE_OFF
import android.view.HapticFeedbackConstants.TOGGLE_ON
import android.view.HapticFeedbackConstants.VIRTUAL_KEY
import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/** The haptic vocabulary, with the closest constant for Android 14+, 11+ and older phones. */
enum class Feel(val v34: Int, val v30: Int, val base: Int) {
    Tap(KEYBOARD_TAP, KEYBOARD_TAP, VIRTUAL_KEY),
    Tick(SEGMENT_TICK, CLOCK_TICK, CLOCK_TICK),
    Edge(GESTURE_THRESHOLD_ACTIVATE, CONTEXT_CLICK, CONTEXT_CLICK),
    Unedge(GESTURE_THRESHOLD_DEACTIVATE, CLOCK_TICK, CLOCK_TICK),
    Confirm(CONFIRM, CONFIRM, VIRTUAL_KEY),
    Lock(CONFIRM, CONFIRM, LONG_PRESS),
    Reject(REJECT, REJECT, LONG_PRESS),
    Hold(LONG_PRESS, LONG_PRESS, LONG_PRESS),
    On(TOGGLE_ON, VIRTUAL_KEY, VIRTUAL_KEY),
    Off(TOGGLE_OFF, CLOCK_TICK, CLOCK_TICK),
    ;

    companion object {
        /** Off when the owner turned haptics off; every [feel] respects it. */
        var enabled = true
    }
}

private var lastFelt = 0L

/** Plays [f], dropping it when another one went out less than 60 ms ago so ticks never buzz. */
fun View.feel(f: Feel) {
    if (!Feel.enabled) return
    val now = SystemClock.uptimeMillis()
    if (now - lastFelt < 60) return
    lastFelt = now
    performHapticFeedback(
        when {
            Build.VERSION.SDK_INT >= 34 -> f.v34
            Build.VERSION.SDK_INT >= 30 -> f.v30
            else -> f.base
        },
    )
}

/** TalkBack reads [text] once without moving focus: a finished answer, a toast. */
@Suppress("DEPRECATION") // The live-region replacement re-reads a streaming answer on every token.
fun View.announce(text: String) {
    if (text.isNotBlank()) announceForAccessibility(text)
}

/** TalkBack reads this element again whenever its text changes, after what it is saying now. */
fun Modifier.polite(): Modifier = semantics { liveRegion = LiveRegionMode.Polite }
