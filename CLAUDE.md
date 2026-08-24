# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
[`CONTRIBUTING.md`](./CONTRIBUTING.md) owns the commit/branch/PR/workflow rules; don't duplicate those
here.

## Project status

`docs/roadmap.md` is the authoritative up-to-date checklist — check it for exactly which sections
are done. As of this writing, both modules' public APIs are implemented and tested through section
13: `adventure-i18n-core` (locale parsing, lang file scanning, fallback resolution, `TagPalette`,
`PrefixPolicy`, `KeyedTranslator`, `TranslationIssueListener`, `LocaleSource`, `Messages`,
`LangFileConsistency`) and `adventure-i18n-json` (`JsonLangFileFormat`). Section 14 (Javadoc
completeness, `README.md` quickstart, versioning/publishing decision) is what's left before v1.
**Read `docs/spec.md` in full before
writing any code here** — it is the authoritative design for the class layout, API shapes, and
rationale described below. Do not invent structure that contradicts it; if an implementation
decision isn't covered by the spec, prefer asking or extending it consistently rather than
guessing — and if implementing something reveals a real gap or correction in the spec, fix the spec
in the same commit (`CONTRIBUTING.md`'s rule), the way several `docs/decisions/` ADRs already did.

## What this project is

`adventure-i18n` is a generic Adventure/MiniMessage translation library for Minecraft server
projects (Paper, Velocity, Minestom, Sponge). It exists to pull the "read lang files per locale,
keep them in a map, register as a `GlobalTranslator` source, resolve `<tag>` placeholders against a
project's color palette" logic out of individual projects and into a standalone, platform-neutral
library.

Three things that are usually hardcoded in ad-hoc implementations are the fixed points this library
must keep configurable/pluggable instead:
1. The set of supported locales — must be **auto-discovered** from lang files found at a scanned
   location, not enumerated in a constant. Only the fallback locale is required config, and it's
   validated (fail fast) against what was actually discovered.
2. The translator's namespace `Key` and color palette — passed in by the consumer, never hardcoded.
3. The lang file format — pluggable via `LangFileFormat`; `PropertiesLangFileFormat` (in core, zero
   extra dependencies) is the default, JSON is an optional add-on module.

## Commands

Build/test via the Gradle wrapper (Gradle 9.6.1, configured in `gradle/wrapper/gradle-wrapper.properties`):

```
./gradlew build                                  # build + test every module
./gradlew :adventure-i18n-core:test              # tests for one module only
./gradlew test --tests "gg.cubix.adventurei18n.SomeTestClass"        # single test class
./gradlew test --tests "gg.cubix.adventurei18n.SomeTestClass.someMethod"  # single test method
./gradlew publishToMavenLocal                    # publish both artifacts locally
```

Tests use JUnit 5 (Jupiter) via `junit-bom`, executed with `useJUnitPlatform()`. No JDK toolchain
is pinned yet.

## Architecture (per docs/spec.md)

The build is split into **two Gradle modules/artifacts**, specifically so that depending on core
never transitively pulls in a JSON library:

- `adventure-i18n-core` (package `gg.cubix.adventurei18n`) — depends only on `adventure-api`
  (`api`), `adventure-text-minimessage` (`api`), `slf4j-api` (`implementation`). Contains locale
  parsing (`LocaleCodes`), lang file scanning (`LangSource` / `ClasspathLangSource`), the pluggable
  parser interface (`LangFileFormat`) with its default `PropertiesLangFileFormat`, locale
  resolution (`FallbackStrategy` / `LanguageVariantFallback`), the color mechanism
  (`TagPalette`), the optional `<prefix>` splice (`PrefixPolicy`), the `MiniMessageTranslator`
  subclass (`KeyedTranslator`, builder-assembled — see ADR context in `docs/spec.md`'s
  `KeyedTranslator` section: it's a thin adapter over an upstream abstract class, not a parser
  built from scratch), the per-recipient locale override seam (`LocaleSource`), and the
  install/render entry points (`Messages` — there is no `Args`/`Placeholders` class, see
  [ADR-0004](./docs/decisions/0004-no-custom-args-class.md); use upstream
  `net.kyori.adventure.text.minimessage.translation.Argument` directly).
- `adventure-i18n-json` (package `gg.cubix.adventurei18n.json`) — optional add-on, depends on
  `adventure-i18n-core` (`api` — `JsonLangFileFormat` implements core's `LangFileFormat`) and
  `gson` (`implementation`), provides only `JsonLangFileFormat`. Not yet built
  (`docs/roadmap.md` section 13).

Root `build.gradle.kts` is a parent aggregator only — it has no sources or plugins of its own, just
shared `group`/`version`/repository/publishing config for the subprojects.

All dependency versions live in `gradle/libs.versions.toml`. No inline version strings in a
module's `build.gradle.kts` — add a new entry to the catalog instead, even for a dependency used by
only one module. See [ADR-0002](./docs/decisions/0002-gradle-version-catalog-for-dependency-management.md)
for why.

[`docs/decisions/`](./docs/decisions) holds ADRs for load-bearing decisions like the module split
([ADR-0001](./docs/decisions/0001-two-module-split-core-and-json.md)) — read one before
second-guessing the decision it covers, and write a new one before overriding it.

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
- **Named placeholders never parse user-supplied text as MiniMessage.** Upstream
  `Argument.string(name, value)` wraps the value in `Component.text(...)` — verbatim, so
  player-controlled strings (e.g. display names) can't inject colors/click events. Don't build a
  parallel mechanism that re-parses it (see ADR-0004).
- **Fail-fast only where silence would be worse:** a missing fallback-locale file aborts at build
  time; an unrecognized file name in the scanned directory is just skipped and logged once (not a
  boot abort, since stray non-lang files may legitimately be present).

## Explicit non-goals (do not implement without discussion)

- No ICU/CLDR plural rules or `MessageFormat` — MiniMessage choice tags + `Argument.numeric` are
  considered sufficient for v1.
- No YAML lang file format in v1 (only the `LangFileFormat` interface needs to allow for it later).
- No Bukkit/Velocity/Minestom platform adapter classes in the library — `Messages.install` is
  already platform-neutral via `GlobalTranslator`; a platform adapter, if ever needed, is a separate
  add-on module, not core. `adventure-i18n-minestom-demo` doesn't contradict this: it's a runnable,
  unpublished example server that *consumes* both published modules, not a library adapter class —
  see its own `README.md`.
