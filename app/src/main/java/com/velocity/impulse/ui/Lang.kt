package com.velocity.impulse.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** English or Polish, following the language the profile chose in Impulse. */
object Lang {
    var pl by mutableStateOf(false)

    fun tr(en: String, pl: String) = if (this.pl) pl else en
}
