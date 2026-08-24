package gg.cubix.adventurei18n.minestomdemo;

import gg.cubix.adventurei18n.LocaleSource;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The simplest possible {@link LocaleSource}: an in-memory map, populated by {@code /lang} and
 * read back by every command that renders eagerly for a specific player (see {@link LangCommand},
 * {@link ScoreCommand}). A real project would back this with a database or player-data file,
 * populated on login and on explicit language change - {@link #localeOf} itself must stay a cheap,
 * in-memory lookup, per {@link LocaleSource}'s own class documentation.
 */
public final class PlayerLocaleStore implements LocaleSource {

    private final Map<UUID, Locale> overrides = new ConcurrentHashMap<>();

    @Override
    public Optional<Locale> localeOf(UUID recipientId) {
        return Optional.ofNullable(overrides.get(recipientId));
    }

    public void set(UUID recipientId, Locale locale) {
        overrides.put(recipientId, locale);
    }

    public void clear(UUID recipientId) {
        overrides.remove(recipientId);
    }
}
