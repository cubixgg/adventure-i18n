package gg.cubix.adventurei18n;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Looks up which locale a specific recipient should be served in - the seam that makes locale
 * origin swappable instead of always deferring to whatever the client itself reports. No
 * reference to any {@code Player}/{@code Audience} type here, just a {@link UUID} - the one
 * identifier every Minecraft project already has - so this interface stays platform-free.
 *
 * <p>A project implements this against its own cache (populated from a database on login, updated
 * on an explicit language change); this library prescribes neither caching nor database access,
 * that stays the consumer's job.
 *
 * <p><b>A {@code LocaleSource} that runs a database query on every call is a bug in the consumer,
 * not a problem of this interface.</b> {@link #localeOf} is expected to be a cheap, in-memory
 * lookup - it may be called on every rendered message.
 */
@FunctionalInterface
public interface LocaleSource {

    /**
     * The locale {@code recipientId} should be served in, or {@link Optional#empty()} if this
     * source has no opinion (e.g. the recipient hasn't picked a language yet, or was never seen).
     */
    Optional<Locale> localeOf(UUID recipientId);
}
