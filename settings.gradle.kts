@file:Suppress("UnstableApiUsage")

pluginManagement {
    plugins {
        id("com.android.application") version "8.13.2"
        id("com.android.library") version "8.13.2"
        id("org.jetbrains.kotlin.android") version "1.9.22"
        id("com.google.gms.google-services") version "4.4.1"
        id("com.google.devtools.ksp") version "1.9.22-1.0.17"
    }
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "QR-Magic"
include(":app")
