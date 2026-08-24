# 0004: No custom `Args` class - use Adventure's own `Argument` directly

Status: Accepted

## Context

`spec.md` originally sketched an `Args` class for section 11 (`Messages`/`Args`): named
placeholders (text, number, boolean, nested components), with one specific safety property called
out explicitly - text values always inserted verbatim, never parsed as MiniMessage, so a
player-controlled string (e.g. a display name) can never inject colors or click events into a
rendered message.

While implementing `KeyedTranslator` ([ADR context: `docs/spec.md`'s "`KeyedTranslator`"
section]), inspecting the actual Adventure 5.2.0 jars showed that
`net.kyori.adventure.text.minimessage.translation.Argument` (shipped in `adventure-text-minimessage`
since Adventure 4.20.0/4.21.0 - already a transitive dependency of `adventure-i18n-core`, pinned
well below the `5.2.0` this project uses) already provides exactly this:

- `Argument.bool(name, value)`, `.numeric(name, value)`, `.component(name, value)`
- `Argument.string(name, value)` - which wraps the value in `Component.text(...)` (source-verified:
  `argument(name, TranslationArgument.component(Component.text(value)))`), i.e. already
  verbatim/never-parsed-as-MiniMessage, the exact property `Args` was designed to provide

These are designed specifically to be used as `Component.translatable(key, Argument.string("player",
name), ...)` arguments for a `MiniMessageTranslator` - which is exactly what `KeyedTranslator`
extends and what `Messages.render(...)` builds.

## Decision

`adventure-i18n-core` does not ship a custom `Args` or `Placeholders` class. `Messages`'s `render`
overloads accept plain `ComponentLike... args`, and a project builds named placeholders with
upstream `Argument`'s own static factories directly.

## Consequences

- No parallel API that just re-exports upstream's own mechanism under a different name - avoids the
  "why are there two argument classes that do the same thing" question a wrapper would invite.
- The verbatim-text safety guarantee is inherited for free from upstream (and from upstream's own
  test coverage) rather than reimplemented and separately tested here.
- One less public class to design, document and version - `Messages` is genuinely all this section
  needed to add.
- If upstream's `Argument` ever turns out to be insufficient for a genuine adventure-i18n-specific
  need (not just a naming preference), that's a new ADR to introduce a wrapper then - not a default
  taken now.

## References

- [`spec.md`](../spec.md), "`KeyedTranslator`" and "`Messages`" sections
- [`roadmap.md`](../roadmap.md) section 11
- `KeyedTranslatorTest#translateIntegratesUpstreamDeserializationAndArguments` - already exercises
  `Argument.string(...)` end-to-end through `KeyedTranslator`
