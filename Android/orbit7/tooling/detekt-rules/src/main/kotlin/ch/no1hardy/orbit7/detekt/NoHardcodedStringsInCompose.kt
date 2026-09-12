package ch.no1hardy.orbit7.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Hard rule: no hardcoded user-visible strings, from the first commit.
 *
 * Every string a user can read comes from resources, because the app ships de-CH and English and a
 * literal in a composable is a translation that will never happen. Test tags and other technical
 * identifiers are exempt — they are deliberately *not* user-visible.
 */
class NoHardcodedStringsInCompose(
    config: Config,
) : Rule(config) {
    override val issue =
        Issue(
            id = "NoHardcodedStringsInCompose",
            severity = Severity.Maintainability,
            description = "User-visible strings in composables must come from string resources.",
            debt = Debt.FIVE_MINS,
        )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        val isComposable = function.annotationEntries.any { it.shortName?.asString() == "Composable" }
        val isPreview = function.annotationEntries.any { it.shortName?.asString() == "Preview" }
        if (!isComposable || isPreview) return
        if (function.containingKtFile.virtualFilePath.contains("/test/")) return

        function
            .collectDescendantsOfType<KtStringTemplateExpression>()
            .filter { it.text.trim('"').isNotBlank() }
            .filterNot { literal -> EXEMPT_ARGUMENT_NAMES.any { literal.isArgumentNamed(it) } }
            .forEach { literal ->
                report(
                    CodeSmell(
                        issue,
                        Entity.from(literal),
                        "String literal ${literal.text} in composable `${function.name}`. " +
                            "Use `stringResource(R.string.…)`.",
                    ),
                )
            }
    }

    private fun KtStringTemplateExpression.isArgumentNamed(name: String): Boolean =
        parent?.text?.startsWith("$name =") == true ||
            parent?.parent?.text?.contains("$name = ${this.text}") == true

    private companion object {
        val EXEMPT_ARGUMENT_NAMES = listOf("testTag", "tag", "key", "route", "moduleKey", "colorToken")
    }
}
