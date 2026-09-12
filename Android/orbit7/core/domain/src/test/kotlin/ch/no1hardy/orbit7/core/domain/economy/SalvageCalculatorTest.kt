package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.model.Cadence
import ch.no1hardy.orbit7.core.domain.test.aContract
import ch.no1hardy.orbit7.core.domain.test.chf
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SalvageCalculatorTest {
    private val calculator = SalvageCalculator()

    @Test
    @DisplayName("cancelling a monthly contract pays six months of it")
    fun `monthly cancellation`() {
        val contract = aContract(amount = chf(45), cadence = Cadence.MONTHLY)

        calculator.cancellationPayout(contract) shouldBe 270 // 6 × 45
    }

    @Test
    fun `a quarterly contract is normalised to a monthly equivalent first`() {
        val contract = aContract(amount = chf(150), cadence = Cadence.QUARTERLY)

        contract.monthlyEquivalent shouldBe chf(50)
        calculator.cancellationPayout(contract) shouldBe 300 // 6 × 50
    }

    @Test
    fun `a yearly contract is normalised to a monthly equivalent first`() {
        val contract = aContract(amount = chf(360), cadence = Cadence.YEARLY)

        contract.monthlyEquivalent shouldBe chf(30)
        calculator.cancellationPayout(contract) shouldBe 180 // 6 × 30
    }

    @Test
    fun `an odd yearly amount rounds the monthly equivalent half-up, once`() {
        val contract = aContract(amount = chf(100), cadence = Cadence.YEARLY)

        contract.monthlyEquivalent shouldBe chf(8, 33) // 100 / 12 = 8.3333…
        calculator.cancellationPayout(contract) shouldBe 50 // 6 × 8.33 = 49.98 → 50 EP
    }

    @Test
    @DisplayName("renegotiation pays on the delta only, never on the whole contract")
    fun `renegotiation pays the delta`() {
        calculator.renegotiationPayout(
            previousAmount = chf(89),
            newAmount = chf(39),
            cadence = Cadence.MONTHLY,
        ) shouldBe 300 // 6 × (89 − 39)
    }

    @Test
    fun `a renegotiation that saves nothing pays nothing`() {
        calculator.renegotiationPayout(chf(50), chf(50), Cadence.MONTHLY) shouldBe 0
        calculator.renegotiationPayout(chf(50), chf(70), Cadence.MONTHLY) shouldBe 0
    }

    @Test
    fun `the grid's monthly bleed counts only active drains`() {
        val contracts =
            listOf(
                aContract(id = 1, amount = chf(45), cadence = Cadence.MONTHLY),
                aContract(id = 2, amount = chf(150), cadence = Cadence.QUARTERLY),
                aContract(
                    id = 3,
                    amount = chf(19, 90),
                    status = ch.no1hardy.orbit7.core.domain.model.ContractStatus.CANCELLED,
                ),
            )

        calculator.monthlyBleedEp(contracts) shouldBe 95 // 45 + 50, the cancelled one no longer bleeds
    }
}
