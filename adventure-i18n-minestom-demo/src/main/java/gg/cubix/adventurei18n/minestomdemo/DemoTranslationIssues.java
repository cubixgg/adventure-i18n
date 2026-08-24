package gg.cubix.adventurei18n.minestomdemo;

import gg.cubix.adventurei18n.LocaleCodes;
import gg.cubix.adventurei18n.TranslationIssueListener;
import net.kyori.adventure.key.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * {@link TranslationIssueListener#logging()} already reports both kinds of issues via SLF4J - this
 * is a second, distinctly-tagged implementation, wired into both demo translators via
 * {@code .issues(...)} instead of the default, to demonstrate that the listener is a plain
 * interface a project can implement itself (e.g. to page staff in-game instead of only logging).
 */
public final class DemoTranslationIssues implements TranslationIssueListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("adventure-i18n-demo");

    @Override
    public void missingKey(Key namespace, String key, Locale requested) {
        LOGGER.warn("[i18n-demo] no translation at all for '{}' in namespace '{}' (requested {})",
                key, namespace, LocaleCodes.id(requested));
    }

    @Override
    public void fallbackServed(Key namespace, String key, Locale requested, Locale served) {
        LOGGER.info("[i18n-demo] '{}' in namespace '{}': served the '{}' bundle for a '{}' request",
                key, namespace, LocaleCodes.id(served), LocaleCodes.id(requested));
    }
}
