package gg.cubix.adventurei18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A {@link LangSource} that discovers lang bundles by scanning a directory on the classpath -
 * {@code ClassLoader#getResources(directory)} returns one URL per classpath root that contains
 * that directory, and each root is scanned in whichever shape it actually has: an exploded
 * directory ({@code file:} URL, typical for an IDE run) or a jar ({@code jar:file:...!/lang}).
 *
 * <p>A file directly inside the scanned directory that doesn't match {@code
 * <localeId>.<extension>} (wrong extension, or {@link LocaleCodes#parse} rejects the identifier) is
 * skipped and logged - not a boot abort, since the directory may well contain something like a
 * stray {@code README.md}. Nested subdirectories are ignored entirely, without logging: scanning is
 * not recursive.
 *
 * <p>Which regional variant represents a language when several are discovered (e.g. {@code de_DE}
 * for a client on {@code de_AT}) is deliberately not decided here - that's an explicit rule of
 * {@code FallbackStrategy}, never an accident of scan order.
 */
public final class ClasspathLangSource implements LangSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathLangSource.class);

    private final ClassLoader loader;
    private final String directory;
    private final LangFileFormat format;

    private ClasspathLangSource(ClassLoader loader, String directory, LangFileFormat format) {
        this.loader = loader;
        this.directory = directory;
        this.format = format;
    }

    /**
     * Scans {@code directory} for {@code .properties} lang files, using this class's own
     * {@link ClassLoader}. Use the explicit overload instead if the lang files live in a jar
     * loaded by a different classloader than this library's own classes.
     */
    public static ClasspathLangSource scanning(String directory) {
        return scanning(ClasspathLangSource.class.getClassLoader(), directory, new PropertiesLangFileFormat());
    }

    /**
     * Scans {@code directory}, visible to {@code loader}, for files matching
     * {@code <localeId>.<format.fileExtension()>}.
     */
    public static ClasspathLangSource scanning(ClassLoader loader, String directory, LangFileFormat format) {
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(format, "format");
        return new ClasspathLangSource(loader, directory, format);
    }

    @Override
    public Map<Locale, Map<String, String>> load() {
        Map<Locale, Map<String, String>> bundles = new LinkedHashMap<>();
        try {
            Enumeration<URL> roots = loader.getResources(directory);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                switch (root.getProtocol()) {
                    case "file" -> scanFileDirectory(toFile(root), bundles);
                    case "jar" -> scanJar(root, bundles);
                    default -> LOGGER.warn(
                            "Ignoring lang directory '{}' at {} - unsupported classpath URL protocol '{}'",
                            directory, root, root.getProtocol());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to scan classpath for lang directory '" + directory + "'", e);
        }
        return bundles;
    }

    private void scanFileDirectory(File directory, Map<Locale, Map<String, String>> bundles) {
        File[] entries = directory.listFiles();
        if (entries == null) {
            return;
        }

        for (File entry : entries) {
            if (!entry.isFile()) {
                continue;
            }

            Locale locale = matchLocale(entry.getName());
            if (locale == null) {
                logSkipped(entry.getPath());
                continue;
            }

            try (InputStream in = new FileInputStream(entry)) {
                merge(bundles, locale, format.parse(in, entry.getPath()));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read lang file '" + entry.getPath() + "'", e);
            }
        }
    }

    private void scanJar(URL root, Map<Locale, Map<String, String>> bundles) throws IOException {
        JarURLConnection connection = (JarURLConnection) root.openConnection();
        // Without this, JarURLConnection#getJarFile() returns a JVM-wide cached JarFile shared
        // with every other "jar:" URL for the same underlying file - closing it here (via the
        // try-with-resources below) would then break any other code in the same JVM still reading
        // from that jar ("Zip file closed"). Disabling the cache gives this call its own instance.
        connection.setUseCaches(false);

        String prefix = directory.endsWith("/") ? directory : directory + "/";

        try (JarFile jarFile = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith(prefix)) {
                    continue;
                }

                String name = entry.getName().substring(prefix.length());
                if (name.isEmpty() || name.contains("/")) {
                    // The directory entry itself, or a file nested in a subdirectory - scanning
                    // isn't recursive, so neither is a candidate.
                    continue;
                }

                Locale locale = matchLocale(name);
                if (locale == null) {
                    logSkipped(entry.getName());
                    continue;
                }

                try (InputStream in = jarFile.getInputStream(entry)) {
                    merge(bundles, locale, format.parse(in, entry.getName()));
                }
            }
        }
    }

    private Locale matchLocale(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }

        String extension = fileName.substring(dot + 1);
        if (!extension.equalsIgnoreCase(format.fileExtension())) {
            return null;
        }

        return LocaleCodes.parse(fileName.substring(0, dot));
    }

    private void logSkipped(String source) {
        LOGGER.info(
                "Skipping '{}' in lang directory '{}' - not a <localeId>.{} file",
                source, directory, format.fileExtension());
    }

    private static void merge(Map<Locale, Map<String, String>> bundles, Locale locale, Map<String, String> translations) {
        bundles.computeIfAbsent(locale, l -> new LinkedHashMap<>()).putAll(translations);
    }

    private static File toFile(URL url) {
        try {
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            return new File(url.getPath());
        }
    }
}
