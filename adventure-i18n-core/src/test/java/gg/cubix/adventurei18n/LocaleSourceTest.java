package gg.cubix.adventurei18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code LocaleSource} has no library-provided implementation - it's meant to be implemented
 * against a project's own cache (spec.md's own example: {@code dbBackedLocales::get}). These tests
 * exercise a stub implementation to prove the intended usage pattern - a plain lambda over a
 * project's own lookup - actually satisfies the interface as expected.
 */
class LocaleSourceTest {

    @Test
    void stubBackedByMapReturnsConfiguredLocale() {
        UUID recipient = UUID.randomUUID();
        Map<UUID, Locale> cache = Map.of(recipient, Locale.of("no", "NO"));
        LocaleSource source = recipientId -> Optional.ofNullable(cache.get(recipientId));

        assertEquals(Optional.of(Locale.of("no", "NO")), source.localeOf(recipient));
    }

    @Test
    void stubReturnsEmptyForUnknownRecipient() {
        LocaleSource source = recipientId -> Optional.empty();

        assertEquals(Optional.empty(), source.localeOf(UUID.randomUUID()));
    }
}
