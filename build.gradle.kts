// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}

// Force Gradle to use a specific Java Toolchain (Pre-installed JDK 17)
// This fixes the "bootstrap class path not set" and "missing compiler" errors
// by ensuring we use a full JDK, not just a JRE.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
}