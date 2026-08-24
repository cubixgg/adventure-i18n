plugins {
    `java-library`
}

description = "Optional JSON LangFileFormat for adventure-i18n-core, for consumers who need JSON instead of the default Properties format."

dependencies {
    // api, not implementation: JsonLangFileFormat implements core's LangFileFormat interface, so
    // core's types are part of this module's own public API surface.
    api(project(":adventure-i18n-core"))
    implementation(libs.gson)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
