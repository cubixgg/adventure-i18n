package gg.cubix.adventurei18n;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Separates a *specific* color palette (a project's own {@code error}/{@code success}/... tag
 * names and colors) from the generic mechanism that turns such a palette into MiniMessage tags.
 * This class has no notion of what any tag name means - a project supplies its own constants via
 * {@link #of}.
 */
public final class TagPalette {

    private final Map<String, TextColor> tags;
    private final TagResolver resolver;

    private TagPalette(Map<String, TextColor> tags, TagResolver resolver) {
        this.tags = tags;
        this.resolver = resolver;
    }

    /**
     * Builds a palette from {@code tag name -> color}. Neither tag names nor colors may be
     * {@code null}.
     */
    public static TagPalette of(Map<String, TextColor> tags) {
        Objects.requireNonNull(tags, "tags");

        Map<String, TextColor> copy = new LinkedHashMap<>(tags.size());
        TagResolver.Builder resolverBuilder = TagResolver.builder();
        tags.forEach((name, color) -> {
            Objects.requireNonNull(name, "tag name");
            Objects.requireNonNull(color, "color for tag '" + name + "'");
            copy.put(name, color);
            resolverBuilder.tag(name, Tag.styling(color));
        });

        return new TagPalette(Collections.unmodifiableMap(copy), resolverBuilder.build());
    }

    /**
     * A {@link TagResolver} for exactly this palette's tags - nothing else. Combine it yourself
     * (e.g. with {@link TagResolver#standard()}) if you need both; {@link #miniMessage()} already
     * does that.
     */
    public TagResolver resolver() {
        return resolver;
    }

    /**
     * A {@link MiniMessage} instance that understands both the standard MiniMessage tags and this
     * palette's own - equivalent to {@code MiniMessage.builder().editTags(...).build()}.
     */
    public MiniMessage miniMessage() {
        return MiniMessage.builder()
                .editTags(builder -> builder.resolver(resolver))
                .build();
    }

    /**
     * The color configured for {@code tag}.
     *
     * @throws IllegalArgumentException if {@code tag} isn't part of this palette
     */
    public TextColor color(String tag) {
        Objects.requireNonNull(tag, "tag");

        TextColor color = tags.get(tag);
        if (color == null) {
            throw new IllegalArgumentException("No color configured for tag '" + tag + "'");
        }
        return color;
    }
}
