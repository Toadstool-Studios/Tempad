enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "tempad"

pluginManagement {
    repositories {
        mavenLocal()
        maven(url = "https://maven.architectury.dev/")
        maven(url = "https://maven.neoforged.net/releases/")
        maven(url = "https://maven.resourcefulbees.com/repository/maven-public/")
        maven(url = "https://jitpack.io/")
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}