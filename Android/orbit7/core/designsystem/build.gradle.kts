plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "ch.no1hardy.orbit7.core.designsystem"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        // Accessibility is a build failure, not a warning (docs/06-test-strategy.md §2).
        error += listOf("ContentDescription", "TouchTargetSizeCheck", "SetTextI18n", "HardcodedText")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    api(project(":core:domain"))

    implementation(platform(libs.compose.bom))
    api(libs.compose.foundation)
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.ui.text)
    api(libs.compose.material3)
    api(libs.compose.material.icons.core)
    api(libs.androidx.lifecycle.runtime.compose)
    debugApi(libs.compose.ui.tooling)
    api(libs.compose.ui.tooling.preview)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit4)
    testImplementation(libs.kotest.assertions)
}
