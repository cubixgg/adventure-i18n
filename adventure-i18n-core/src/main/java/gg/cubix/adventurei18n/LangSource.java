package gg.cubix.adventurei18n;

import java.util.Locale;
import java.util.Map;

/**
 * Loads every discovered lang bundle, keyed by the {@link Locale} it was found for. There is no
 * bundled list of supported locales anywhere in this library - whichever locales a {@code
 * LangSource} finds *are* the supported locales; a new language is added by adding a bundle a
 * source can find, not by registering it in code.
 */
@FunctionalInterface
public interface LangSource {

    /**
     * Loads every bundle this source can currently find. May be called more than once (e.g. for a
     * manual reload); nothing about this interface requires caching the result.
     */
    Map<Locale, Map<String, String>> load();
}
