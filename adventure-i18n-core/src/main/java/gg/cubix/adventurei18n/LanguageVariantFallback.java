package gg.cubix.adventurei18n;

import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Default {@link FallbackStrategy}: exact locale, then a regional variant of the same language,
 * then the translator's designated fallback locale, then nothing. Stateless - the fallback locale
 * is a {@link #resolve} parameter, not constructor state.
 *
 * <p>When several regional variants of the requested language are available (e.g. both {@code
 * de_AT} and {@code de_DE} for a {@code de_CH} request), the one that sorts first by
 * {@link LocaleCodes#id} wins - a deterministic rule, explicitly not an accident of whichever order
 * a {@code LangSource} happened to discover them in.
 */
public final class LanguageVariantFallback implements FallbackStrategy {

    @Override
    public Optional<Locale> resolve(Locale requested, Locale fallback, Set<Locale> available) {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(fallback, "fallback");
        Objects.requireNonNull(available, "available");

        if (available.contains(requested)) {
            return Optional.of(requested);
        }

        Optional<Locale> variant = sameLanguageVariant(requested, available);
        if (variant.isPresent()) {
            return variant;
        }

        if (available.contains(fallback)) {
            return Optional.of(fallback);
        }

        return Optional.empty();
    }

    private static Optional<Locale> sameLanguageVariant(Locale requested, Set<Locale> available) {
        String language = requested.getLanguage();
        return available.stream()
                .filter(locale -> locale.getLanguage().equals(language))
                .min(Comparator.comparing(LocaleCodes::id));
    }
}
