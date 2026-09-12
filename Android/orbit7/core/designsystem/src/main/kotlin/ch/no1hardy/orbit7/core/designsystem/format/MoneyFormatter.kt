package ch.no1hardy.orbit7.core.designsystem.format

import ch.no1hardy.orbit7.core.domain.money.Money
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

/**
 * The one place money becomes text (`docs/04-design-system.md` §8).
 *
 * Swiss formatting: apostrophe grouping and a period decimal separator — `CHF 1'234.50`. It takes
 * `Long` minor units, never a floating point number, and it is the only function in the app allowed
 * to produce a currency string.
 */
@Singleton
class MoneyFormatter
    @Inject
    constructor() {
        fun format(
            amount: Money,
            currency: String = DEFAULT_CURRENCY,
            withSign: Boolean = false,
        ): String {
            val negative = amount.minor < 0
            val absolute = amount.minor.absoluteValue
            val major = absolute / Money.MINOR_PER_MAJOR
            val minor = absolute % Money.MINOR_PER_MAJOR
            val sign =
                when {
                    negative -> "−" // U+2212, a minus sign rather than a hyphen: it aligns with digits
                    withSign -> "+"
                    else -> ""
                }
            return "$currency $sign${group(major)}.${minor.toString().padStart(2, '0')}"
        }

        /** Without the currency code, for dense table columns where the code is in the header. */
        fun formatPlain(amount: Money): String = format(amount, currency = "").trim()

        /** EP is an integer and is grouped the same way: `1'340 EP`. */
        fun formatEnergy(
            ep: Int,
            withSign: Boolean = false,
        ): String {
            val sign =
                if (ep < 0) {
                    "−"
                } else if (withSign) {
                    "+"
                } else {
                    ""
                }
            return "$sign${group(ep.toLong().absoluteValue)} EP"
        }

        /** `Mo, 14.04.` in de-CH; the locale decides the pattern. */
        fun formatShortDate(
            date: LocalDate,
            locale: Locale,
        ): String = date.format(DateTimeFormatter.ofPattern(shortDatePattern(locale), locale))

        fun formatLongDate(
            date: LocalDate,
            locale: Locale,
        ): String = date.format(DateTimeFormatter.ofPattern(longDatePattern(locale), locale))

        private fun shortDatePattern(locale: Locale): String =
            if (locale.language ==
                GERMAN
            ) {
                "EE, dd.MM."
            } else {
                "EE, dd MMM"
            }

        private fun longDatePattern(locale: Locale): String =
            if (locale.language ==
                GERMAN
            ) {
                "dd.MM.yyyy"
            } else {
                "dd MMM yyyy"
            }

        /** Swiss digit grouping with an apostrophe. */
        private fun group(value: Long): String {
            val digits = value.toString()
            if (digits.length <= GROUP_SIZE) return digits
            return digits
                .reversed()
                .chunked(GROUP_SIZE)
                .joinToString(GROUP_SEPARATOR)
                .reversed()
        }

        companion object {
            const val DEFAULT_CURRENCY = "CHF"
            private const val GROUP_SIZE = 3
            private const val GROUP_SEPARATOR = "'"
            private const val GERMAN = "de"
        }
    }
