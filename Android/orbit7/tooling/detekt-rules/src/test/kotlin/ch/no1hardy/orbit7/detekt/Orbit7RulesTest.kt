package ch.no1hardy.orbit7.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.kotest.matchers.collections.shouldHaveSize
import org.junit.jupiter.api.Test

/**
 * The custom rules that enforce the four hard rules have tests of their own — a lint rule that does
 * not fire is worse than no lint rule, because it is believed.
 */
class Orbit7RulesTest {
    @Test
    fun `direct clock access is reported`() {
        val rule = NoDirectClockAccess(Config.empty)

        rule.compileAndLint(
            """
            import java.time.LocalDate
            fun today() = LocalDate.now()
            """.trimIndent(),
        ) shouldHaveSize 1
    }

    @Test
    fun `an injected clock is fine`() {
        val rule = NoDirectClockAccess(Config.empty)

        rule.compileAndLint(
            """
            import java.time.Clock
            import java.time.LocalDate
            fun today(clock: Clock) = LocalDate.now(clock)
            """.trimIndent(),
        ) shouldHaveSize 0
    }

    @Test
    fun `an empty catch block is reported`() {
        val rule = NoEmptyCatch(Config.empty)

        rule.compileAndLint(
            """
            fun risky() {
                try {
                    error("boom")
                } catch (failure: IllegalStateException) {
                }
            }
            """.trimIndent(),
        ) shouldHaveSize 1
    }

    @Test
    fun `a catch block with an explanation is allowed`() {
        val rule = NoEmptyCatch(Config.empty)

        rule.compileAndLint(
            """
            fun risky() {
                try {
                    error("boom")
                } catch (failure: IllegalStateException) {
                    // Expected: the week is already settled, and settlement is idempotent.
                }
            }
            """.trimIndent(),
        ) shouldHaveSize 0
    }

    @Test
    fun `a hardcoded string in a composable is reported`() {
        val rule = NoHardcodedStringsInCompose(Config.empty)

        rule.compileAndLint(
            """
            annotation class Composable
            @Composable
            fun Greeting() {
                Text("Hello")
            }
            fun Text(value: String) = value
            """.trimIndent(),
        ) shouldHaveSize 1
    }
}
