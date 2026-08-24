package gg.cubix.adventurei18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClasspathLangSourceTest {

    /**
     * src/test/resources/lang/ contains en_us.properties, de_de.properties, a stray README.md
     * (wrong extension), notalocale.properties (right extension, but the identifier is too long
     * to be a valid locale id), and sub/ignored.properties (a subdirectory - scanning isn't
     * recursive). Gradle puts test resources on the test classpath as an exploded directory, so
     * this exercises the file: URL scanning path.
     */
    @Test
    void scansExplodedResourceDirectoryIgnoringNonMatchingEntries() {
        LangSource source = ClasspathLangSource.scanning("lang");

        Map<Locale, Map<String, String>> bundles = source.load();

        assertEquals(2, bundles.size());
        assertEquals("Hello", bundles.get(Locale.of("en", "US")).get("greeting"));
        assertEquals("Hallo", bundles.get(Locale.of("de", "DE")).get("greeting"));
    }

    @Test
    void scansARealBuiltJarIgnoringNonMatchingEntries(@TempDir Path tempDir) throws IOException {
        Path jarPath = tempDir.resolve("fixture.jar");
        writeJar(jarPath, Map.of(
                "lang/en_us.properties", "greeting = Hello from jar",
                "lang/fr_fr.properties", "greeting = Bonjour depuis le jar",
                "lang/README.md", "not a lang file",
                "lang/notalocale.properties", "greeting = identifier too long to be a locale id",
                "lang/sub/ignored.properties", "greeting = nested, scanning isn't recursive"
        ));

        // Isolated (null parent) so this loader only ever sees the fixture jar's own "lang"
        // resources, never this module's own src/test/resources/lang.
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jarPath.toUri().toURL()}, null)) {
            LangSource source = ClasspathLangSource.scanning(loader, "lang", new PropertiesLangFileFormat());

            Map<Locale, Map<String, String>> bundles = source.load();

            assertEquals(2, bundles.size());
            assertEquals("Hello from jar", bundles.get(Locale.of("en", "US")).get("greeting"));
            assertEquals("Bonjour depuis le jar", bundles.get(Locale.of("fr", "FR")).get("greeting"));
        }
    }

    @Test
    void returnsEmptyMapWhenDirectoryDoesNotExistOnClasspath() {
        LangSource source = ClasspathLangSource.scanning("no-such-directory");

        Map<Locale, Map<String, String>> bundles = source.load();

        assertEquals(0, bundles.size());
        assertNull(bundles.get(Locale.US));
    }

    /**
     * Writes a jar the way a real build tool does: with an explicit directory entry for every
     * directory level, not just the file entries. A jar assembled without those (e.g. writing raw
     * file entries straight into a {@link JarOutputStream}, skipping directories) doesn't actually
     * resemble what {@code ClassLoader#getResources("lang")} finds in practice - {@code
     * jarFile.getEntry("lang")} needs that directory entry to exist to resolve at all, which real
     * jars (this module's own, verified with the JDK's {@code jar} tool) always have.
     */
    private static void writeJar(Path jarPath, Map<String, String> entries) throws IOException {
        Set<String> directories = new LinkedHashSet<>();
        for (String name : entries.keySet()) {
            for (int slash = name.indexOf('/'); slash >= 0; slash = name.indexOf('/', slash + 1)) {
                directories.add(name.substring(0, slash + 1));
            }
        }

        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            for (String directory : directories) {
                out.putNextEntry(new JarEntry(directory));
                out.closeEntry();
            }
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                out.putNextEntry(new JarEntry(entry.getKey()));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
    }
}
