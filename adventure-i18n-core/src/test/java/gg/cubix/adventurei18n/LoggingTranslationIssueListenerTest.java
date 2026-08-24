package gg.cubix.adventurei18n;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/**
 * Only proves the listener doesn't throw and can be invoked repeatedly for the same and for
 * different (namespace, key, locale) combinations - actual dedupe-vs-log-line behavior lives in
 * SLF4J output, which {@code TranslationIssueListener} exists precisely so callers don't have to
 * scrape (see the test double usage in {@code KeyedTranslatorTest} for the seam this interface is
 * for).
 */
class LoggingTranslationIssueListenerTest {

    private static final Key NAMESPACE = Key.key("test", "i18n");

    @Test
    void missingKeyCanBeCalledRepeatedlyWithoutThrowing() {
        TranslationIssueListener listener = new LoggingTranslationIssueListener();

        listener.missingKey(NAMESPACE, "greeting", Locale.US);
        listener.missingKey(NAMESPACE, "greeting", Locale.US);
        listener.missingKey(NAMESPACE, "farewell", Locale.US);
        listener.missingKey(NAMESPACE, "greeting", Locale.of("no", "NO"));
    }

    @Test
    void fallbackServedCanBeCalledRepeatedlyWithoutThrowing() {
        TranslationIssueListener listener = new LoggingTranslationIssueListener();

        listener.fallbackServed(NAMESPACE, "greeting", Locale.of("nb", "NO"), Locale.of("no", "NO"));
        listener.fallbackServed(NAMESPACE, "greeting", Locale.of("nb", "NO"), Locale.of("no", "NO"));
    }

    @Test
    void factoryMethodsReturnUsableListeners() {
        TranslationIssueListener logging = TranslationIssueListener.logging();
        TranslationIssueListener noop = TranslationIssueListener.noop();

        logging.missingKey(NAMESPACE, "greeting", Locale.US);
        noop.missingKey(NAMESPACE, "greeting", Locale.US);
        noop.fallbackServed(NAMESPACE, "greeting", Locale.US, Locale.of("no", "NO"));
    }
}
