package gg.cubix.adventurei18n;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which of the {@code available} locales serves a {@code requested} one. Deliberately
 * stateless with respect to any particular translator: the translator's own designated
 * {@code fallback} locale (validated against {@code available} at build time) is passed in as a
 * parameter rather than held by the strategy, so a strategy implementation can be swapped without
 * needing to know which translator it's plugged into.
 */
@FunctionalInterface
public interface FallbackStrategy {

    /**
     * Resolves {@code requested} against {@code available}, falling back toward {@code fallback}
     * if nothing better applies. Returns {@link Optional#empty()} if nothing in {@code available}
     * can serve the request at all - including {@code fallback} itself not being present.
     */
    Optional<Locale> resolve(Locale requested, Locale fallback, Set<Locale> available);
}
