// Parent build only - no sources of its own. Real modules are adventure-i18n-core and
// adventure-i18n-json; this file applies shared config to both.

subprojects {
    group = "gg.cubix.adventurei18n"
    version = "0.2.0" // x-release-please-version

    repositories {
        mavenCentral()
    }

    // Deferred (plugins.withType, not an immediate components["java"] lookup) because this
    // subprojects {} closure runs before a subproject's own plugins {} block has necessarily
    // applied java-library yet. This also doubles as the publishing scope: only
    // adventure-i18n-core and adventure-i18n-json apply java-library;
    // adventure-i18n-minestom-demo applies `application` instead (it's a runnable example, not a
    // consumable dependency, see its own README.md), so it never enters this block and is never
    // published - no separate exclusion set needed.
    plugins.withType<JavaLibraryPlugin> {
        apply(plugin = "maven-publish")

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
            repositories {
                // Self-hosted Reposilite, not Maven Central - see
                // docs/decisions/0005-reposilite-release-please.md for why.
                maven {
                    name = "reposilite"
                    url = uri("https://maven.cubix.gg/public-releases")
                    credentials {
                        username = System.getenv("REPOSILITE_USERNAME")
                        password = System.getenv("REPOSILITE_PASSWORD")
                    }
                }
            }
        }
    }
}
