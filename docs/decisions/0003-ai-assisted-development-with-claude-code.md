# 0003: AI-assisted development ("vibecoding") with Claude Code

Status: Accepted

## Context

Turning `spec.md`'s design into a real, standalone library needs a fair amount of scaffolding before
the interesting design work (locale fallback, classpath jar-scanning, the `LocaleSource`/client-locale
split) pays off: a multi-module Gradle build, a version catalog, repo documentation
(`README.md`, `CONTRIBUTING.md`, `SECURITY.md`, this ADR set), and eventually two required-testing
layers per class. Building all of that by hand takes real time that would otherwise come out of the
time available for getting the design itself right.

## Decision

This project is built primarily through AI-assisted development ("vibecoding") using
[Claude Code](https://claude.com/claude-code), as a deliberate experiment: whether an AI coding agent
can carry a real, fully-specified project — not a prototype — from concept through implementation,
tests and documentation, held to the same bar a careful hand-written implementation would be held to.

This is not a shortcut around design rigor. `spec.md` — an exact concept, including its explicit
non-goals — was worked out and reviewed before any implementation started. `roadmap.md` enforces full
implementation over partial, one checkbox at a time, and `CONTRIBUTING.md`'s one-item-per-commit
workflow exists to keep each step reviewable rather than trusting output wholesale.

This mirrors the same decision already made and documented in a sibling project of the same org
(`cloud-minestom`'s own `docs/decisions/0005-ai-assisted-development-with-claude-code.md`).

## Consequences

- Significantly faster to stand up the module layout, version catalog, and initial documentation set
  than doing it by hand, freeing time for the design itself.
- Forces the design to be written down precisely before implementation, since an agent needs an
  explicit concept to build against — a discipline that benefits the project regardless of who or
  what writes the code.
- Requires deliberate review at each step rather than trusting output wholesale — `CONTRIBUTING.md`'s
  one-item-per-commit/PR workflow exists partly to keep that review tractable, not just to keep
  history tidy.
- Documented here rather than left unstated, so anyone evaluating the codebase's origin has the
  actual reasoning, not just the result.

## References

- [`README.md`](../../README.md), "A note on how this project is being built"
- [`spec.md`](../spec.md) (the concept, written before any implementation)
- [`CONTRIBUTING.md`](../../CONTRIBUTING.md) (workflow enforcing incremental, reviewable steps)
- `cloud-minestom`'s own `docs/decisions/0005-ai-assisted-development-with-claude-code.md` — the
  precedent this decision reuses
