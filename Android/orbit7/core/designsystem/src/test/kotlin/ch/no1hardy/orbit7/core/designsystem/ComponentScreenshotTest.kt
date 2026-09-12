package ch.no1hardy.orbit7.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
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
import org.junit.Rule
import org.junit.Test

/**
 * The design system's goldens (`docs/06-test-strategy.md` §7).
 *
 * Every component is rendered in three configurations, because these are the regressions that
 * actually happen and that nobody notices by re-opening a screen that "still works":
 *
 * | Config | What it catches |
 * |---|---|
 * | Default (Pixel 6, de-CH) | the reference look |
 * | Font scale 2.0 | truncated numbers and broken panels |
 * | `reduceEffects` on | that the app is still coherent with every effect disabled |
 *
 * Animations are pinned to a fixed frame and every fixture is a constant, so the goldens are
 * deterministic.
 */
class ComponentScreenshotTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_6.copy(locale = "de-CH"),
            showSystemUi = false,
        )

    @Test
    fun `components at the reference configuration`() {
        paparazzi.snapshot(name = "components_default") {
            Orbit7Theme(reduceEffects = false) { Gallery() }
        }
    }

    @Test
    fun `components at the largest font scale`() {
        paparazzi.unsafeUpdateConfig(
            deviceConfig = DeviceConfig.PIXEL_6.copy(locale = "de-CH", fontScale = LARGEST_FONT_SCALE),
        )
        paparazzi.snapshot(name = "components_font_scale_200") {
            Orbit7Theme(reduceEffects = false) { Gallery() }
        }
    }

    @Test
    fun `components with every effect disabled`() {
        paparazzi.snapshot(name = "components_reduce_effects") {
            Orbit7Theme(reduceEffects = true) { Gallery() }
        }
    }

    /**
     * The same composable as the debug component gallery screen, so what is reviewed on a device is
     * what is committed as a golden.
     */
    @Composable
    private fun Gallery() {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Panel(caption = "Leistung") {
                ReadoutWindow {
                    DigitRollText(text = "1'340 EP", contentDescription = "1340 Energiepunkte")
                }
            }
            Gauge(progress = 0.62f, contentDescription = "62 Prozent des Wochenbudgets übrig", dayMarker = 0.5f)
            Gauge(progress = 1f, contentDescription = "Budget überschritten", overdrawn = true)
            HazardBanner(title = "Frist in 3 Tagen", detail = "Fitness Wankdorf")
            SwitchToggle(
                checked = true,
                onCheckedChange = {},
                label = "Effekte reduzieren",
                stateOn = "ein",
                stateOff = "aus",
            )
            CategoryChip(
                label = "Lebensmittel",
                selected = true,
                onClick = {},
                stateDescription = "ausgewählt",
                testTag = TestTags.categoryChip(1),
            )
            NumpadKey(label = "7", onClick = {})
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
                levels =
                    mapOf(
                        StationModuleKey.REACTOR_CORE to 2,
                        StationModuleKey.LIFE_SUPPORT to 1,
                    ),
                contentDescription = "ORBIT-7: Reaktorkern Stufe 2, Lebenserhaltung Stufe 1",
            )
            Text(text = "CHF 1'234.50", style = Orbit7Theme.typography.readoutM)
        }
    }

    private companion object {
        const val LARGEST_FONT_SCALE = 2.0f
    }
}
