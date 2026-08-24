package gg.cubix.adventurei18n;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangFileConsistencyTest {

    @Test
    void reportsNoIssuesForConsistentBundles() {
        Map<Locale, Map<String, String>> bundles = Map.of(
                Locale.US, Map.of("greeting", "Hello", "farewell", "Bye"),
                Locale.of("no", "NO"), Map.of("greeting", "Hei", "farewell", "Ha det"));

        LangFileConsistency.Report report = LangFileConsistency.check(bundles, Locale.US);

        assertTrue(report.isConsistent());
        assertTrue(report.missingKeys().isEmpty());
        assertTrue(report.extraKeys().isEmpty());
    }

    @Test
    void reportsMissingKeyInNonFallbackLocale() {
        Map<Locale, Map<String, String>> bundles = Map.of(
                Locale.US, Map.of("greeting", "Hello", "farewell", "Bye"),
                Locale.of("no", "NO"), Map.of("greeting", "Hei"));

        LangFileConsistency.Report report = LangFileConsistency.check(bundles, Locale.US);

        assertTrue(report.missingKeys().containsKey(Locale.of("no", "NO")));
        assertEquals(Set.of("farewell"), report.missingKeys().get(Locale.of("no", "NO")));
        assertTrue(report.extraKeys().isEmpty());
        assertTrue(report.describe().contains("farewell"));
    }

    @Test
    void reportsExtraKeyInNonFallbackLocale() {
        Map<Locale, Map<String, String>> bundles = Map.of(
                Locale.US, Map.of("greeting", "Hello"),
                Locale.of("no", "NO"), Map.of("greeting", "Hei", "orphaned", "Foreldrelos"));

        LangFileConsistency.Report report = LangFileConsistency.check(bundles, Locale.US);

        assertTrue(report.missingKeys().isEmpty());
        assertEquals(Set.of("orphaned"), report.extraKeys().get(Locale.of("no", "NO")));
    }

    @Test
    void checkThrowsWhenFallbackBundleIsAbsent() {
        Map<Locale, Map<String, String>> bundles = Map.of(Locale.of("no", "NO"), Map.of("greeting", "Hei"));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> LangFileConsistency.check(bundles, Locale.US));
        assertTrue(thrown.getMessage().contains("en_us"));
    }

    @Test
    void assertConsistentThrowsAssertionErrorWhenInconsistent() {
        Map<Locale, Map<String, String>> bundles = Map.of(
                Locale.US, Map.of("greeting", "Hello"),
                Locale.of("no", "NO"), Map.of());

        assertThrows(AssertionError.class, () -> LangFileConsistency.assertConsistent(bundles, Locale.US));
    }

    @Test
    void assertConsistentDoesNotThrowWhenConsistent() {
        Map<Locale, Map<String, String>> bundles = Map.of(
                Locale.US, Map.of("greeting", "Hello"),
                Locale.of("no", "NO"), Map.of("greeting", "Hei"));

        LangFileConsistency.assertConsistent(bundles, Locale.US);
    }
}
