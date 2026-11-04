package com.velocity.impulse.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velocity.impulse.ui.theme.Space

/** A scrolling page with room at the top for the floating back island. */
@Composable
fun Page(scroll: ScrollState = rememberScrollState(), content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = Space.S4),
        verticalArrangement = Arrangement.spacedBy(Space.S4),
    ) {
        Spacer(Modifier.statusBarsPadding().height(60.dp))
        content()
        Spacer(Modifier.navigationBarsPadding().height(32.dp))
    }
}
