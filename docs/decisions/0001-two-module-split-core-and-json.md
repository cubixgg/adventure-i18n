# 0001: Two-module split — `adventure-i18n-core` and `adventure-i18n-json`

Status: Accepted

## Context

`spec.md`'s "Module and package layout" section identifies the recurring failure mode this library
exists to avoid: a project's hand-rolled i18n class ends up welded to one specific JSON library for
its lang files, even though the default case — a flat `key -> String` object — needs no third-party
parser at all. `java.util.Properties`/`PropertyResourceBundle` already read that shape, in UTF-8, with
no dependency beyond the JDK itself.

If `adventure-i18n` shipped as a single artifact with an optional Gson dependency (`compileOnly`, or a
runtime check), every consumer would still see Gson on their dependency tree metadata, and "fewer
dependencies" would be a documentation claim rather than something Gradle actually enforces.

## Decision

Two Gradle modules, two published artifacts:

- `adventure-i18n-core` — depends only on `adventure-api` (`api`), `adventure-text-minimessage`
  (`api`), `slf4j-api` (`implementation`). Contains `PropertiesLangFileFormat` as the default
  `LangFileFormat`, alongside every other core class (`LocaleCodes`, `LangSource`,
  `FallbackStrategy`, `TagPalette`, `KeyedTranslator`, `Messages`, ...).
- `adventure-i18n-json` — depends on `adventure-i18n-core` (`api` — `JsonLangFileFormat` implements
  core's `LangFileFormat` interface, so core's types are part of this module's own public API
  surface) and `gson` (`implementation`). Provides only `JsonLangFileFormat`.

A consumer who never asks for `adventure-i18n-json` never sees Gson at all, not even transitively.

## Consequences

- "No JSON library, not even transitively" (`docs/roadmap.md` section 1) is a Gradle-enforced
  property of `adventure-i18n-core`'s dependency graph, not just a claim in prose.
- A future third `LangFileFormat` (YAML is the explicitly named non-goal-for-now case in `spec.md`)
  has this split as its template: its own module, its own single dependency, `api`-depending on core.
- Two modules to publish and version instead of one — accepted, since the alternative (a single
  artifact quietly depending on Gson for every consumer) is exactly the coupling this library exists
  to avoid.
- `adventure-i18n-json`'s tests exercise `JsonLangFileFormat` in isolation from `PropertiesLangFileFormat`
  (`docs/roadmap.md` section 13) — the two formats never share test fixtures beyond the common
  `LangFileFormat` contract.

## References

- [`spec.md`](../spec.md), "Module and package layout" and "Core classes in detail" → `LangFileFormat`
- [`roadmap.md`](../roadmap.md) sections 1 and 13
- [`CLAUDE.md`](../../CLAUDE.md), "Architecture (per docs/spec.md)"
