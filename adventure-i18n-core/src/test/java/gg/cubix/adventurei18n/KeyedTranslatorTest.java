package gg.cubix.adventurei18n;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyedTranslatorTest {

    private static final Key NAMESPACE = Key.key("test", "i18n");

    @Test
    void buildRequiresSource() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> KeyedTranslator.builder(NAMESPACE).fallback(Locale.US).build());

        assertTrue(thrown.getMessage().contains("source"));
    }

    @Test
    void buildRequiresFallback() {
        LangSource source = () -> Map.of(Locale.US, Map.of("greeting", "Hello"));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> KeyedTranslator.builder(NAMESPACE).source(source).build());

        assertTrue(thrown.getMessage().contains("fallback"));
    }

    @Test
    void buildFailsFastWhenFallbackLocaleNotDiscovered() {
        LangSource source = () -> Map.of(Locale.GERMANY, Map.of("greeting", "Hallo"));

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> KeyedTranslator.builder(NAMESPACE).source(source).fallback(Locale.US).build());

        assertTrue(thrown.getMessage().contains("en_us"));
    }

    @Test
    void resolvesExactLocale() {
        KeyedTranslator translator = translatorWith(Map.of(
                Locale.US, Map.of("greeting", "Hello"),
                Locale.GERMANY, Map.of("greeting", "Hallo")));

        assertEquals("Hello", translator.getMiniMessageString("greeting", Locale.US));
        assertEquals("Hallo", translator.getMiniMessageString("greeting", Locale.GERMANY));
    }

    @Test
    void fallsBackToRegionalVariant() {
        KeyedTranslator translator = translatorWith(Map.of(
                Locale.US, Map.of("greeting", "Hello"),
                Locale.GERMANY, Map.of("greeting", "Hallo")));

        assertEquals("Hallo", translator.getMiniMessageString("greeting", Locale.of("de", "AT")));
    }

    @Test
    void fallsBackToFallbackLocaleWhenNoVariantAvailable() {
        KeyedTranslator translator = translatorWith(Map.of(Locale.US, Map.of("greeting", "Hello")));

        assertEquals("Hello", translator.getMiniMessageString("greeting", Locale.FRANCE));
    }

    @Test
    void returnsNullForCompletelyMissingKey() {
        KeyedTranslator translator = translatorWith(Map.of(Locale.US, Map.of("greeting", "Hello")));

        assertNull(translator.getMiniMessageString("does.not.exist", Locale.US));
    }

    @Test
    void splicesPrefixWhenConfigured() {
        LangSource source = () -> Map.of(Locale.US, Map.of(
                "myproject.prefix", "[Server]",
                "greeting", "<prefix> Hello"));

        KeyedTranslator translator = KeyedTranslator.builder(NAMESPACE)
                .source(source)
                .fallback(Locale.US)
                .prefix(PrefixPolicy.of("myproject.prefix", "<prefix>"))
                .build();

        assertEquals("[Server] Hello", translator.getMiniMessageString("greeting", Locale.US));
    }

    @Test
    void registerOverwritesExistingKeysAndAddsNewOnesForALocale() {
        KeyedTranslator translator = translatorWith(Map.of(Locale.US, Map.of("greeting", "Hello")));

        translator.register(Locale.US, Map.of("greeting", "Hi", "farewell", "Bye"));

        assertEquals("Hi", translator.getMiniMessageString("greeting", Locale.US));
        assertEquals("Bye", translator.getMiniMessageString("farewell", Locale.US));
    }

    @Test
    void registerCanAddAWholeNewLocale() {
        KeyedTranslator translator = translatorWith(Map.of(Locale.US, Map.of("greeting", "Hello")));

        translator.register(Locale.FRANCE, Map.of("greeting", "Bonjour"));

        assertEquals("Bonjour", translator.getMiniMessageString("greeting", Locale.FRANCE));
    }

    @Test
    void nameReturnsConfiguredNamespace() {
        KeyedTranslator translator = translatorWith(Map.of(Locale.US, Map.of("greeting", "Hello")));

        assertEquals(NAMESPACE, translator.name());
    }

    @Test
    void fallbackReturnsConfiguredFallbackLocale() {
        KeyedTranslator translator = translatorWith(Map.of(Locale.US, Map.of("greeting", "Hello")));

        assertEquals(Locale.US, translator.fallback());
    }

    /**
     * End-to-end through the public {@code MiniMessageTranslator} contract: deserialization and
     * named-argument substitution are handled entirely by the upstream abstract class, not by
     * {@code getMiniMessageString} itself.
     */
    @Test
    void translateIntegratesUpstreamDeserializationAndArguments() {
        LangSource source = () -> Map.of(Locale.US, Map.of("greeting", "Hello <name>!"));
        KeyedTranslator translator = KeyedTranslator.builder(NAMESPACE)
                .source(source)
                .fallback(Locale.US)
                .build();

        TranslatableComponent component =
                (TranslatableComponent) Component.translatable("greeting", Argument.string("name", "World"));

        Component result = translator.translate(component, Locale.US);

        assertNotNull(result);
        assertEquals("Hello World!", MiniMessage.miniMessage().serialize(result));
    }

    /**
     * Per CLAUDE.md/CONTRIBUTING.md: assert against a test double, not by scraping log output.
     */
    @Test
    void reportsNoIssueOnExactMatchButDoesOnVariantAndOnMissingKey() {
        RecordingIssueListener issues = new RecordingIssueListener();
        LangSource source = () -> Map.of(
                Locale.US, Map.of("greeting", "Hello"),
                Locale.GERMANY, Map.of("greeting", "Hallo"));

        KeyedTranslator translator = KeyedTranslator.builder(NAMESPACE)
                .source(source)
                .fallback(Locale.US)
                .issues(issues)
                .build();

        assertEquals("Hello", translator.getMiniMessageString("greeting", Locale.US));
        assertTrue(issues.missingKeys.isEmpty());
        assertTrue(issues.fallbacksServed.isEmpty());

        Locale deAt = Locale.of("de", "AT");
        assertEquals("Hallo", translator.getMiniMessageString("greeting", deAt));
        assertEquals(List.of(new FallbackServed("greeting", deAt, Locale.GERMANY)), issues.fallbacksServed);

        assertNull(translator.getMiniMessageString("does.not.exist", Locale.US));
        assertEquals(List.of(new MissingKey("does.not.exist", Locale.US)), issues.missingKeys);
    }

    private record MissingKey(String key, Locale requested) {
    }

    private record FallbackServed(String key, Locale requested, Locale served) {
    }

    private static final class RecordingIssueListener implements TranslationIssueListener {
        private final List<MissingKey> missingKeys = new ArrayList<>();
        private final List<FallbackServed> fallbacksServed = new ArrayList<>();

        @Override
        public void missingKey(Key namespace, String key, Locale requested) {
            missingKeys.add(new MissingKey(key, requested));
        }

        @Override
        public void fallbackServed(Key namespace, String key, Locale requested, Locale served) {
            fallbacksServed.add(new FallbackServed(key, requested, served));
        }
    }

    @Test
    void concurrentRegisterAndLookupDoNotCorruptState() throws InterruptedException {
        KeyedTranslator translator = translatorWith(Map.of(Locale.US, Map.of("greeting", "Hello")));

        int threadCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger errors = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            int index = i;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 500; j++) {
                        translator.register(Locale.US, Map.of("thread" + index, "value" + j));
                        translator.getMiniMessageString("greeting", Locale.US);
                        translator.getMiniMessageString("thread" + index, Locale.US);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(0, errors.get());
        for (int i = 0; i < threadCount; i++) {
            assertNotNull(translator.getMiniMessageString("thread" + i, Locale.US));
        }
    }

    private static KeyedTranslator translatorWith(Map<Locale, Map<String, String>> bundles) {
        return KeyedTranslator.builder(NAMESPACE)
                .source(() -> bundles)
                .fallback(Locale.US)
                .build();
    }
}
