# 0005: Reposilite + release-please for Maven publishing

Status: Accepted

## Context

`roadmap.md`'s section 14 left the versioning/publishing decision deliberately open: neither Maven
Central's Sonatype namespace/GPG signing setup, nor a self-hosted alternative, had been chosen, so
CI/release automation was held off until a real target existed to build against. `adventure-i18n-core`
and `adventure-i18n-json` are the two published artifacts (see
[ADR-0001](./0001-two-module-split-core-and-json.md)); `adventure-i18n-minestom-demo` is a runnable
example server, not a consumable dependency, and stays unpublished (see its own `README.md`).

That target now exists: this project publishes to a self-hosted [Reposilite](https://reposilite.com/)
instance at `https://maven.cubix.gg/public-releases`, the same pattern already proven out in a sibling
`gg.cubix` project (`cloud-minestom`, itself following an internal precedent for exactly this kind of
library artifact). `REPOSILITE_USERNAME`/`REPOSILITE_PASSWORD` are already configured as repo secrets.

Versioning needed a single source of truth that couldn't drift from what's actually tagged and
published. [`release-please`](https://github.com/googleapis/release-please) computes the next semver
from Conventional Commits (already this repo's required commit format, `CONTRIBUTING.md`) and opens a
release PR with the version bump and a generated `CHANGELOG.md` entry; merging that PR is what
triggers an actual release.

## Decision

- **Single repo-wide version**, not per-module: root `build.gradle.kts`'s
  `subprojects { version = "0.1.0" // x-release-please-version }` line is release-please's
  `extra-files` generic target (a plain find-and-replace on that annotated line).
- **Only `adventure-i18n-core` and `adventure-i18n-json` publish.** No explicit exclusion set is
  needed for `adventure-i18n-minestom-demo`: publishing is scoped via
  `plugins.withType<JavaLibraryPlugin>` in root `build.gradle.kts`, and the demo module applies
  `application` (+ shadow), never `java-library`, so it never enters that block.
- **Release-triggered only, no per-commit snapshot publishing.**
  `.github/workflows/release-please.yml` opens/updates a release PR against `main` on every push, and
  only runs `./gradlew publish` when merging that PR actually creates a release (`release_created`
  output) - not on every commit. Reposilite has no built-in artifact retention/eviction (only a
  storage-quota rejection), so per-commit snapshots would need a retention policy decided first, and
  there's no consumer needing pre-release artifacts yet.
- **`REPOSILITE_USERNAME`/`REPOSILITE_PASSWORD` repo secrets**, read via `System.getenv(...)` in the
  `maven-publish` repository credentials block - not committed, not defaulted; the `publish` job
  simply fails without them.
- `.github/workflows/build.yml` runs `./gradlew build` on every push to `main` and every PR - separate
  from the release workflow, so a broken build is caught before a release PR would ever be opened.

## Why not what was originally considered

Maven Central requires a registered Sonatype namespace and GPG-signed artifacts - real infrastructure
this project doesn't have set up, and no consumer outside this org needs Central specifically today.
Reposilite is already the org's standing choice for exactly this kind of internal library artifact, so
reusing it instead of standing up a second publishing target was the pragmatic call. If a genuine
public/Central-distribution need shows up later, that's a new decision to make then, not a default to
guess at now.

## Consequences

- `README.md`'s "Status" and "Usage" sections are updated in the same commit as this ADR to describe
  the Reposilite coordinate instead of "nothing published yet", and `roadmap.md` section 14's last box
  is checked.
- `CHANGELOG.md` is not hand-seeded: release-please generates it itself from Conventional Commit
  history the first time it opens a release PR.
- The repo's merge button is restricted to squash-merge only (merge commit and rebase-merge disabled)
  as part of this change - a regular merge commit's auto-generated message repeats the PR title on
  its second line, which release-please's Conventional Commits parser would read as a second,
  duplicate changelog entry for the same change. `CONTRIBUTING.md`'s Pull requests section now says
  so explicitly.
- A snapshot-publishing pipeline, if ever added, needs a Reposilite retention policy decided first.

## References

- [`roadmap.md`](../roadmap.md) section 14 (Documentation & release readiness)
- [`CONTRIBUTING.md`](../../CONTRIBUTING.md) (Conventional Commits, PR-only workflow)
- [ADR-0001](./0001-two-module-split-core-and-json.md) (which modules are the published artifacts)
- `cloud-minestom`'s own `docs/decisions/0006-reposilite-release-please.md` - the proven-out pattern
  this decision reuses
