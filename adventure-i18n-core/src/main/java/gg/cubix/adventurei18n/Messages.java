package gg.cubix.adventurei18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Installs/uninstalls a translator on the {@link GlobalTranslator}, and renders a translatable
 * message either lazily - resolved later, automatically, against whichever locale the recipient's
 * client itself reports (the default Adventure/platform behavior) - or eagerly against an explicit
 * locale, for a project managing locale itself via {@link LocaleSource}.
 *
 * <p>There is no {@code Args}/{@code Placeholders} class here (see ADR-0004): build named
 * placeholders with upstream
 * {@link net.kyori.adventure.text.minimessage.translation.Argument}'s own static factories
 * (e.g. {@code Argument.string("player", name)}) and pass them as {@code args}.
 */
public final class Messages {

    private Messages() {
    }

    /**
     * Registers {@code translator} as a {@link GlobalTranslator} source.
     */
    public static void install(Translator translator) {
        Objects.requireNonNull(translator, "translator");
        GlobalTranslator.translator().addSource(translator);
    }

    /**
     * Removes {@code translator} from the {@link GlobalTranslator}'s sources.
     */
    public static void uninstall(Translator translator) {
        Objects.requireNonNull(translator, "translator");
        GlobalTranslator.translator().removeSource(translator);
    }

    /**
     * Builds an unresolved {@link Component#translatable(String, ComponentLike...)}. Resolution
     * happens later, automatically, against whichever locale the recipient's client reports.
     *
     * <p><b>Do not use this overload for a recipient whose locale is managed via a
     * {@link LocaleSource}</b> - the client's own reported locale would win again via
     * {@link GlobalTranslator}; use
     * {@link #render(LocaleSource, UUID, String, ComponentLike...)} instead for that case.
     */
    public static Component render(String key, ComponentLike... args) {
        Objects.requireNonNull(key, "key");
        return Component.translatable(key, args);
    }

    /**
     * Resolves a translatable message immediately, against {@code locale}, using whichever
     * translators are currently installed via {@link #install}.
     */
    public static Component render(Locale locale, String key, ComponentLike... args) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(key, "key");
        return GlobalTranslator.render(Component.translatable(key, args), locale);
    }

    /**
     * Resolves {@code recipientId}'s locale via {@code locales}, falling back to
     * {@link KeyedTranslator#systemFallback()} if it has no opinion, then renders eagerly against
     * that locale - see {@link #render(Locale, String, ComponentLike...)}.
     */
    public static Component render(LocaleSource locales, UUID recipientId, String key, ComponentLike... args) {
        Objects.requireNonNull(locales, "locales");
        Objects.requireNonNull(recipientId, "recipientId");

        Locale locale = locales.localeOf(recipientId).orElseGet(KeyedTranslator::systemFallback);
        return render(locale, key, args);
    }
}
