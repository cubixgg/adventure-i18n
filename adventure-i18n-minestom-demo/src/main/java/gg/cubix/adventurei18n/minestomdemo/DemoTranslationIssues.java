package gg.cubix.adventurei18n.minestomdemo;

import gg.cubix.adventurei18n.LocaleCodes;
import gg.cubix.adventurei18n.TranslationIssueListener;
import net.kyori.adventure.key.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link TranslationIssueListener#logging()} already reports both kinds of issues via SLF4J - this
 * is a second, distinctly-tagged implementation, wired into both demo translators via
 * {@code .issues(...)} instead of the default, to demonstrate that the listener is a plain
 * interface a project can implement itself (e.g. to page staff in-game instead of only logging).
 *
 * <p>Deduplicated per (namespace, key, locale) the same way {@code LoggingTranslationIssueListener}
 * is - with {@code MinestomDemoServer}'s {@code minestom.automatic-component-translation} flag on,
 * every outgoing translatable Component (including vanilla ones Minestom itself sends, e.g.
 * {@code block.minecraft.air}) is queried against both installed translators, so an undeduplicated
 * listener would otherwise log the same miss once per packet instead of once ever.
 */
public final class DemoTranslationIssues implements TranslationIssueListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("adventure-i18n-demo");

    private final Set<String> loggedMissingKeys = ConcurrentHashMap.newKeySet();
    private final Set<String> loggedFallbackServed = ConcurrentHashMap.newKeySet();

    @Override
    public void missingKey(Key namespace, String key, Locale requested) {
        String id = namespace + "/" + key + "/" + LocaleCodes.id(requested);
        if (loggedMissingKeys.add(id)) {
            LOGGER.warn("[i18n-demo] no translation at all for '{}' in namespace '{}' (requested {})",
                    key, namespace, LocaleCodes.id(requested));
        }
    }

    @Override
    public void fallbackServed(Key namespace, String key, Locale requested, Locale served) {
        String id = namespace + "/" + key + "/" + LocaleCodes.id(requested) + "->" + LocaleCodes.id(served);
        if (loggedFallbackServed.add(id)) {
            LOGGER.info("[i18n-demo] '{}' in namespace '{}': served the '{}' bundle for a '{}' request",
                    key, namespace, LocaleCodes.id(served), LocaleCodes.id(requested));
        }
    }
}
