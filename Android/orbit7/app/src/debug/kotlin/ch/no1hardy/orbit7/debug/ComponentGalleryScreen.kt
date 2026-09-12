package ch.no1hardy.orbit7.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.component.BusLine
import ch.no1hardy.orbit7.core.designsystem.component.CategoryChip
import ch.no1hardy.orbit7.core.designsystem.component.DigitRollText
import ch.no1hardy.orbit7.core.designsystem.component.Gauge
import ch.no1hardy.orbit7.core.designsystem.component.HazardBanner
import ch.no1hardy.orbit7.core.designsystem.component.ModuleTile
import ch.no1hardy.orbit7.core.designsystem.component.NumpadKey
import ch.no1hardy.orbit7.core.designsystem.component.Panel
import ch.no1hardy.orbit7.core.designsystem.component.ReadoutWindow
import ch.no1hardy.orbit7.core.designsystem.component.StationView
import ch.no1hardy.orbit7.core.designsystem.component.SwitchToggle
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme
import ch.no1hardy.orbit7.core.domain.model.StationModuleKey

/**
 * Every component on one screen, for manual review on a real device
 * (`docs/04-design-system.md` §7).
 *
 * Screenshot goldens are rendered from the same fixtures, so what is reviewed by eye here is what
 * is committed as an image — and the toggles let a reviewer see the digit roll, the throw of the
 * switch and the key press, which no golden can show.
 */
@Composable
fun ComponentGalleryScreen() {
    var digits by rememberSaveable { mutableStateOf(1_340) }
    var toggled by rememberSaveable { mutableStateOf(true) }
    var selectedChip by rememberSaveable { mutableStateOf(true) }
    var gauge by rememberSaveable { mutableStateOf(0.62f) }

    Column(
        modifier =
            Modifier
                .testTag(TestTags.GALLERY)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Panel(caption = "Panel + ReadoutWindow + DigitRollText") {
            ReadoutWindow {
                DigitRollText(
                    text = "$digits EP",
                    contentDescription = "$digits Energiepunkte",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumpadKey(label = "+", onClick = { digits += 37 }, contentDescription = "Add")
                NumpadKey(label = "-", onClick = { digits -= 37 }, contentDescription = "Subtract")
            }
        }

        Panel(caption = "Gauge") {
            Gauge(progress = gauge, contentDescription = "Gauge at $gauge", dayMarker = 0.5f)
            Gauge(progress = 1f, contentDescription = "Overdrawn", overdrawn = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumpadKey(
                    label = "<",
                    onClick = { gauge = (gauge - 0.1f).coerceAtLeast(0f) },
                    contentDescription = "Less",
                )
                NumpadKey(
                    label = ">",
                    onClick = { gauge = (gauge + 0.1f).coerceAtMost(1f) },
                    contentDescription = "More",
                )
            }
        }

        HazardBanner(title = "Frist in 3 Tagen", detail = "Fitness Wankdorf")

        BusLine()

        Panel(caption = "Controls") {
            SwitchToggle(
                checked = toggled,
                onCheckedChange = { toggled = it },
                label = "Effekte reduzieren",
                stateOn = "ein",
                stateOff = "aus",
            )
            CategoryChip(
                label = "Lebensmittel",
                selected = selectedChip,
                onClick = { selectedChip = !selectedChip },
                stateDescription = if (selectedChip) "ausgewählt" else "nicht ausgewählt",
                testTag = TestTags.categoryChip(1),
            )
        }

        ModuleTile(
            name = "Reaktorkern",
            levelLabel = "L2",
            statusLabel = "190 EP",
            contentDescription = "Reaktorkern, Stufe 2 von 5, nächste Stufe 190 Energiepunkte",
            affordable = true,
            locked = false,
            lit = true,
            onClick = {},
            testTag = TestTags.moduleTile(StationModuleKey.REACTOR_CORE.name),
        )

        StationView(
            levels = StationModuleKey.entries.associateWith { if (it.ordinal < 3) 2 else 0 },
            contentDescription = "ORBIT-7, drei Module mit Energie",
        )

        Text(
            text = "CHF 1'234.50 · 14.04.2025 · +20 EP",
            style = Orbit7Theme.typography.readoutM,
            color = Orbit7Theme.colors.onSurface,
        )
    }
}
