package ch.no1hardy.orbit7.core.designsystem

import ch.no1hardy.orbit7.core.designsystem.format.MoneyFormatter
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.test.chf
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class MoneyFormatterTest {
    private val formatter = MoneyFormatter()

    @Test
    fun `Swiss grouping uses an apostrophe and a period decimal separator`() {
        formatter.format(chf(1_234, 50)) shouldBe "CHF 1'234.50"
        formatter.format(chf(12, 50)) shouldBe "CHF 12.50"
        formatter.format(chf(1_234_567, 89)) shouldBe "CHF 1'234'567.89"
    }

    @Test
    fun `Rappen are always two digits`() {
        formatter.format(chf(5, 5)) shouldBe "CHF 5.05"
        formatter.format(chf(5)) shouldBe "CHF 5.00"
        formatter.format(Money.ZERO) shouldBe "CHF 0.00"
    }

    @Test
    fun `a negative amount uses a true minus sign, which aligns with digits`() {
        formatter.format(Money(-1_250)) shouldBe "CHF −12.50"
    }

    @Test
    fun `a positive delta can be shown with its sign`() {
        formatter.format(chf(12, 50), withSign = true) shouldBe "CHF +12.50"
    }

    @Test
    fun `energy is grouped the same way`() {
        formatter.formatEnergy(1_340) shouldBe "1'340 EP"
        formatter.formatEnergy(20, withSign = true) shouldBe "+20 EP"
        formatter.formatEnergy(-7) shouldBe "−7 EP"
    }

    @Test
    fun `dates are short and Swiss in German, and readable in English`() {
        val date = LocalDate.of(2025, 4, 14)

        formatter.formatShortDate(date, Locale.forLanguageTag("de-CH")) shouldBe "Mo., 14.04."
        formatter.formatLongDate(date, Locale.forLanguageTag("de-CH")) shouldBe "14.04.2025"
    }
}
