// Parent build only - no sources of its own. Real modules are adventure-i18n-core and
// adventure-i18n-json; this file applies shared config to both.

subprojects {
    group = "gg.cubix.adventurei18n"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }

    // Deferred (plugins.withType, not an immediate components["java"] lookup) because this
    // subprojects {} closure runs before a subproject's own plugins {} block has necessarily
    // applied java-library yet.
    plugins.withType<JavaLibraryPlugin> {
        apply(plugin = "maven-publish")

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
            // No remote repository configured yet - the publishing target (Maven Central vs. a
            // self-hosted repository) is still an open decision. `publishToMavenLocal` already
            // works for both modules.
        }
    }
}
