package ch.no1hardy.orbit7.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Hard rule 1: the `Clock` is injected everywhere.
 *
 * A single `LocalDate.now()` on a settlement path makes the economy untestable, and nobody notices
 * until a test is flaky at midnight.
 */
class NoDirectClockAccess(
    config: Config,
) : Rule(config) {
    override val issue =
        Issue(
            id = "NoDirectClockAccess",
            severity = Severity.Defect,
            description = "Reads the wall clock directly. Inject a java.time.Clock instead.",
            debt = Debt.TEN_MINS,
        )

    private val allowedFiles: List<String> by config(listOf("ClockModule.kt", "TimeModule.kt"))

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.containingKtFile.name in allowedFiles) return

        val text = expression.text.replace(" ", "")
        val offender = FORBIDDEN.firstOrNull { text.startsWith(it) || text.contains(".$it") }
        if (offender != null) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(expression),
                    "`$offender` reads the wall clock. Inject `java.time.Clock` and use " +
                        "`LocalDate.now(clock)` / `clock.instant()` so the value is testable.",
                ),
            )
        }
    }

    private companion object {
        val FORBIDDEN =
            listOf(
                "LocalDate.now()",
                "LocalDateTime.now()",
                "LocalTime.now()",
                "ZonedDateTime.now()",
                "OffsetDateTime.now()",
                "Instant.now()",
                "Year.now()",
                "YearMonth.now()",
                "System.currentTimeMillis()",
                "System.nanoTime()",
                "Calendar.getInstance()",
                "Date()",
            )
    }
}
