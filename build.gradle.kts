// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false

    id("com.google.gms.google-services") version "4.5.0" apply false
}

layout.buildDirectory.set(file("${System.getProperty("user.home")}/.gradle_builds/recipefinder/root"))
