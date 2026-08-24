package gg.cubix.adventurei18n;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Parses and renders locale identifiers in the Minecraft-style {@code en_us} form ({@link Locale}
 * itself prints {@code en_US}). There is no bundled list of known locales here - any id matching
 * the expected shape parses, valid or not; which locales actually exist is decided entirely by a
 * {@code LangSource}, not by this class.
 */
public final class LocaleCodes {

    // Deliberately permissive about the second segment's separator (accepts both "_" and "-") and
    // content (letters or digits, e.g. a UN M49 numeric region code like "419") - the point is
    // rejecting garbage shapes (arbitrary/hostile input, e.g. a player-supplied /lang argument),
    // not enforcing that the region is a real ISO 3166 code.
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z]{2,8}([_-][A-Za-z0-9]{2,8})?");

    private LocaleCodes() {
    }

    /**
     * Parses a Minecraft-style locale id such as {@code en_us} into a {@link Locale}. Returns
     * {@code null} - never throws - when {@code id} is {@code null} or doesn't match the expected
     * shape, so that arbitrary/hostile input (e.g. a player-supplied {@code /lang} argument) can't
     * silently produce a {@link Locale} that then resolves to nothing.
     */
    public static Locale parse(String id) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            return null;
        }

        int separator = separatorIndex(id);
        if (separator < 0) {
            return Locale.of(id.toLowerCase(Locale.ROOT));
        }

        String language = id.substring(0, separator).toLowerCase(Locale.ROOT);
        String country = id.substring(separator + 1).toUpperCase(Locale.ROOT);
        return Locale.of(language, country);
    }

    /**
     * Renders a {@link Locale} in the Minecraft-style {@code en_us} form: lowercase language,
     * lowercase country joined by {@code _}, or just the language if the locale has no country.
     */
    public static String id(Locale locale) {
        Objects.requireNonNull(locale, "locale");

        String language = locale.getLanguage().toLowerCase(Locale.ROOT);
        String country = locale.getCountry();
        if (country.isEmpty()) {
            return language;
        }
        return language + "_" + country.toLowerCase(Locale.ROOT);
    }

    private static int separatorIndex(String id) {
        int underscore = id.indexOf('_');
        int hyphen = id.indexOf('-');
        if (underscore < 0) {
            return hyphen;
        }
        if (hyphen < 0) {
            return underscore;
        }
        return Math.min(underscore, hyphen);
    }
}
