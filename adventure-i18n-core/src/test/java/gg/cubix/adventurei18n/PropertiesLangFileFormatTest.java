package gg.cubix.adventurei18n;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertiesLangFileFormatTest {

    private final PropertiesLangFileFormat format = new PropertiesLangFileFormat();

    @Test
    void fileExtensionIsProperties() {
        assertEquals("properties", format.fileExtension());
    }

    @Test
    void colonInValueStaysPartOfValue() {
        Map<String, String> result = parse("score = Your score: 10");

        assertEquals("Your score: 10", result.get("score"));
    }

    @Test
    void angleBracketTagsStayPartOfValue() {
        Map<String, String> result = parse("score = <error>Score: <points></error>");

        assertEquals("<error>Score: <points></error>", result.get("score"));
    }

    @Test
    void onlyFirstUnescapedSeparatorSplitsKeyFromValue() {
        Map<String, String> result = parse("greeting: Hello: World");

        assertEquals("Hello: World", result.get("greeting"));
    }

    @Test
    void doubledBackslashBecomesLiteralBackslash() {
        Map<String, String> result = parse("path = C:\\\\Users\\\\test");

        assertEquals("C:\\Users\\test", result.get("path"));
    }

    @Test
    void escapedLeadingWhitespaceIsPreserved() {
        Map<String, String> result = parse("padded = \\  two leading spaces");

        assertEquals("  two leading spaces", result.get("padded"));
    }

    @Test
    void unescapedLeadingWhitespaceInValueIsTrimmed() {
        Map<String, String> result = parse("padded =    trimmed");

        assertEquals("trimmed", result.get("padded"));
    }

    @Test
    void decodesUtf8Content() {
        InputStream in = new ByteArrayInputStream(
                "greeting = caf\u00e9 \u2603".getBytes(StandardCharsets.UTF_8));

        Map<String, String> result = format.parse(in, "test.properties");

        assertEquals("caf\u00e9 \u2603", result.get("greeting"));
    }

    @Test
    void parsesMultipleKeys() {
        Map<String, String> result = parse("a = 1\nb = 2\nc = 3");

        assertEquals(Map.of("a", "1", "b", "2", "c", "3"), result);
    }

    @Test
    void wrapsIoExceptionAsUncheckedIoException() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };

        UncheckedIOException thrown = assertThrows(UncheckedIOException.class,
                () -> format.parse(failing, "broken.properties"));
        assertTrue(thrown.getMessage().contains("broken.properties"));
    }

    private Map<String, String> parse(String content) {
        InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return format.parse(in, "test.properties");
    }
}
