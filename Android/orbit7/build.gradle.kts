plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.paparazzi) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
}

// Coverage gates from docs/06-test-strategy.md: >= 90% on :core:domain, >= 70% on :core:data.
dependencies {
    kover(project(":core:domain"))
    kover(project(":core:data"))
}

kover {
    reports {
        verify {
            rule("Line coverage of the game economy") {
                bound {
                    minValue.set(90)
                    coverageUnits.set(kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE)
                }
                filters.includes.classes("ch.no1hardy.orbit7.core.domain.*")
            }
            rule("Line coverage of the data layer") {
                bound {
                    minValue.set(70)
                    coverageUnits.set(kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE)
                }
                filters.includes.classes("ch.no1hardy.orbit7.core.data.*")
            }
        }
    }
}

allprojects {
    apply(
        plugin =
            rootProject.libs.plugins.spotless
                .get()
                .pluginId,
    )

    spotless {
        kotlin {
            target("src/**/*.kt")
            targetExclude("**/build/**")
            ktlint(
                rootProject.libs.versions.kotlin
                    .get()
                    .let { "1.5.0" },
            ).editorConfigOverride(
                mapOf(
                    "ktlint_standard_function-naming" to "disabled", // @Composable functions are PascalCase
                    "ktlint_standard_property-naming" to "disabled",
                    "max_line_length" to "120",
                ),
            )
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint("1.5.0")
        }
    }
}

subprojects {
    apply(
        plugin =
            rootProject.libs.plugins.detekt
                .get()
                .pluginId,
    )

    detekt {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        parallel = true
    }

    dependencies {
        add("detektPlugins", rootProject.libs.detekt.formatting)
        // The four hard rules from docs/05-architecture.md §5, enforced at build time.
        if (path != ":tooling:detekt-rules") {
            add("detektPlugins", project(":tooling:detekt-rules"))
        }
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "21"
        reports {
            html.required.set(true)
            sarif.required.set(true)
        }
    }
}
