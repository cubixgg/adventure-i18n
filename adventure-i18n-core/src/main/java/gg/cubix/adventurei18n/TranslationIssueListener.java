package gg.cubix.adventurei18n;

import net.kyori.adventure.key.Key;

import java.util.Locale;

/**
 * Seam through which a {@code KeyedTranslator}'s missing-key and fallback-served events can be
 * observed - for testing without scraping log output, or for a project that wants its own
 * reporting (metrics, an in-game staff alert, ...) instead of (or in addition to) logging.
 */
public interface TranslationIssueListener {

    /**
     * No translation was found for {@code key} at all - neither in {@code requested}'s resolved
     * locale nor in the translator's fallback locale.
     */
    void missingKey(Key namespace, String key, Locale requested);

    /**
     * {@code key} was resolved from {@code served} instead of the exact {@code requested} locale
     * (a regional variant, or the translator's fallback locale).
     */
    void fallbackServed(Key namespace, String key, Locale requested, Locale served);

    /**
     * The default: logs both kinds of events via SLF4J, deduplicated so a hot lookup path doesn't
     * spam the log with the same issue repeatedly.
     */
    static TranslationIssueListener logging() {
        return new LoggingTranslationIssueListener();
    }

    /**
     * Observes nothing. For a project that has its own reporting and wants to opt out of logging
     * entirely.
     */
    static TranslationIssueListener noop() {
        return new TranslationIssueListener() {
            @Override
            public void missingKey(Key namespace, String key, Locale requested) {
            }

            @Override
            public void fallbackServed(Key namespace, String key, Locale requested, Locale served) {
            }
        };
    }
}
