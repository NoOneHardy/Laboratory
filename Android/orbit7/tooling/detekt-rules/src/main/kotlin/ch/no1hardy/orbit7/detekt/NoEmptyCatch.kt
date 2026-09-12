package ch.no1hardy.orbit7.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCatchClause

/**
 * Hard rule: no silent catch blocks.
 *
 * This is a money app with no crash reporting by design, so a swallowed exception is invisible
 * forever.
 */
class NoEmptyCatch(
    config: Config,
) : Rule(config) {
    override val issue =
        Issue(
            id = "NoEmptyCatch",
            severity = Severity.Defect,
            description = "An empty catch block hides a failure that nobody will ever see.",
            debt = Debt.TEN_MINS,
        )

    override fun visitCatchSection(catchClause: KtCatchClause) {
        super.visitCatchSection(catchClause)
        val body = catchClause.catchBody
        val hasStatements = body?.children?.isNotEmpty() == true
        val hasComment = body?.text?.contains("//") == true || body?.text?.contains("/*") == true
        if (!hasStatements && !hasComment) {
            report(
                CodeSmell(
                    issue,
                    Entity.from(catchClause),
                    "Empty catch block. Handle the failure, map it into UI state, or at minimum " +
                        "explain in a comment why it is safe to ignore.",
                ),
            )
        }
    }
}
