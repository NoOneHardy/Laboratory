package ch.no1hardy.orbit7.core.domain.economy

import ch.no1hardy.orbit7.core.domain.model.Cadence
import ch.no1hardy.orbit7.core.domain.model.ContractStatus
import ch.no1hardy.orbit7.core.domain.model.NoticePeriod
import ch.no1hardy.orbit7.core.domain.test.aContract
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ContractScheduleTest {
    private val schedule = ContractSchedule()

    @Test
    @DisplayName("three months' notice on a renewal of 31 March is 31 December, not 90 days")
    fun `three month notice from 31 March`() {
        schedule.earliestCancellationOn(
            nextChargeOn = LocalDate.of(2026, 3, 31),
            noticePeriod = NoticePeriod.THREE_MONTHS,
        ) shouldBe LocalDate.of(2025, 12, 31)
    }

    @Test
    fun `month arithmetic clamps to the shorter month`() {
        // Three months before 31 May is 28 February in a common year, 29 in a leap year.
        schedule.earliestCancellationOn(LocalDate.of(2025, 5, 31), NoticePeriod.THREE_MONTHS) shouldBe
            LocalDate.of(2025, 2, 28)
        schedule.earliestCancellationOn(LocalDate.of(2024, 5, 31), NoticePeriod.THREE_MONTHS) shouldBe
            LocalDate.of(2024, 2, 29)
    }

    @Test
    fun `a notice period in days is calendar-exact too`() {
        schedule.earliestCancellationOn(LocalDate.of(2025, 3, 1), NoticePeriod(days = 30)) shouldBe
            LocalDate.of(2025, 1, 30)
    }

    @Test
    fun `no notice period means the deadline is the renewal itself`() {
        schedule.earliestCancellationOn(LocalDate.of(2025, 7, 1), NoticePeriod.NONE) shouldBe
            LocalDate.of(2025, 7, 1)
    }

    @Test
    fun `the 30, 14 and 3 day thresholds each fire exactly once`() {
        val contract = aContract(nextChargeOn = LocalDate.of(2026, 1, 1), noticePeriod = NoticePeriod.THREE_MONTHS)
        // Deadline: 1 October 2025.
        val notified = mutableSetOf<Int>()

        schedule.dueReminderThreshold(contract, LocalDate.of(2025, 8, 20), notified).shouldBeNull()

        val thirty = schedule.dueReminderThreshold(contract, LocalDate.of(2025, 9, 1), notified)
        thirty shouldBe 30
        notified += 30

        schedule.dueReminderThreshold(contract, LocalDate.of(2025, 9, 5), notified).shouldBeNull()
        schedule.dueReminderThreshold(contract, LocalDate.of(2025, 9, 17), notified) shouldBe 14
        notified += 14
        schedule.dueReminderThreshold(contract, LocalDate.of(2025, 9, 28), notified) shouldBe 3
        notified += 3
        schedule.dueReminderThreshold(contract, LocalDate.of(2025, 9, 30), notified).shouldBeNull()
    }

    @Test
    fun `a passed deadline and an inactive contract never remind`() {
        val contract = aContract(nextChargeOn = LocalDate.of(2026, 1, 1))
        schedule.dueReminderThreshold(contract, LocalDate.of(2025, 10, 2), emptySet()).shouldBeNull()

        val cancelled = contract.copy(status = ContractStatus.CANCELLED)
        schedule.dueReminderThreshold(cancelled, LocalDate.of(2025, 9, 1), emptySet()).shouldBeNull()
    }

    @Test
    fun `urgency starts exactly 14 days before the deadline`() {
        val contract = aContract(nextChargeOn = LocalDate.of(2026, 1, 1)) // deadline 1 October 2025

        schedule.isUrgent(contract, LocalDate.of(2025, 9, 16)).shouldBeFalse()
        schedule.isUrgent(contract, LocalDate.of(2025, 9, 17)).shouldBeTrue()
        schedule.isUrgent(contract, LocalDate.of(2025, 10, 1)).shouldBeTrue()
        schedule.isUrgent(contract, LocalDate.of(2025, 10, 2)).shouldBeFalse()
    }

    @Test
    fun `a charge date in the past rolls forward by whole cadence periods`() {
        val contract =
            aContract(
                nextChargeOn = LocalDate.of(2025, 1, 3),
                cadence = Cadence.MONTHLY,
                noticePeriod = NoticePeriod.ONE_MONTH,
            )

        val rolled = schedule.rollForward(contract, LocalDate.of(2025, 4, 14))

        rolled.nextChargeOn shouldBe LocalDate.of(2025, 5, 3)
        rolled.earliestCancellationOn shouldBe LocalDate.of(2025, 4, 3)
    }

    @Test
    fun `a future charge date is left alone`() {
        val contract = aContract(nextChargeOn = LocalDate.of(2026, 1, 1))

        schedule.rollForward(contract, LocalDate.of(2025, 4, 14)) shouldBe contract
    }
}
