package gg.cubix.adventurei18n.json;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonLangFileFormatTest {

    private final JsonLangFileFormat format = new JsonLangFileFormat();

    @Test
    void fileExtensionIsJson() {
        assertEquals("json", format.fileExtension());
    }

    @Test
    void parsesAFlatObject() {
        Map<String, String> result = parse("""
                {
                  "greeting": "Hello",
                  "farewell": "Bye"
                }
                """);

        assertEquals(Map.of("greeting", "Hello", "farewell", "Bye"), result);
    }

    @Test
    void decodesUtf8Content() {
        Map<String, String> result = parse("{\"greeting\": \"café ☃\"}");

        assertEquals("café ☃", result.get("greeting"));
    }

    @Test
    void rejectsNestedObjectValue() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> parse("{\"greeting\": {\"nested\": \"value\"}}"));

        assertTrue(thrown.getMessage().contains("greeting"));
        assertTrue(thrown.getMessage().contains("nested object"));
    }

    @Test
    void rejectsArrayValue() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> parse("{\"greeting\": [\"a\", \"b\"]}"));

        assertTrue(thrown.getMessage().contains("greeting"));
        assertTrue(thrown.getMessage().contains("array"));
    }

    @Test
    void rejectsNumberValue() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> parse("{\"count\": 5}"));

        assertTrue(thrown.getMessage().contains("count"));
        assertTrue(thrown.getMessage().contains("number"));
    }

    @Test
    void rejectsBooleanValue() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> parse("{\"flag\": true}"));

        assertTrue(thrown.getMessage().contains("flag"));
        assertTrue(thrown.getMessage().contains("boolean"));
    }

    @Test
    void rejectsNullValue() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> parse("{\"greeting\": null}"));

        assertTrue(thrown.getMessage().contains("greeting"));
        assertTrue(thrown.getMessage().contains("null"));
    }

    @Test
    void rejectsNonObjectTopLevel() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> parse("[\"a\", \"b\"]"));

        assertTrue(thrown.getMessage().contains("not a flat JSON object"));
    }

    @Test
    void rejectsMalformedJsonSyntax() {
        InputStream in = new ByteArrayInputStream("{not valid json".getBytes(StandardCharsets.UTF_8));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> format.parse(in, "broken.json"));
        assertTrue(thrown.getMessage().contains("broken.json"));
    }

    /**
     * A failing stream doesn't surface as a raw {@code IOException} - but Gson itself catches the
     * read failure during {@code JsonParser.parseReader(...)} and wraps it as its own {@code
     * JsonIOException} (a {@code JsonParseException}) before this class ever sees a bare {@code
     * IOException}, so this lands in the {@code JsonParseException} handling, not
     * {@link UncheckedIOException} - that path exists for a failure while *closing* the reader
     * instead, a narrower case this fake stream doesn't exercise.
     */
    @Test
    void wrapsStreamReadFailureAsIllegalStateException() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws java.io.IOException {
                throw new java.io.IOException("boom");
            }

            @Override
            public int read(byte[] b, int off, int len) throws java.io.IOException {
                throw new java.io.IOException("boom");
            }
        };

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> format.parse(failing, "broken-stream.json"));
        assertTrue(thrown.getMessage().contains("broken-stream.json"));
    }

    private Map<String, String> parse(String content) {
        InputStream in = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        return format.parse(in, "test.json");
    }
}
