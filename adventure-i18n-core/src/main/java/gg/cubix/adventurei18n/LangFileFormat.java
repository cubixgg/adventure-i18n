package gg.cubix.adventurei18n;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Parses a single lang file's raw bytes into its flat {@code key -> value} translations.
 * Deliberately small so a new format only ever needs one method implemented.
 *
 * <p>A read failure (malformed input, an encoding a specific format can't decode, ...) is reported
 * by throwing an unchecked exception (typically {@link UncheckedIOException}) rather than
 * returning a partial or empty map - a broken lang file should fail at boot, not surface raw
 * translation keys to players.
 */
public interface LangFileFormat {

    /**
     * The file extension (without the leading {@code .}) this format expects, e.g.
     * {@code "properties"} or {@code "json"}. Used by a {@code LangSource} to decide which files
     * in a scanned directory belong to this format.
     */
    String fileExtension();

    /**
     * Parses {@code in} into its flat {@code key -> value} translations. {@code sourceName} is a
     * human-readable identifier for the file being parsed (e.g. its path or classpath entry name),
     * used only to make a parse failure's exception message point at the right file - it plays no
     * role in the parsed content itself.
     */
    Map<String, String> parse(InputStream in, String sourceName);
}
