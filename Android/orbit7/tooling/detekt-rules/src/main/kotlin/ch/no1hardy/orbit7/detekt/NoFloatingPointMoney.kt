package ch.no1hardy.orbit7.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.config
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Hard rule 2: money is `Long` minor units (Rappen) end to end.
 *
 * `Double` and `Float` may not appear in the domain or the data layer at all. The economy's only
 * legitimate non-integer values (confidence and streak factors) are modelled as [java.math.BigDecimal]
 * -free integer basis points, so there is no exception to carve out here.
 */
class NoFloatingPointMoney(
    config: Config,
) : Rule(config) {
    override val issue =
        Issue(
            id = "NoFloatingPointMoney",
            severity = Severity.Defect,
            description = "Floating point arithmetic is forbidden in :core:domain and :core:data.",
            debt = Debt.TWENTY_MINS,
        )

    private val restrictedPathFragments: List<String> by config(listOf("core/domain", "core/data"))

    override fun visitTypeReference(typeReference: KtTypeReference) {
        super.visitTypeReference(typeReference)
        val path = typeReference.containingKtFile.virtualFilePath.replace('\\', '/')
        if (restrictedPathFragments.none { it in path }) return

        val name = typeReference.text.removeSuffix("?")
        if (name == "Double" || name == "Float") {
            report(
                CodeSmell(
                    issue,
                    Entity.from(typeReference),
                    "`$name` is not allowed here. Money is `Long` minor units; factors are integer " +
                        "basis points (see GameBalance).",
                ),
            )
        }
    }
}
