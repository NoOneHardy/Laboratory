plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Test infrastructure, built in P0 before any feature exists (docs/06-test-strategy.md §9).
// It stays a pure JVM module so that :core:domain's own tests and every Android module's tests can
// share exactly the same fakes and builders.
kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core:domain"))
    api(testFixtures(project(":core:domain")))
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    implementation(libs.kotlinx.coroutines.core)
}
