package gg.cubix.adventurei18n.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import gg.cubix.adventurei18n.LangFileFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Optional {@link LangFileFormat} for consumers who need JSON instead of the default
 * {@code .properties} format. Parses a flat JSON object ({@code key -> string}), always as UTF-8.
 *
 * <p>A nested object/array value, or a non-string primitive (number, boolean, {@code null}), is a
 * hard error - not a silently dropped or coerced key. A broken lang file must fail at boot, not
 * surface raw translation keys (or a stringified number/boolean) in front of players.
 */
public final class JsonLangFileFormat implements LangFileFormat {

    @Override
    public String fileExtension() {
        return "json";
    }

    @Override
    public Map<String, String> parse(InputStream in, String sourceName) {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(sourceName, "sourceName");

        JsonElement root;
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read '" + sourceName + "' as a JSON lang file", e);
        } catch (JsonParseException e) {
            throw new IllegalStateException("Failed to parse '" + sourceName + "' as JSON: " + e.getMessage(), e);
        }

        if (!root.isJsonObject()) {
            throw new IllegalStateException(
                    "'" + sourceName + "' is not a flat JSON object (key -> string) at the top level");
        }

        Map<String, String> translations = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IllegalStateException(
                        "'" + sourceName + "': value for key '" + key + "' is not a string (" + describe(value)
                                + ") - " + fileExtension() + " lang files must be flat key -> string");
            }

            translations.put(key, value.getAsString());
        }

        return translations;
    }

    private static String describe(JsonElement value) {
        if (value.isJsonNull()) {
            return "null";
        }
        if (value.isJsonObject()) {
            return "a nested object";
        }
        if (value.isJsonArray()) {
            return "an array";
        }
        return value.getAsJsonPrimitive().isBoolean() ? "a boolean" : "a number";
    }
}
