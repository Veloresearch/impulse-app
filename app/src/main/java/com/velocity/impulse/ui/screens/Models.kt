package com.velocity.impulse.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.velocity.impulse.data.AppModel
import com.velocity.impulse.ui.Lang.tr
import com.velocity.impulse.ui.components.Btn
import com.velocity.impulse.ui.components.Empty
import com.velocity.impulse.ui.components.PageTitle
import com.velocity.impulse.ui.components.Variant
import com.velocity.impulse.ui.theme.LocalAccent
import com.velocity.impulse.ui.theme.Shapes

@Composable
fun ModelsScreen(m: AppModel) {
    val accent = LocalAccent.current
    Page {
        PageTitle(
            tr("Models", "Modele"),
            tr("Models are the minds that write the answers. These are the ones on your computer; tap one to load it.", "Modele to mózgi, które piszą odpowiedzi. Oto te na Twoim komputerze. Dotknij, żeby wczytać."),
        )
        if (m.library.isEmpty()) {
            Empty(
                tr("No models yet", "Nie ma jeszcze modeli"),
                tr("Download a model in Impulse on your computer. It appears here as soon as it is there.", "Pobierz model w Impulse na komputerze. Pojawi się tu od razu."),
            )
        } else {
            Column(Modifier.fillMaxWidth().clip(Shapes.Tile), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                m.library.forEach { p -> ModelRow(m, p, accent) }
            }
        }
        if (m.status?.model != null) Btn(tr("Unload the model", "Zwolnij model"), { m.unload() }, Modifier.fillMaxWidth(), Variant.Standard)
    }
}
