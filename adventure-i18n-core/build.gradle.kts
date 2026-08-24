plugins {
    `java-library`
}

description = "Locale discovery, fallback resolution and MiniMessage-based translation for Adventure - the generic core, no JSON dependency."

dependencies {
    api(libs.adventure.api)
    api(libs.adventure.text.minimessage)
    implementation(libs.slf4j.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
