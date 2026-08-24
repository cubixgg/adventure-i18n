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
                Locale.GERMANY, Map.of("greeting", "Hallo", "farewell", "Tschuss"));

        LangFileConsistency.Report report = LangFileConsistency.check(bundles, Locale.US);

        assertTrue(report.isConsistent());
        assertTrue(report.missingKeys().isEmpty());
        assertTrue(report.extraKeys().isEmpty());
    }

    @Test
    void reportsMissingKeyInNonFallbackLocale() {
        Map<Locale, Map<String, String>> bundles = Map.of(
                Locale.US, Map.of("greeting", "Hello", "farewell", "Bye"),
                Locale.GERMANY, Map.of("greeting", "Hallo"));

        LangFileConsistency.Report report = LangFileConsistency.check(bundles, Locale.US);

        assertTrue(report.missingKeys().containsKey(Locale.GERMANY));
        assertEquals(Set.of("farewell"), report.missingKeys().get(Locale.GERMANY));
        assertTrue(report.extraKeys().isEmpty());
        assertTrue(report.describe().contains("farewell"));
    }

    @Test
    void reportsExtraKeyInNonFallbackLocale() {
        Map<Locale, Map<String, String>> bundles = Map.of(
                Locale.US, Map.of("greeting", "Hello"),
                Locale.GERMANY, Map.of("greeting", "Hallo", "orphaned", "Verwaist"));

        LangFileConsistency.Report report = LangFileConsistency.check(bundles, Locale.US);

        assertTrue(report.missingKeys().isEmpty());
        assertEquals(Set.of("orphaned"), report.extraKeys().get(Locale.GERMANY));
    }

    @Test
    void checkThrowsWhenFallbackBundleIsAbsent() {
        Map<Locale, Map<String, String>> bundles = Map.of(Locale.GERMANY, Map.of("greeting", "Hallo"));

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> LangFileConsistency.check(bundles, Locale.US));
        assertTrue(thrown.getMessage().contains("en_us"));
    }

    @Test
    void assertConsistentThrowsAssertionErrorWhenInconsistent() {
        Map<Locale, Map<String, String>> bundles = Map.of(
                Locale.US, Map.of("greeting", "Hello"),
                Locale.GERMANY, Map.of());

        assertThrows(AssertionError.class, () -> LangFileConsistency.assertConsistent(bundles, Locale.US));
    }

    @Test
    void assertConsistentDoesNotThrowWhenConsistent() {
        Map<Locale, Map<String, String>> bundles = Map.of(
                Locale.US, Map.of("greeting", "Hello"),
                Locale.GERMANY, Map.of("greeting", "Hallo"));

        LangFileConsistency.assertConsistent(bundles, Locale.US);
    }
}
