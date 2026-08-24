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

- [x] `FallbackStrategy` interface — `Optional<Locale> resolve(Locale requested, Locale fallback, Set<Locale> available)` (corrected from the original 2-arg sketch — see the spec.md fix made alongside this item)
- [x] `LanguageVariantFallback` (default implementation, stateless — no constructor argument)
  - [x] Order: exact locale → regional variant of same language → fixed fallback locale → empty
  - [x] Deterministic selection of "which regional variant represents a language" (alphabetical by `LocaleCodes.id(...)`) — explicitly not dependent on scan/read order
  - [x] Unit tests: exact match, variant match (e.g. `de_AT` client, `de_DE` bundle), fallback-locale match, no match at all

## 6. Core: color palette

- [x] `TagPalette`
  - [x] `of(Map<String, TextColor> tags)` factory
  - [x] `resolver()` — `TagResolver` for the configured tags
  - [x] `miniMessage()` — `MiniMessage.builder().editTags(...).build()`
  - [x] `color(String tag)` accessor
  - [x] Unit tests: known tag resolves to configured color, unknown tag behavior defined and tested (throws `IllegalArgumentException`)

## 7. Core: prefix splicing

- [x] `PrefixPolicy` record — `templateKey`, `tag`, `of(String key, String tag)` factory
- [x] Splice logic: substitute the prefix template's own translation textually before MiniMessage parses the surrounding message (`PrefixPolicy#splice(String, Map<String, String>)`)
- [x] Guard against self-reference (prefix key itself containing its own tag) — a single, non-recursive substitution pass by construction; a leftover unresolved tag is left as-is rather than recursing
- [x] Make it optional on `KeyedTranslator`'s builder
- [x] Unit tests: prefix present, prefix omitted, self-referencing prefix guarded

## 8. Core: translator assembly

- [x] `KeyedTranslator` extends `MiniMessageTranslator` (an abstract class since Adventure 4.20.0, not something to implement from scratch — see the spec.md correction made alongside this item; only `getMiniMessageString(key, locale)` and `name()` are overridden)
  - [x] Builder: `builder(Key namespace)`
  - [x] `.source(LangSource)`
  - [x] `.fallback(Locale)` — validated against discovered bundles, `IllegalStateException` if missing
  - [x] `.fallbackStrategy(FallbackStrategy)` — optional, defaults to `LanguageVariantFallback`
  - [x] `.miniMessage(MiniMessage)` — optional, defaults to `MiniMessage.miniMessage()`
  - [x] `.prefix(PrefixPolicy)` — optional
  - [x] `.issues(TranslationIssueListener)` — optional, defaults to `logging()`
  - [x] `.build()`
  - [x] Thread-safety: lookups must be safe from tick and IO threads alike (`ConcurrentHashMap`-backed bundles)
  - [x] `register(Locale, Map<String,String>)` for runtime bundle extension (e.g. plugins adding their own keys), overwriting existing keys for that locale
  - [x] Unit tests: builder validation (missing fallback bundle fails fast), thread-safety smoke test, runtime `register` overwrite behavior

## 9. Core: translation issue reporting

Implemented alongside section 8 rather than after it: `KeyedTranslator`'s builder needs a working
`.issues(...)` default (`TranslationIssueListener.logging()`) to exist at all — see
`CONTRIBUTING.md`'s "phase order is the recommended build/test order, not a strict dependency".

- [x] `TranslationIssueListener` interface — seam for missing-key / fallback-served-key events
- [x] `LoggingTranslationIssueListener` — default implementation, deduplicated via SLF4J
- [x] Unit tests using a test double listener instead of scraping log output

## 10. Core: per-recipient locale override

- [x] `LocaleSource` interface — `Optional<Locale> localeOf(UUID recipientId)` (no `Player`/`Audience` dependency)
- [x] Class documentation: a `LocaleSource` that queries a database per message is a consumer bug, not a library concern — state this explicitly so it isn't built wrong
- [x] Unit tests with a stub `LocaleSource`

## 11. Core: install & render entry points

- [x] `Messages`
  - [x] `install(Translator)` / `uninstall(Translator)` against `GlobalTranslator`
  - [x] `render(String key, ComponentLike... args)` — lazy, resolved later against the client-reported locale
  - [x] `render(Locale locale, String key, ComponentLike... args)` — eager rendering
  - [x] `render(LocaleSource locales, UUID recipientId, String key, ComponentLike... args)` — resolves via `LocaleSource`, falls back to `KeyedTranslator.systemFallback()`, delegates to the eager overload
  - [x] Document clearly: consumers using their own `LocaleSource` must use `render(...)` and must **not** send raw `Component.translatable(...)` (client locale would win again via `GlobalTranslator`)
- [x] ~~`Args`~~ — **not built** (see [ADR-0004](./decisions/0004-no-custom-args-class.md)): upstream `net.kyori.adventure.text.minimessage.translation.Argument` already provides named text/number/boolean/component placeholders, with `.string(...)` already wrapping values in `Component.text(...)` (verbatim, never parsed as MiniMessage) — verified against the actual Adventure 5.2.0 sources, not assumed
- [x] Unit tests: install/uninstall round-trip, lazy render, eager render, `LocaleSource`-based render (both a present and an absent `LocaleSource` result, to prove `systemFallback()` — not the installed translator's own fallback — is used when absent)

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
