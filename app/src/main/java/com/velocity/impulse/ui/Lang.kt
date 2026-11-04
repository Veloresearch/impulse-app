package com.velocity.impulse.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** English for now. The Polish strings stay beside each English one for when the app follows Impulse's language again. */
object Lang {
    var pl by mutableStateOf(false)

    fun tr(en: String, pl: String) = if (this.pl) pl else en
}
