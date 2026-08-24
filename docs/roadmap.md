# Roadmap

Implementation plan derived from [`spec.md`](spec.md). Check items off as they land.

## 1. Project & build setup

- [x] Split the single-module Gradle skeleton into a multi-module build
  - [x] `adventure-i18n-core` module (own `build.gradle.kts`)
  - [x] `adventure-i18n-json` module (own `build.gradle.kts`, depends on `core`)
  - [x] Update `settings.gradle.kts` to `include("adventure-i18n-core", "adventure-i18n-json")`
- [x] `adventure-i18n-core` dependencies: `adventure-api`, `adventure-text-minimessage`, `slf4j-api` only — no JSON library, not even transitively
- [x] `adventure-i18n-json` dependencies: `adventure-i18n-core` + Gson
- [x] Decide package name (spec uses placeholder `yourorg.i18n`; current group id is `gg.cubix.adventurei18n`) and apply consistently across both modules — decided: `gg.cubix.adventurei18n` for core, `gg.cubix.adventurei18n.json` for the JSON add-on
- [x] Set up publishing config (`maven-publish`) for both artifacts, since the goal is a standalone open-source library — `publishToMavenLocal` works for both; no remote repository target configured yet (Maven Central vs. self-hosted, see section 14)

## 2. Core: locale identifiers

- [x] `LocaleCodes`
  - [x] `parse(String id)` — Minecraft-style `en_us` → `Locale`, returns `null` on invalid format
  - [x] `id(Locale locale)` — `Locale` → `en_us`-style string
  - [x] Validation regex `[A-Za-z]{2,8}([_-][A-Za-z0-9]{2,8})?` to reject arbitrary/hostile input (e.g. from a `/lang` command)
  - [x] Unit tests: valid ids, invalid ids, round-trip `id(parse(x)) == x`

## 3. Core: lang file parsing

