package gg.cubix.adventurei18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageVariantFallbackTest {

    private final LanguageVariantFallback strategy = new LanguageVariantFallback();

    @Test
    void exactLocaleWins() {
        Locale deDe = Locale.of("de", "DE");
        Set<Locale> available = Set.of(Locale.US, deDe);

        Optional<Locale> resolved = strategy.resolve(deDe, Locale.US, available);

        assertEquals(Optional.of(deDe), resolved);
    }

    @Test
    void regionalVariantOfSameLanguageWinsOverFallback() {
        Locale deDe = Locale.of("de", "DE");
        Locale deAt = Locale.of("de", "AT");
        Set<Locale> available = Set.of(Locale.US, deDe);

        Optional<Locale> resolved = strategy.resolve(deAt, Locale.US, available);

        assertEquals(Optional.of(deDe), resolved);
    }

    @Test
    void multipleVariantsResolveDeterministicallyByAlphabeticalId() {
        Locale deAt = Locale.of("de", "AT");
        Locale deDe = Locale.of("de", "DE");
        Locale deCh = Locale.of("de", "CH");
        Set<Locale> available = Set.of(Locale.US, deAt, deDe);

        Optional<Locale> resolved = strategy.resolve(deCh, Locale.US, available);

        // "de_at" sorts before "de_de" - deterministic regardless of Set iteration order.
        assertEquals(Optional.of(deAt), resolved);
    }

    @Test
    void fallbackLocaleWinsWhenNoVariantAvailable() {
        Set<Locale> available = Set.of(Locale.US, Locale.of("de", "DE"));

        Optional<Locale> resolved = strategy.resolve(Locale.FRANCE, Locale.US, available);

        assertEquals(Optional.of(Locale.US), resolved);
    }

    @Test
    void emptyWhenNothingMatchesNotEvenFallback() {
        Set<Locale> available = Set.of(Locale.of("de", "DE"));

        Optional<Locale> resolved = strategy.resolve(Locale.FRANCE, Locale.US, available);

        assertTrue(resolved.isEmpty());
    }

    @Test
    void resolveRequiresNonNullArguments() {
        Set<Locale> available = Set.of(Locale.US);

        assertThrows(NullPointerException.class, () -> strategy.resolve(null, Locale.US, available));
        assertThrows(NullPointerException.class, () -> strategy.resolve(Locale.US, null, available));
        assertThrows(NullPointerException.class, () -> strategy.resolve(Locale.US, Locale.US, null));
    }
}
