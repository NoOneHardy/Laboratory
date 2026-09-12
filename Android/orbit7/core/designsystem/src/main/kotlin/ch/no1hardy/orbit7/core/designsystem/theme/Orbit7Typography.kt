package ch.no1hardy.orbit7.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Two families, with a strict split (`docs/04-design-system.md` §2).
 *
 * Monospace carries every numeral — amounts, dates, counters, table columns — with tabular figures,
 * because a readout that reflows as digits change destroys the instrument illusion. The grotesque
 * carries body copy: long German strings in a monospace face become unreadable at small sizes,
 * which is exactly why this is not a single-font design.
 *
 * The bundled font files are an open question in `docs/07-roadmap.md` (JetBrains Mono + Inter, or
 * IBM Plex Mono + Space Grotesk — both OFL, licence to be checked before the files are committed).
 * Until then the system monospace and sans stand in, and every style below already has its final
 * size, tracking and case.
 */
@Immutable
data class Orbit7Typography(
    val mono: FontFamily = FontFamily.Monospace,
    val grotesque: FontFamily = FontFamily.SansSerif,
) {
    /** EP balance, Quick Add amount. */
    val readoutXL: TextStyle =
        TextStyle(
            fontFamily = mono,
            fontSize = 48.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )

    /** Screen-level figures. */
    val readoutL: TextStyle = TextStyle(fontFamily = mono, fontSize = 32.sp, fontWeight = FontWeight.Medium)

    /** Row amounts, countdowns. */
    val readoutM: TextStyle = TextStyle(fontFamily = mono, fontSize = 20.sp)

    /** Panel captions, axis labels. */
    val label: TextStyle =
        TextStyle(
            fontFamily = mono,
            fontSize = 12.sp,
            letterSpacing = 0.08.em,
            fontWeight = FontWeight.Medium,
        )

    val titleL: TextStyle = TextStyle(fontFamily = grotesque, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    val titleM: TextStyle = TextStyle(fontFamily = grotesque, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val body: TextStyle = TextStyle(fontFamily = grotesque, fontSize = 15.sp)
    val bodyS: TextStyle = TextStyle(fontFamily = grotesque, fontSize = 13.sp)
}
