plugins {
    application
    alias(libs.plugins.shadow)
}

description = "Runnable Minestom server demonstrating adventure-i18n end to end - not published, " +
        "not part of the library's public API."

// Minestom requires Java 25; the rest of this repository doesn't pin a toolchain yet (see
// roadmap.md section 1), so this module pins its own rather than forcing it repo-wide.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "gg.cubix.adventurei18n.minestomdemo.MinestomDemoServer"
}

dependencies {
    implementation(project(":adventure-i18n-core"))
    implementation(project(":adventure-i18n-json"))
    implementation(libs.minestom)
    // core/json only expose slf4j-api as `implementation`, not `api` (see their build.gradle.kts) -
    // this module logs directly (DemoTranslationIssues, MinestomDemoServer), so it needs it too.
    implementation(libs.slf4j.api)
    // An app, unlike a library, should ship a concrete SLF4J binding rather than leave it to a
    // consumer - otherwise every log line (including LoggingTranslationIssueListener's default,
    // if it were used here) silently goes nowhere.
    runtimeOnly(libs.slf4j.simple)
}

tasks.shadowJar {
    // Keep shadow's default "-all" classifier rather than overwriting the plain `jar` task's own
    // output path - `application`'s startScripts/distZip/distTar already consume the plain jar
    // implicitly, and colliding paths turn into an implicit, unordered task dependency.
    // Several dependencies (adventure's own serializers, slf4j) ship META-INF/services files under
    // the same path - INCLUDE (rather than shadow's default EXCLUDE) is what lets
    // mergeServiceFiles() actually see every duplicate to merge, instead of only the first one.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}
