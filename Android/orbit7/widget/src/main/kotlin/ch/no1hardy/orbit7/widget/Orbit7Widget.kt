package ch.no1hardy.orbit7.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** What the widget shows. Computed by [Orbit7WidgetRepository], never by the widget itself. */
data class WidgetState(
    val balanceEp: Int,
    val remainingThisWeekMinor: Long,
    val todaySpentMinor: Long,
    val weekProgress: Float,
    val hasData: Boolean,
)

/**
 * The home-screen widget (`docs/03-screens.md`, Android surfaces).
 *
 * Two sizes: a 2×2 power ring, and a 4×2 that adds the week's remaining budget, today's total and a
 * quick-add target. It renders in the same cassette-futurist palette **with all motion removed** —
 * widgets do not animate, and pretending otherwise wastes battery.
 */
class Orbit7Widget(
    private val stateProvider: suspend (Context) -> WidgetState,
) : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val state = stateProvider(context)
        provideContent { Content(state) }
    }

    @Composable
    private fun Content(state: WidgetState) {
        val context = LocalContext.current
        val width = LocalSize.current.width

        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(VOID)
                    .cornerRadius(8.dp)
                    .padding(12.dp)
                    .clickable(actionStartActivity(quickAddIntent(context))),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = context.getString(R.string.widget_power, state.balanceEp),
                style = readout(AMBER),
                modifier =
                    GlanceModifier.semantics {
                        contentDescription = context.getString(R.string.widget_power_description, state.balanceEp)
                    },
            )

            if (!state.hasData) {
                Text(text = context.getString(R.string.widget_empty), style = label(MUTED))
                return@Column
            }

            if (width >= MEDIUM_WIDTH) {
                Spacer(GlanceModifier.height(8.dp))
                PowerBar(progress = state.weekProgress)
                Spacer(GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = context.getString(R.string.widget_remaining, formatMinor(state.remainingThisWeekMinor)),
                        style = label(CREAM),
                    )
                }
                Text(
                    text = context.getString(R.string.widget_today, formatMinor(state.todaySpentMinor)),
                    style = label(MUTED),
                )
                Text(text = context.getString(R.string.widget_quick_add), style = label(GREEN))
            }
        }
    }

    /** A static bar: the gauge from the Bridge, with the animation taken out. */
    @Composable
    private fun PowerBar(progress: Float) {
        Row(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(BEZEL),
        ) {
            val filled = (progress.coerceIn(0f, 1f) * BAR_SEGMENTS).toInt()
            repeat(BAR_SEGMENTS) { index ->
                Spacer(
                    GlanceModifier
                        .defaultWeight()
                        .height(8.dp)
                        .background(if (index < filled) GREEN else BEZEL),
                )
            }
        }
    }

    private fun readout(color: ColorProvider) =
        TextStyle(
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
        )

    private fun label(color: ColorProvider) = TextStyle(color = color, fontFamily = FontFamily.Monospace)

    /** `CHF 12.50`, formatted here because a Glance widget cannot reach the app's injector. */
    private fun formatMinor(minor: Long): String {
        val negative = minor < 0
        val absolute = if (negative) -minor else minor
        val major = absolute / 100
        val rest = absolute % 100
        val sign = if (negative) "−" else ""
        return "CHF $sign$major.${rest.toString().padStart(2, '0')}"
    }

    private fun quickAddIntent(context: Context) =
        context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.putExtra(EXTRA_DESTINATION, DESTINATION_QUICK_ADD)
            ?: android.content.Intent()

    companion object {
        const val EXTRA_DESTINATION = "orbit7.destination"
        const val DESTINATION_QUICK_ADD = "quick_add"

        private const val BAR_SEGMENTS = 12
        private val MEDIUM_WIDTH = 180.dp

        private val VOID = ColorProvider(Color(0xFF07090A))
        private val BEZEL = ColorProvider(Color(0xFF1D262B))
        private val AMBER = ColorProvider(Color(0xFFFFB000))
        private val GREEN = ColorProvider(Color(0xFF33FF66))
        private val CREAM = ColorProvider(Color(0xFFE8E2D4))
        private val MUTED = ColorProvider(Color(0xFF8496A0))
    }
}
