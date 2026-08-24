package gg.cubix.adventurei18n;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrefixPolicyTest {

    private final PrefixPolicy policy = PrefixPolicy.of("myproject.prefix", "<prefix>");

    @Test
    void ofFactoryMatchesRecordComponents() {
        assertEquals("myproject.prefix", policy.templateKey());
        assertEquals("<prefix>", policy.tag());
    }

    @Test
    void splicesPrefixWhenTagPresent() {
        Map<String, String> bundle = Map.of("myproject.prefix", "[Server]");

        String result = policy.splice("<prefix> Hello", bundle);

        assertEquals("[Server] Hello", result);
    }

    @Test
    void leavesMessageUnchangedWhenTagAbsent() {
        String result = policy.splice("Hello world", Map.of());

        assertEquals("Hello world", result);
    }

    @Test
    void leavesMessageUnchangedWhenPrefixTranslationMissing() {
        String result = policy.splice("<prefix> Hello", Map.of());

        assertEquals("<prefix> Hello", result);
    }

    @Test
    void selfReferencingPrefixDoesNotRecurseInfinitely() {
        Map<String, String> bundle = Map.of("myproject.prefix", "[Server <prefix>]");

        String result = policy.splice("<prefix> Hello", bundle);

        // Single substitution pass: the tag inside the substituted-in prefix value itself is
        // never re-processed, so it's left unresolved rather than recursing.
        assertEquals("[Server <prefix>] Hello", result);
    }

    @Test
    void ofThrowsOnNullTemplateKey() {
        assertThrows(NullPointerException.class, () -> PrefixPolicy.of(null, "<prefix>"));
    }

    @Test
    void ofThrowsOnNullTag() {
        assertThrows(NullPointerException.class, () -> PrefixPolicy.of("myproject.prefix", null));
    }

    @Test
    void spliceThrowsOnNullMessage() {
        assertThrows(NullPointerException.class, () -> policy.splice(null, Map.of()));
    }

    @Test
    void spliceThrowsOnNullBundle() {
        assertThrows(NullPointerException.class, () -> policy.splice("<prefix> Hello", null));
    }
}
