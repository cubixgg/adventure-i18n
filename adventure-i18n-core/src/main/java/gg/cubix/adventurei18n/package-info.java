/**
 * A generic Adventure/MiniMessage translation library: locale discovery
 * ({@link gg.cubix.adventurei18n.LangSource}/{@link gg.cubix.adventurei18n.ClasspathLangSource}),
 * fallback resolution ({@link gg.cubix.adventurei18n.FallbackStrategy}), a translator assembled via
 * a builder ({@link gg.cubix.adventurei18n.KeyedTranslator}), and install/render entry points
 * ({@link gg.cubix.adventurei18n.Messages}).
 *
 * <p>Depends only on {@code adventure-api}, {@code adventure-text-minimessage} and
 * {@code slf4j-api} - no JSON library, not even transitively. The optional JSON
 * {@link gg.cubix.adventurei18n.LangFileFormat} lives in the separate {@code adventure-i18n-json}
 * artifact ({@code gg.cubix.adventurei18n.json}) for exactly that reason.
 *
 * <p>None of the locales a project supports are enumerated anywhere in this package - whichever
 * lang bundles a {@link gg.cubix.adventurei18n.LangSource} discovers are the supported locales.
 * See <a href="https://github.com/cubixgg/adventure-i18n/blob/main/docs/spec.md">{@code spec.md}</a>
 * for the full design and the reasoning behind it.
 */
package gg.cubix.adventurei18n;
