package ch.no1hardy.orbit7.core.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Everything Settings owns (`docs/03-screens.md` §9). Persisted as typed preferences. */
data class AppSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val currency: String = "CHF",
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val rappenFirstInput: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val dailyReminderTime: LocalTime = LocalTime.of(20, 0),
    val contractRemindersEnabled: Boolean = true,
    val reduceEffects: Boolean = false,
    val soundEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    /** The last settlement the user has actually seen, so the summary is shown exactly once. */
    val lastAcknowledgedSettlementWeekId: String? = null,
    /** Per-category dismissal dates of baseline suggestions, for the 28-day cooldown. */
    val dismissedSuggestions: Map<Long, LocalDate> = emptyMap(),
    /** Contract id → thresholds (30/14/3) already notified, so each fires exactly once. */
    val notifiedDeadlines: Map<Long, Set<Int>> = emptyMap(),
    val lastAppUsageOn: LocalDate? = null,
)

enum class AppLanguage {
    SYSTEM,
    DE_CH,
    EN,
}
