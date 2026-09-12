package ch.no1hardy.orbit7.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.no1hardy.orbit7.core.designsystem.TestTags
import ch.no1hardy.orbit7.core.designsystem.theme.Orbit7Theme

/**
 * The one place the design system is allowed to shout.
 *
 * Hazard striping is reserved for an overdrawn week, a deadline inside 14 days, and destructive
 * confirmations. The banner always carries a text label as well as the stripes: no information is
 * ever conveyed by colour or pattern alone.
 */
@Composable
fun HazardBanner(
    title: String,
    detail: String?,
    modifier: Modifier = Modifier,
    testTag: String = TestTags.HAZARD_BANNER,
) {
    val colors = Orbit7Theme.colors
    Column(
        modifier =
            modifier
                .testTag(testTag)
                .fillMaxWidth()
                .semantics {
                    contentDescription = detail?.let { "$title. $it" } ?: title
                    liveRegion = LiveRegionMode.Polite
                },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().height(STRIPE_BAND)) {
            drawHazardStripes(colors.hazardStripe, colors.surface)
        }
        Box(Modifier.padding(horizontal = 4.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = Orbit7Theme.typography.titleM, color = colors.danger)
                if (detail != null) {
                    Text(detail, style = Orbit7Theme.typography.bodyS, color = colors.onSurfaceMuted)
                }
            }
        }
    }
}

private val STRIPE_BAND = 6.dp
