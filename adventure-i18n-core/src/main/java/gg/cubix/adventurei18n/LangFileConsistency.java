package gg.cubix.adventurei18n;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Checks whether every discovered locale has the same key set as the fallback locale - a plain
 * Java utility with no test-framework dependency, so it's usable equally as a JUnit assertion in a
 * consumer project's own tests, or from a hand-written Gradle task, or anywhere else a consumer
 * wants to validate its own lang files.
 */
public final class LangFileConsistency {

    private LangFileConsistency() {
    }

    /**
     * Compares every bundle in {@code bundles} against the {@code fallback} bundle's key set.
     *
     * @throws IllegalArgumentException if {@code fallback} isn't among {@code bundles}' keys
     */
    public static Report check(Map<Locale, Map<String, String>> bundles, Locale fallback) {
        Objects.requireNonNull(bundles, "bundles");
        Objects.requireNonNull(fallback, "fallback");

        Map<String, String> fallbackBundle = bundles.get(fallback);
        if (fallbackBundle == null) {
            throw new IllegalArgumentException(
                    "Fallback locale '" + LocaleCodes.id(fallback) + "' is not among the given bundles");
        }

        Set<String> fallbackKeys = fallbackBundle.keySet();
        Map<Locale, Set<String>> missingKeys = new LinkedHashMap<>();
        Map<Locale, Set<String>> extraKeys = new LinkedHashMap<>();

        bundles.forEach((locale, bundle) -> {
            if (locale.equals(fallback)) {
                return;
            }

            Set<String> missing = new TreeSet<>(fallbackKeys);
            missing.removeAll(bundle.keySet());
            if (!missing.isEmpty()) {
                missingKeys.put(locale, Collections.unmodifiableSet(missing));
            }

            Set<String> extra = new TreeSet<>(bundle.keySet());
            extra.removeAll(fallbackKeys);
            if (!extra.isEmpty()) {
                extraKeys.put(locale, Collections.unmodifiableSet(extra));
            }
        });

        return new Report(fallback, Collections.unmodifiableMap(missingKeys), Collections.unmodifiableMap(extraKeys));
    }

    /**
     * {@link #check}, but throws a plain {@link AssertionError} describing every inconsistency
     * instead of returning a {@link Report} - usable directly as a JUnit assertion without a JUnit
     * dependency in this artifact.
     */
    public static void assertConsistent(Map<Locale, Map<String, String>> bundles, Locale fallback) {
        Report report = check(bundles, fallback);
        if (!report.isConsistent()) {
            throw new AssertionError(report.describe());
        }
    }

    /**
     * @param fallback    the locale every other bundle was compared against
     * @param missingKeys per non-fallback locale, keys present in {@code fallback} but missing there
     * @param extraKeys   per non-fallback locale, keys present there but missing from {@code fallback}
     */
    public record Report(Locale fallback, Map<Locale, Set<String>> missingKeys, Map<Locale, Set<String>> extraKeys) {

        public boolean isConsistent() {
            return missingKeys.isEmpty() && extraKeys.isEmpty();
        }

        /**
         * A human-readable summary, one line per affected locale - suitable as an assertion
         * failure message or Gradle task output.
         */
        public String describe() {
            if (isConsistent()) {
                return "All discovered locales have the same key set as fallback locale '"
                        + LocaleCodes.id(fallback) + "'.";
            }

            StringBuilder description = new StringBuilder();
            missingKeys.forEach((locale, keys) -> description
                    .append("Locale '").append(LocaleCodes.id(locale))
                    .append("' is missing keys present in fallback '").append(LocaleCodes.id(fallback))
                    .append("': ").append(keys).append(System.lineSeparator()));
            extraKeys.forEach((locale, keys) -> description
                    .append("Locale '").append(LocaleCodes.id(locale))
                    .append("' has keys not present in fallback '").append(LocaleCodes.id(fallback))
                    .append("': ").append(keys).append(System.lineSeparator()));
            return description.toString();
        }
    }
}
