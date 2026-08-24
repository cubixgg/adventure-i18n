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
        Locale frFr = Locale.of("fr", "FR");
        Set<Locale> available = Set.of(Locale.US, frFr);

        Optional<Locale> resolved = strategy.resolve(frFr, Locale.US, available);

        assertEquals(Optional.of(frFr), resolved);
    }

    @Test
    void regionalVariantOfSameLanguageWinsOverFallback() {
        Locale frFr = Locale.of("fr", "FR");
        Locale frCa = Locale.of("fr", "CA");
        Set<Locale> available = Set.of(Locale.US, frFr);

        Optional<Locale> resolved = strategy.resolve(frCa, Locale.US, available);

        assertEquals(Optional.of(frFr), resolved);
    }

    @Test
    void multipleVariantsResolveDeterministicallyByAlphabeticalId() {
        Locale frCa = Locale.of("fr", "CA");
        Locale frFr = Locale.of("fr", "FR");
        Locale frCh = Locale.of("fr", "CH");
        Set<Locale> available = Set.of(Locale.US, frCa, frFr);

        Optional<Locale> resolved = strategy.resolve(frCh, Locale.US, available);

        // "fr_ca" sorts before "fr_fr" - deterministic regardless of Set iteration order.
        assertEquals(Optional.of(frCa), resolved);
    }

    @Test
    void fallbackLocaleWinsWhenNoVariantAvailable() {
        Set<Locale> available = Set.of(Locale.US, Locale.of("fr", "FR"));

        Optional<Locale> resolved = strategy.resolve(Locale.of("no", "NO"), Locale.US, available);

        assertEquals(Optional.of(Locale.US), resolved);
    }

    @Test
    void emptyWhenNothingMatchesNotEvenFallback() {
        Set<Locale> available = Set.of(Locale.of("fr", "FR"));

        Optional<Locale> resolved = strategy.resolve(Locale.of("no", "NO"), Locale.US, available);

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
