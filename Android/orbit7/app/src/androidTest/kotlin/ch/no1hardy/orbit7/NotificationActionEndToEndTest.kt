package ch.no1hardy.orbit7

import ch.no1hardy.orbit7.core.domain.economy.SettlementCalculator
import ch.no1hardy.orbit7.core.domain.economy.SettlementInput
import ch.no1hardy.orbit7.core.domain.model.CategoryKey
import ch.no1hardy.orbit7.core.domain.model.ZeroSpendSource
import ch.no1hardy.orbit7.core.domain.money.Money
import ch.no1hardy.orbit7.core.domain.repository.BudgetRepository
import ch.no1hardy.orbit7.core.domain.repository.CategoryRepository
import ch.no1hardy.orbit7.core.domain.repository.ZeroSpendRepository
import ch.no1hardy.orbit7.core.domain.test.FakeClock
import ch.no1hardy.orbit7.core.domain.time.Week
import ch.no1hardy.orbit7.core.domain.usecase.MarkZeroSpendDayUseCase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import javax.inject.Inject

/**
 * Journey 5: the notification's "Nothing spent today" action writes a zero-spend mark without
 * launching the app, and that mark raises the confidence factor at settlement.
 *
 * This single tap is the cheapest possible way to produce a signal day, which is what keeps the
 * pure-trust model honest.
 */
@HiltAndroidTest
class NotificationActionEndToEndTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var clock: FakeClock

    @Inject lateinit var markZeroSpendDay: MarkZeroSpendDayUseCase

    @Inject lateinit var zeroSpend: ZeroSpendRepository

    @Inject lateinit var categories: CategoryRepository

    @Inject lateinit var budgets: BudgetRepository

    @Inject lateinit var calculator: SettlementCalculator

    @Before
    fun setUp() = hiltRule.inject()

    @Test
    fun zeroSpendFromNotification() =
        runBlocking {
            val monday = LocalDate.of(2025, 4, 7)
            val seeded = categories.seedDefaults(listOf(CategoryKey.GROCERIES))
            budgets.setMonthlyBudget(seeded.single().id, Money.ofMajor(600), monday)

            // Seven taps on the notification, one per day — the app is never opened.
            (0..6).forEach { offset ->
                clock.setDate(monday.plusDays(offset.toLong()))
                markZeroSpendDay(source = ZeroSpendSource.NOTIFICATION).shouldBeTrue()
            }

            val marks = zeroSpend.between(monday, monday.plusDays(6))
            marks.size shouldBe 7
            marks.all { it.source == ZeroSpendSource.NOTIFICATION }.shouldBeTrue()

            val week = Week(monday)
            val logged =
                calculator
                    .settle(
                        SettlementInput(
                            week = week,
                            budgets = budgets.allVersions(),
                            expenses = emptyList(),
                            zeroSpendDates = marks.map { it.date }.toSet(),
                            consecutiveGoodWeeksBefore = 0,
                            settledAt = clock.instant(),
                        ),
                    ).settlement
            val silent =
                calculator
                    .settle(
                        SettlementInput(
                            week = week,
                            budgets = budgets.allVersions(),
                            expenses = emptyList(),
                            zeroSpendDates = emptySet(),
                            consecutiveGoodWeeksBefore = 0,
                            settledAt = clock.instant(),
                        ),
                    ).settlement

            logged.confidenceBasisPoints shouldBe 10_000
            silent.confidenceBasisPoints shouldBe 4_000
            logged.awardedEp shouldBeGreaterThan silent.awardedEp
        }
}
