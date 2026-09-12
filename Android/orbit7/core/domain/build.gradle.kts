plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
    `java-test-fixtures`
}

// :core:domain is a pure Kotlin/JVM module with zero Android dependencies, by design
// (docs/05-architecture.md §2). It contains the entire game economy, and it is impossible to reach
// for a Context or System.currentTimeMillis() here because neither compiles.
kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    api(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)

    // Fakes and builders live here rather than in :core:testing so that the domain's own tests can
    // use them without a project dependency cycle. :core:testing re-exports them to every other
    // module (docs/06-test-strategy.md §9).
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesApi(libs.javax.inject)

    testImplementation(testFixtures(project(":core:domain")))
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.test {
    useJUnitPlatform()
}
