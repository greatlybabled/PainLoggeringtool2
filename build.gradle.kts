// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Expose these plugins from the version catalog (but don't apply them here)
    // These aliases must match the [plugins] section in libs.versions.toml
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android.gradle) apply false // Corrected Hilt Gradle plugin alias
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false // Added parcelize plugin from TOML
    alias(libs.plugins.org.jetbrains.kotlin.plugin.compose) apply false // Added Compose plugin from TOML
}

// Configure all projects with common settings
subprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        // Migrate from deprecated kotlinOptions to compilerOptions
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

tasks.register("clean", Delete::class) {
    // Use layout.buildDirectory for accessing build directory (replaces deprecated buildDir)
    delete(layout.buildDirectory)
}


