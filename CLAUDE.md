# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
[`CONTRIBUTING.md`](./CONTRIBUTING.md) owns the commit/branch/PR/workflow rules; don't duplicate those
here.

## Project status

This repository is currently a concept/pre-implementation stage: `docs/spec.md` is the design
document, and the Gradle scaffold has no source files yet (`src/main/java`, `src/main/resources`,
`src/test/java`, `src/test/resources` are all empty). **Read `docs/spec.md` in full before writing
any code here** — it is the authoritative design for the class layout, API shapes, and rationale
described below. Do not invent structure that contradicts it; if an implementation decision isn't
covered by the spec, prefer asking or extending it consistently rather than guessing.

## What this project is

`adventure-i18n` is a generic Adventure/MiniMessage translation library for Minecraft server
projects (Paper, Velocity, Minestom, Sponge). It exists to pull the "read lang files per locale,
keep them in a map, register as a `GlobalTranslator` source, resolve `<tag>` placeholders against a
project's colour palette" logic out of individual projects and into a standalone, platform-neutral
library.

Three things that are usually hardcoded in ad-hoc implementations are the fixed points this library
must keep configurable/pluggable instead:
1. The set of supported locales — must be **auto-discovered** from lang files found at a scanned
   location, not enumerated in a constant. Only the fallback locale is required config, and it's
   validated (fail fast) against what was actually discovered.
2. The translator's namespace `Key` and colour palette — passed in by the consumer, never hardcoded.
3. The lang file format — pluggable via `LangFileFormat`; `PropertiesLangFileFormat` (in core, zero
   extra dependencies) is the default, JSON is an optional add-on module.

## Commands

Build/test via the Gradle wrapper (Gradle 9.6.1, configured in `gradle/wrapper/gradle-wrapper.properties`):

```
./gradlew build          # compile + run all tests
./gradlew test           # run tests only
./gradlew test --tests "gg.cubix.adventurei18n.SomeTestClass"        # single test class
./gradlew test --tests "gg.cubix.adventurei18n.SomeTestClass.someMethod"  # single test method
```

Tests use JUnit 5 (Jupiter) via `junit-bom`, executed with `useJUnitPlatform()`.

## Architecture (per docs/spec.md)

The design calls for **two Gradle modules/artifacts**, not one, specifically so that depending on
core never transitively pulls in a JSON library:

- `adventure-i18n-core` — depends only on `adventure-api`, `adventure-text-minimessage`, `slf4j-api`.
  Contains locale parsing (`LocaleCodes`), lang file scanning (`LangSource` /
  `ClasspathLangSource`), the pluggable parser interface (`LangFileFormat`) with its default
  `PropertiesLangFileFormat`, locale resolution (`FallbackStrategy` / `LanguageVariantFallback`),
  the colour mechanism (`TagPalette`), the optional `<prefix>` splice (`PrefixPolicy`), the
  `MiniMessageTranslator` implementation (`KeyedTranslator`, builder-assembled), the
  per-recipient locale override seam (`LocaleSource`), and the install/render entry points
  (`Messages`, `Args`).
- `adventure-i18n-json` — optional add-on, pulls in Gson, provides only `JsonLangFileFormat`.

**Note:** the current `build.gradle.kts`/`settings.gradle.kts` are still a single-module skeleton.
Splitting into the two-module layout described above is part of the implementation work, not
something already done.

Key architectural decisions worth internalizing before touching related code (all elaborated with
rationale in `docs/spec.md`):

- **No bundled locale list anywhere.** A language exists because a lang file for it was found by a
  `LangSource`, never because it's registered in code.
- **`FallbackStrategy` is swappable and separate from scanning.** Which regional variant represents
  a language (e.g. does `de_DE` serve a `de_AT` client?) is an explicit, deterministic rule — never
  decided implicitly by classpath scan/read order.
- **Client-locale automatism vs. self-managed locale:** the default path
  (`Messages.install(translator)` + `Component.translatable(...)`) resolves against the locale the
  client reports, via `GlobalTranslator`. A project that manages locale itself (e.g. stored per
  player in a database) must use the `LocaleSource`-based `Messages.render(...)` overload and
  resolve eagerly server-side — sending a raw `Component.translatable(...)` in that mode would let
  the client's own locale silently win again. Both paths coexist and are chosen per call site.
- **`Args` never parses user-supplied text as MiniMessage.** Text placeholders are inserted verbatim
  so player-controlled strings (e.g. display names) can't inject colours/click events.
- **Fail-fast only where silence would be worse:** a missing fallback-locale file aborts at build
  time; an unrecognized file name in the scanned directory is just skipped and logged once (not a
  boot abort, since stray non-lang files may legitimately be present).

## Explicit non-goals (do not implement without discussion)

- No ICU/CLDR plural rules or `MessageFormat` — MiniMessage choice tags + `Args.number` are
  considered sufficient for v1.
- No YAML lang file format in v1 (only the `LangFileFormat` interface needs to allow for it later).
- No Bukkit/Velocity/Minestom platform adapter classes in the library — `Messages.install` is
  already platform-neutral via `GlobalTranslator`; a platform adapter, if ever needed, is a separate
  add-on module, not core.
