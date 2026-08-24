# Contributing

Thanks for looking at contributing to `adventure-i18n`. Before making changes, skim:

- [`spec.md`](./docs/spec.md) – full design and rationale for the library
- [`roadmap.md`](./docs/roadmap.md) – implementation status and what to work on next
- [`CLAUDE.md`](./CLAUDE.md) – architecture principles and non-negotiables
- [`docs/`](./docs) – user-facing documentation, once it exists, [`docs/decisions/`](./docs/decisions) –
  ADRs for load-bearing decisions

## Setup

- `./gradlew build` builds and tests every module (`adventure-i18n-core`, `adventure-i18n-json`)
- No JDK toolchain is pinned yet — still an open item in
  [`roadmap.md`](./docs/roadmap.md)'s project/build setup section

See [`README.md`](./README.md) for more on local development.

## Workflow

- **Every change is a commit, every commit goes through a pull request.** There is no direct commit to
  `main` — not for a one-line fix, not for a docs typo, not for a version bump. Work happens on a
  branch and lands through a PR, every time, no exceptions carved out for "it's trivial."
- Pick the next unchecked item in `roadmap.md`. Phase order is the recommended build/test order, not
  a strict dependency.
- **One item = one commit.** Don't fold multiple checkboxes into one commit, even if both are small and
  related — each commit should be reviewable and revertable on its own. If an item turns out to be too
  big for one commit, split it into further sub-items in the roadmap instead.
- **PRs are grouped per roadmap section, not per item.** A section's commits land on one branch and go
  out as one PR once the section (or a sensible chunk of it) is done — not a PR per checkbox. Use
  judgment on where a section's PR boundary actually falls; a section that's naturally two reviewable
  halves can be two PRs, but don't default to the smallest possible PR.
- Check the box in `roadmap.md` in the same commit as the implementation — never leave it out of sync,
  even briefly.
- If implementing an item reveals a real gap or correction in `spec.md`, fix `spec.md` in the same
  commit and say so in the commit message.
- This is meant to be a full implementation, not a partial one. A roadmap item isn't done because it
  compiles — it's done when it's tested (see Testing below) and documented where the item calls for it.

## Commit messages

Commits follow [Conventional Commits](https://www.conventionalcommits.org/):
`<type>(<scope>): <description>`. There's no release automation set up yet, but keeping this
consistent from the start keeps the option open later, so it's not just style.

- Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`.
- Scope = the module touched where useful, e.g. `feat(adventure-i18n-core): ...`,
  `feat(adventure-i18n-json): ...`. Omit it for repo-wide changes.
- Breaking change: `!` after the type/scope (`feat(adventure-i18n-core)!: ...`) or a
  `BREAKING CHANGE:` footer — never just a description that happens to mention it.
- Each commit's type reflects what *that* commit does, not the theme of the overall PR: adding a
  builder option is `feat`, fixing a stale Javadoc is `fix` or `docs`, adding a missing test for
  already-merged behavior is `test`.

## Branch naming

`<username>/<type>/<short-description>`, e.g. `marlon/feature/classpath-lang-source`. `<type>` is the
Conventional Commits type spelled out: `feature`, `fix`, `chore`, `docs`, `refactor`, `test`, `build`,
`ci`, `perf`.

## Pull requests

- Base branch: `main`. `main` only ever receives merged PRs, never a direct push.
- One PR per roadmap section (or sensible chunk of one), not per item (see Workflow above).
- Make sure `./gradlew build` passes before opening.

## Testing

Per `spec.md`'s "Validation / testability" section:

- Every public class gets unit tests, run via `./gradlew test`.
- `TranslationIssueListener` is the seam for testing fallback/missing-key behavior — assert against a
  test double, don't scrape log output.
- Test `ClasspathLangSource` scanning against **both** classpath shapes: an exploded resource
  directory and a real built jar. Jar scanning is called out in the spec as the part with the
  greatest platform-dependent risk — don't settle for only the exploded-directory case.
- `LangFileConsistency` (once implemented) should itself be exercised as a JUnit assertion against
  both a consistent and an inconsistent fixture bundle.

## Key architectural rules

Full detail and rationale in `CLAUDE.md` and `spec.md`; the short version:

- No bundled/enumerated locale list anywhere — a language exists because a lang file for it was
  found, never because it's registered in code.
- `FallbackStrategy` decides which regional variant serves a request, explicitly and
  deterministically — never implicitly via classpath scan/read order.
- A project using its own `LocaleSource` must render eagerly via `Messages.render(locales, ...)` and
  must never send a raw `Component.translatable(...)` for that recipient — the client's own locale
  would silently win again via `GlobalTranslator`.
- `Args` never parses user-supplied text as MiniMessage — text placeholders are always inserted
  verbatim.
- Fail fast only where silence would be worse: a missing fallback-locale bundle aborts at build time;
  an unrecognized file name in a scanned directory is skipped and logged once, not a boot abort.

## Documentation

- User-facing docs go in `docs/`, written for someone who already knows Adventure/MiniMessage and
  wants to use this library, not a re-explanation of Adventure itself.
- Architecture Decision Records go in `docs/decisions/`, one file per decision, numbered
  sequentially. Write one only for a decision a future contributor could plausibly second-guess
  without the context — not for every roadmap item.
- If a roadmap item changes what an existing `docs/` page (or `spec.md`) says, update that page in
  the same commit.
