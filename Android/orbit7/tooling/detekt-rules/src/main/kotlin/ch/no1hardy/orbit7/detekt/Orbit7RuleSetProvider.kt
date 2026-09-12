package ch.no1hardy.orbit7.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/**
 * The rule set that enforces the four hard rules from `docs/05-architecture.md` §5.
 *
 * These are the rules that make the economy testable and trustworthy, so they are enforced by
 * tooling rather than by review discipline.
 */
class Orbit7RuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "orbit7"

    override fun instance(config: Config): RuleSet =
        RuleSet(
            ruleSetId,
            listOf(
                NoDirectClockAccess(config),
                NoFloatingPointMoney(config),
                NoHardcodedStringsInCompose(config),
                NoEmptyCatch(config),
            ),
        )
}
