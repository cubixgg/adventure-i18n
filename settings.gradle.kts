// Not in gradle/libs.versions.toml: the catalog isn't available yet this early in the settings
// script's own plugins {} block, only in subprojects' build.gradle.kts. Lets Gradle auto-provision
// the Java 25 toolchain adventure-i18n-minestom-demo needs (see its build.gradle.kts) without
// requiring it preinstalled.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "adventure-i18n"

include("adventure-i18n-core", "adventure-i18n-json", "adventure-i18n-minestom-demo")
