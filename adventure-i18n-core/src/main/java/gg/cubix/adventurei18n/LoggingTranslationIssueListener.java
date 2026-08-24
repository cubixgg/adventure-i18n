package gg.cubix.adventurei18n;

import net.kyori.adventure.key.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default {@link TranslationIssueListener}: logs both kinds of events via SLF4J, deduplicated per
 * distinct (namespace, key, locale) combination so a key looked up on every tick or every chat
 * message doesn't spam the log with the same issue over and over.
 */
public final class LoggingTranslationIssueListener implements TranslationIssueListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingTranslationIssueListener.class);

    private final Set<String> loggedMissingKeys = ConcurrentHashMap.newKeySet();
    private final Set<String> loggedFallbackServed = ConcurrentHashMap.newKeySet();

    @Override
    public void missingKey(Key namespace, String key, Locale requested) {
        String id = namespace + "/" + key + "/" + LocaleCodes.id(requested);
        if (loggedMissingKeys.add(id)) {
            LOGGER.warn("[{}] Missing translation for key '{}' in locale '{}'",
                    namespace, key, LocaleCodes.id(requested));
        }
    }

    @Override
    public void fallbackServed(Key namespace, String key, Locale requested, Locale served) {
        String id = namespace + "/" + key + "/" + LocaleCodes.id(requested) + "->" + LocaleCodes.id(served);
        if (loggedFallbackServed.add(id)) {
            LOGGER.info("[{}] Key '{}' requested for locale '{}' served from '{}' instead",
                    namespace, key, LocaleCodes.id(requested), LocaleCodes.id(served));
        }
    }
}
