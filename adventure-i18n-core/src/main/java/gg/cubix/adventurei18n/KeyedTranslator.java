package gg.cubix.adventurei18n;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.MiniMessageTranslator;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A {@link MiniMessageTranslator} assembled via a builder instead of a hardcoded namespace and
 * palette. {@code MiniMessageTranslator} already does the heavy lifting - deserializing with the
 * configured {@link MiniMessage} instance, resolving {@code <arg:0>}/named-argument tags, applying
 * fallback style, appending children - so this class only has to resolve, for a given key and
 * requested locale, which raw MiniMessage string (if any) should be returned: locale-level
 * resolution via {@link FallbackStrategy}, then a plain key lookup in the resolved locale's bundle,
 * then (optionally) prefix splicing.
 *
 * <p>Thread-safe: bundles are stored in {@link ConcurrentHashMap}s so lookups (tick thread, e.g. on
 * every chat message) and {@link #register} (e.g. a plugin loading extra keys at startup, from an
 * IO thread) can happen concurrently without external synchronization.
 */
public final class KeyedTranslator extends MiniMessageTranslator {

    private final Key namespace;
    private final Locale fallbackLocale;
    private final FallbackStrategy fallbackStrategy;
    private final PrefixPolicy prefix;
    private final TranslationIssueListener issues;
    private final Map<Locale, Map<String, String>> bundles = new ConcurrentHashMap<>();

    private KeyedTranslator(Builder builder, Map<Locale, Map<String, String>> loaded) {
        super(builder.miniMessage);
        this.namespace = builder.namespace;
        this.fallbackLocale = builder.fallbackLocale;
        this.fallbackStrategy = builder.fallbackStrategy;
        this.prefix = builder.prefix;
        this.issues = builder.issues;
        loaded.forEach((locale, translations) -> bundles.put(locale, new ConcurrentHashMap<>(translations)));
    }

    public static Builder builder(Key namespace) {
        return new Builder(namespace);
    }

    /**
     * A sane, documented last resort for {@code Messages.render(LocaleSource, ...)} when a
     * {@link LocaleSource} genuinely has no opinion. Deliberately just a constant, not tied to any
     * specific installed {@code KeyedTranslator}'s own {@link #fallback()} - nothing tracks "the"
     * installed translator, since {@code GlobalTranslator} already supports several at once (e.g.
     * one per namespace).
     */
    public static Locale systemFallback() {
        return Locale.US;
    }

    @Override
    public Key name() {
        return namespace;
    }

    /**
     * The fallback locale this translator was built with.
     */
    public Locale fallback() {
        return fallbackLocale;
    }

    /**
     * Registers additional translations for {@code locale} at runtime (e.g. a plugin bringing its
     * own keys) - keys that already exist for that locale are overwritten, everything else is kept.
     * Safe to call concurrently with lookups happening on another thread.
     */
    public void register(Locale locale, Map<String, String> translations) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(translations, "translations");
        bundles.computeIfAbsent(locale, l -> new ConcurrentHashMap<>()).putAll(translations);
    }

    @Override
    protected String getMiniMessageString(String key, Locale locale) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(locale, "locale");

        Optional<Locale> served = fallbackStrategy.resolve(locale, fallbackLocale, bundles.keySet());
        if (served.isEmpty()) {
            issues.missingKey(namespace, key, locale);
            return null;
        }

        Map<String, String> bundle = bundles.get(served.get());
        String template = bundle == null ? null : bundle.get(key);
        if (template == null) {
            issues.missingKey(namespace, key, locale);
            return null;
        }

        if (!served.get().equals(locale)) {
            issues.fallbackServed(namespace, key, locale, served.get());
        }

        return prefix == null ? template : prefix.splice(template, bundle);
    }

    public static final class Builder {

        private final Key namespace;
        private LangSource source;
        private Locale fallbackLocale;
        private FallbackStrategy fallbackStrategy = new LanguageVariantFallback();
        private MiniMessage miniMessage = MiniMessage.miniMessage();
        private PrefixPolicy prefix;
        private TranslationIssueListener issues = TranslationIssueListener.logging();

        private Builder(Key namespace) {
            this.namespace = Objects.requireNonNull(namespace, "namespace");
        }

        public Builder source(LangSource source) {
            this.source = Objects.requireNonNull(source, "source");
            return this;
        }

        public Builder fallback(Locale fallback) {
            this.fallbackLocale = Objects.requireNonNull(fallback, "fallback");
            return this;
        }

        public Builder fallbackStrategy(FallbackStrategy fallbackStrategy) {
            this.fallbackStrategy = Objects.requireNonNull(fallbackStrategy, "fallbackStrategy");
            return this;
        }

        public Builder miniMessage(MiniMessage miniMessage) {
            this.miniMessage = Objects.requireNonNull(miniMessage, "miniMessage");
            return this;
        }

        public Builder prefix(PrefixPolicy prefix) {
            this.prefix = Objects.requireNonNull(prefix, "prefix");
            return this;
        }

        public Builder issues(TranslationIssueListener issues) {
            this.issues = Objects.requireNonNull(issues, "issues");
            return this;
        }

        /**
         * @throws IllegalStateException if {@link #source} wasn't set, or if the fallback locale
         *                                isn't among the bundles {@link #source} discovers
         */
        public KeyedTranslator build() {
            if (source == null) {
                throw new IllegalStateException("source(...) must be set before build()");
            }
            if (fallbackLocale == null) {
                throw new IllegalStateException("fallback(...) must be set before build()");
            }

            Map<Locale, Map<String, String>> loaded = source.load();
            if (!loaded.containsKey(fallbackLocale)) {
                throw new IllegalStateException(
                        "Fallback locale '" + LocaleCodes.id(fallbackLocale)
                                + "' was not found among the discovered lang bundles: "
                                + describe(loaded.keySet()));
            }

            return new KeyedTranslator(this, loaded);
        }

        private static String describe(Set<Locale> locales) {
            return locales.stream().map(LocaleCodes::id).sorted().collect(Collectors.joining(", "));
        }
    }
}
