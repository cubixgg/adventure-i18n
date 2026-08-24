package gg.cubix.adventurei18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Default {@link LangFileFormat}: flat {@code key -> value} pairs via {@link java.util.Properties},
 * with no third-party dependency.
 *
 * <p><b>Encoding:</b> the stream is always decoded as UTF-8, explicitly - {@code
 * Properties#load(InputStream)} itself still defaults to ISO-8859-1 (only {@code
 * PropertyResourceBundle}, via {@code ResourceBundle.getBundle}, defaults to UTF-8 since Java 9,
 * and this class doesn't go through that machinery), so this format wraps the stream in a UTF-8
 * {@link InputStreamReader} before calling {@link Properties#load(Reader)} instead of relying on
 * that default.
 *
 * <p><b>Escaping:</b> only the *first* unescaped {@code =}, {@code :}, or whitespace character
 * separates a key from its value - everything after it, including further {@code :} or
 * {@code <}/{@code >} characters, stays part of the value unchanged. A line like
 * {@code key = <error>Score: <points></error>} therefore parses exactly as written. Escaping only
 * matters for two things in the value: a literal backslash needs doubling ({@code \\}), and
 * leading whitespace needs an escaping {@code \} (e.g. {@code key = \ padded}) or it's trimmed -
 * both inherited directly from {@link Properties#load(Reader)}'s own format.
 */
public final class PropertiesLangFileFormat implements LangFileFormat {

    @Override
    public String fileExtension() {
        return "properties";
    }

    @Override
    public Map<String, String> parse(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(sourceName, "sourceName");

        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to parse '" + sourceName + "' as a properties lang file", e);
        }

        Map<String, String> translations = new LinkedHashMap<>(properties.size());
        for (String key : properties.stringPropertyNames()) {
            translations.put(key, properties.getProperty(key));
        }
        return translations;
    }
}