- [x] `LangFileFormat` interface — `fileExtension()`, `parse(InputStream in, String sourceName)`
- [x] `PropertiesLangFileFormat` (default implementation)
  - [x] Built on `java.util.Properties` (explicit UTF-8 `InputStreamReader`, not `Properties#load(InputStream)`'s ISO-8859-1 default — see the spec.md correction made alongside this item)
  - [x] Class documentation explaining escaping rules (backslashes, leading whitespace in values) so e.g. `key = <error>Score: <points></error>` is understood to parse correctly
  - [x] Unit tests covering keys/values with `:`, `<`, `>`, and escaped characters

## 4. Core: locale discovery

- [x] `LangSource` functional interface — `Map<Locale, Map<String,String>> load()`
- [x] `ClasspathLangSource`
  - [x] `scanning(String directory)` — default class loader + `PropertiesLangFileFormat`
  - [x] `scanning(ClassLoader loader, String directory, LangFileFormat format)` — explicit variant
  - [x] File-URL scanning (exploded classes / IDE run): list directory, match `<identifier>.<extension>`
  - [x] Jar-URL scanning (`jar:file:...!/lang`): open as `JarFile`, filter by `lang/` prefix + format extension
  - [x] Skip (and log once) file names that don't parse as a locale id via `LocaleCodes.parse` — no boot abort
  - [x] Unit tests against an exploded resource directory
  - [x] Unit tests against a real built jar (packaged fixture) — flagged in spec as highest platform-dependent risk; caught a real bug (see below)

## 5. Core: fallback resolution

- [ ] `FallbackStrategy` interface — `Optional<Locale> resolve(Locale requested, Set<Locale> available)`
- [ ] `LanguageVariantFallback` (default implementation)
  - [ ] Order: exact locale → regional variant of same language → fixed fallback locale → empty
  - [ ] Deterministic selection of "which regional variant represents a language" (e.g. alphabetical, or a priority list on the builder) — explicitly not dependent on scan/read order
  - [ ] Unit tests: exact match, variant match (e.g. `de_AT` client, `de_DE` bundle), fallback-locale match, no match at all

## 6. Core: color palette

- [ ] `TagPalette`
  - [ ] `of(Map<String, TextColor> tags)` factory
  - [ ] `resolver()` — `TagResolver` for the configured tags
  - [ ] `miniMessage()` — `MiniMessage.builder().editTags(...).build()`
  - [ ] `color(String tag)` accessor
  - [ ] Unit tests: known tag resolves to configured color, unknown tag behavior defined and tested

## 7. Core: prefix splicing

- [ ] `PrefixPolicy` record — `templateKey`, `tag`, `of(String key, String tag)` factory
- [ ] Splice logic: substitute the prefix template's own translation textually before MiniMessage parses the surrounding message
- [ ] Guard against self-reference (prefix key itself containing its own tag)
- [ ] Make it optional on `KeyedTranslator`'s builder
- [ ] Unit tests: prefix present, prefix omitted, self-referencing prefix guarded

## 8. Core: translator assembly

- [ ] `KeyedTranslator` implementing `MiniMessageTranslator`
  - [ ] Builder: `builder(Key namespace)`
  - [ ] `.source(LangSource)`
  - [ ] `.fallback(Locale)` — validated against discovered bundles, `IllegalStateException` if missing
  - [ ] `.fallbackStrategy(FallbackStrategy)` — optional, defaults to `LanguageVariantFallback`
  - [ ] `.miniMessage(MiniMessage)` — optional, defaults to `MiniMessage.miniMessage()`
  - [ ] `.prefix(PrefixPolicy)` — optional
  - [ ] `.issues(TranslationIssueListener)` — optional, defaults to `logging()`
  - [ ] `.build()`
  - [ ] Thread-safety: lookups must be safe from tick and IO threads alike
  - [ ] `register(Locale, Map<String,String>)` for runtime bundle extension (e.g. plugins adding their own keys), overwriting existing keys for that locale
  - [ ] Unit tests: builder validation (missing fallback bundle fails fast), thread-safety smoke test, runtime `register` overwrite behavior

## 9. Core: translation issue reporting

- [ ] `TranslationIssueListener` interface — seam for missing-key / fallback-served-key events
- [ ] `LoggingTranslationIssueListener` — default implementation, deduplicated via SLF4J
- [ ] Unit tests using a test double listener instead of scraping log output

## 10. Core: per-recipient locale override

- [ ] `LocaleSource` interface — `Optional<Locale> localeOf(UUID recipientId)` (no `Player`/`Audience` dependency)
- [ ] Class documentation: a `LocaleSource` that queries a database per message is a consumer bug, not a library concern — state this explicitly so it isn't built wrong
- [ ] Unit tests with a stub `LocaleSource`

## 11. Core: install & render entry points

- [ ] `Messages`
  - [ ] `install(Translator)` / `uninstall(Translator)` against `GlobalTranslator`
  - [ ] `render(Locale locale, String key, ComponentLike... args)` — eager rendering
  - [ ] `render(LocaleSource locales, UUID recipientId, String key, ComponentLike... args)` — resolves via `LocaleSource`, falls back to `KeyedTranslator.systemFallback()`, delegates to the eager overload
  - [ ] Document clearly: consumers using their own `LocaleSource` must use `render(...)` and must **not** send raw `Component.translatable(...)` (client locale would win again via `GlobalTranslator`)
- [ ] `Args`
  - [ ] Named placeholders: text, number, boolean, nested components
  - [ ] Text values always inserted verbatim, never parsed as MiniMessage (prevents injected colors/click events from player-controlled strings, e.g. display names)
  - [ ] Consider renaming to `Placeholders` for a non-Minecraft-flavored name (evaluate before first release, since it's a public API decision)
- [ ] Unit tests: install/uninstall round-trip, eager render, `LocaleSource`-based render, verbatim placeholder insertion (no MiniMessage injection)

## 12. Validation & cross-cutting testing

- [ ] `LangFileConsistency` utility — asserts all discovered locales have the same key set as the fallback locale
  - [ ] Usable as a plain JUnit assertion
  - [ ] Usable as a Gradle task for consumer projects
- [ ] End-to-end test: full `KeyedTranslator` built via builder, rendering a message through `Messages` with a real lang bundle

## 13. `adventure-i18n-json` add-on module

- [ ] `JsonLangFileFormat` implementing `LangFileFormat`
  - [ ] Parses a flat JSON object (`key -> string`)
  - [ ] Hard error (not silent drop) on nested values or non-string values — broken lang file must fail at boot, not surface raw keys to players
  - [ ] Unit tests: valid flat file, nested-value rejection, non-string-value rejection

## 14. Documentation & release readiness

- [ ] Package-level / class-level Javadoc for all public core classes (especially the escaping rules noted for `PropertiesLangFileFormat` and the caching warning on `LocaleSource`)
- [ ] Top-level `README.md`: what the library is, quickstart (`ClasspathLangSource` + `KeyedTranslator.builder` + `Messages.install`), module split rationale
- [ ] Usage examples for both the client-locale path and the `LocaleSource`-managed path
- [ ] Versioning/publishing decision (Maven Central / a repository) before tagging v1

## Explicitly out of scope for this roadmap (non-goals, per spec.md)

- [ ] ~~ICU/CLDR plural rules or `MessageFormat` support~~ — not planned for v1
- [ ] ~~YAML `LangFileFormat`~~ — not planned for v1, only keep the interface open for it later
- [ ] ~~Bukkit/Velocity/Minestom platform adapter modules~~ — not planned; `Messages.install` is already platform-neutral via `GlobalTranslator`
