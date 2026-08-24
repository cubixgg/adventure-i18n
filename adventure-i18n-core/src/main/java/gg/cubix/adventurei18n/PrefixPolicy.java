package gg.cubix.adventurei18n;

import java.util.Map;
import java.util.Objects;

/**
 * Configures the recurring "{@code <prefix>} tag whose value is itself a translation" pattern
 * (e.g. a {@code [ProjectName]} prefix on every chat line): which lang key holds the prefix's own
 * translation, and which tag string stands for it in other messages. Only the key name and tag
 * string are configurable - the library has no notion of what a project's prefix looks like.
 */
public record PrefixPolicy(String templateKey, String tag) {

    public PrefixPolicy {
        Objects.requireNonNull(templateKey, "templateKey");
        Objects.requireNonNull(tag, "tag");
    }

    /**
     * @param templateKey the lang key holding the prefix's own translation
     * @param tag         the tag string that stands for it in other messages, e.g. {@code "<prefix>"}
     */
    public static PrefixPolicy of(String templateKey, String tag) {
        return new PrefixPolicy(templateKey, tag);
    }

    /**
     * Splices this policy's prefix into {@code message}: every literal occurrence of {@link #tag}
     * is replaced with the prefix's own translation, looked up in {@code bundle} under
     * {@link #templateKey} - done textually, *before* MiniMessage parses {@code message}, so the
     * prefix's own tags get parsed in the same pass as the surrounding message.
     *
     * <p>This is a single, non-recursive substitution: the prefix's own value is never itself
     * re-scanned for further occurrences of {@link #tag}. That's the guard against self-reference
     * (a prefix template that, by mistake, contains its own tag) - it can't recurse, because it
     * never re-processes its own output. A leftover, unresolved {@code tag} in that case is left
     * as-is rather than causing unbounded recursion.
     *
     * <p>If {@code bundle} has no value for {@link #templateKey}, {@code message} is returned
     * unchanged - a missing prefix translation shouldn't break every other message that uses one.
     */
    public String splice(String message, Map<String, String> bundle) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(bundle, "bundle");

        if (!message.contains(tag)) {
            return message;
        }

        String prefixValue = bundle.get(templateKey);
        if (prefixValue == null) {
            return message;
        }

        return message.replace(tag, prefixValue);
    }
}
