# 0002: Gradle version catalog for dependency management

Status: Accepted

## Context

`adventure-i18n` is a multi-module Gradle build (`adventure-i18n-core`, `adventure-i18n-json`,
[ADR-0001](./0001-two-module-split-core-and-json.md)) with dependencies that either need to stay in
lockstep (both modules' JUnit setup) or need a single obvious place to bump when a new Adventure
release ships. Without a single source of truth for versions, each module's `build.gradle.kts` would
pin its own version strings, and nothing would stop `adventure-i18n-core`'s and
`adventure-i18n-json`'s JUnit versions from quietly drifting apart from each other.

This also matches the established convention in a sibling project of the same org
(`cloud-minestom`'s own `docs/decisions/0004-gradle-version-catalog-for-dependency-management.md`) —
reusing a proven pattern rather than inventing a second convention for the same problem.

## Decision

Every dependency version is declared exactly once in `gradle/libs.versions.toml`, using
[Gradle's built-in version catalog](https://docs.gradle.org/current/userguide/platforms.html). No
module's `build.gradle.kts` contains an inline version string — including a dependency used by only
one module (`gson`, currently only consumed by `adventure-i18n-json`).

## Consequences

- One place to bump a version — a one-line diff in the catalog instead of a grep-and-replace across
  both modules' build files.
- Type-safe `libs.*` accessors catch a mistyped dependency coordinate at configuration time rather
  than at dependency resolution.
- Slightly more ceremony to add a dependency used by only one module (it still needs a catalog
  entry). Accepted: the consistency this buys is worth the extra line for the rare single-use
  dependency.

## References

- [`libs.versions.toml`](../../gradle/libs.versions.toml)
- [`CLAUDE.md`](../../CLAUDE.md), "All dependency versions live in `gradle/libs.versions.toml`"
- [ADR-0001](./0001-two-module-split-core-and-json.md) (the module split this catalog spans)
- `cloud-minestom`'s own `docs/decisions/0004-gradle-version-catalog-for-dependency-management.md` —
  the precedent this decision reuses
