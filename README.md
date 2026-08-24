# adventure-i18n

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](./LICENSE)

A generic Adventure/MiniMessage translation library for Minecraft server projects (Paper, Velocity,
Minestom, Sponge). It separates the generic core that every such project ends up writing anyway —
per-locale lang file loading, fallback resolution, `GlobalTranslator` registration, `<tag>` placeholder
resolution against a color palette — from the three points that are usually hardcoded instead of
configured: the set of supported locales, the translator's namespace and color palette, and the lang
file format. See [`spec.md`](./docs/spec.md) for the full design and the reasoning behind each of
those.

## Status

[`spec.md`](./docs/spec.md) is the complete design, [`roadmap.md`](./docs/roadmap.md) is the
implementation checklist. Both modules' public APIs are implemented and tested through section 13
of the roadmap (locale discovery, fallback, color palette, `<prefix>` splicing, `KeyedTranslator`,
`Messages`, `LangFileConsistency`, and the optional `JsonLangFileFormat`). Section 14 (Javadoc
completeness, this README's quickstart, a versioning/publishing decision) is what's left before v1.
Nothing is published to a repository yet, so this isn't usable as a dependency in another project.

## Modules

| Module | Purpose |
|---|---|
| `adventure-i18n-core` | the library — locale discovery, fallback, `KeyedTranslator`, `Messages`; depends only on `adventure-api`, `adventure-text-minimessage`, `slf4j-api` |
| `adventure-i18n-json` | optional add-on providing `JsonLangFileFormat` for consumers who want JSON instead of the default `.properties` format |
| `adventure-i18n-minestom-demo` | runnable [Minestom](https://minestom.net) server demonstrating both modules end to end - not published, not part of the library's public API, see its own [`README.md`](./adventure-i18n-minestom-demo/README.md) |

Two modules, not one, so that depending on `adventure-i18n-core` alone never pulls in a JSON
library, not even transitively — the default `.properties` format needs nothing beyond the JDK
itself. A project that wants JSON lang files adds `adventure-i18n-json` explicitly; everyone else
never sees Gson on their classpath. See [ADR-0001](./docs/decisions/0001-two-module-split-core-and-json.md)
for the full reasoning.

## Usage

Not published yet, so there's no coordinate to depend on - but the shape below already works
against this repository's current `adventure-i18n-core`. Setup is the same either way:

```java
Key namespace = Key.key("myproject", "i18n");

KeyedTranslator translator = KeyedTranslator.builder(namespace)
    .source(ClasspathLangSource.scanning("lang"))
    .fallback(Locale.US)
    .build();

Messages.install(translator);
```

From there, two ways to render a message, depending on who decides the recipient's locale:

**Client-reported locale (the default)** — a raw `Component.translatable(...)`, resolved
automatically against whatever locale the recipient's own client reports:

```java
player.sendMessage(Messages.render("myproject.welcome", Argument.string("player", player.getUsername())));
```

**A project-managed locale** (e.g. stored per player in a database, so switching Minecraft clients
doesn't change it) — resolved eagerly, via a `LocaleSource`:

```java
LocaleSource locales = playerId -> Optional.ofNullable(dbBackedLocales.get(playerId));

player.sendMessage(Messages.render(
    locales, player.getUniqueId(), "myproject.welcome", Argument.string("player", player.getUsername())));
```

Whoever uses their own `LocaleSource` for a recipient **must** use this overload for them, not the
client-locale one above - the client's own reported locale would otherwise win again via
`GlobalTranslator`. Both paths work side by side, decided per message, not a project-wide choice.

`Argument` is upstream Adventure's own
`net.kyori.adventure.text.minimessage.translation.Argument` - there's no `Args`/`Placeholders`
class in this library (see [ADR-0004](./docs/decisions/0004-no-custom-args-class.md)). See
[`spec.md`](./docs/spec.md)'s "Core classes in detail" section for the full API surface.

## Documentation

- [`spec.md`](./docs/spec.md) — the full design and every decision, with rationale
- [`roadmap.md`](./docs/roadmap.md) — implementation checklist
- [`CLAUDE.md`](./CLAUDE.md) — architecture principles and non-negotiables
- [`docs/decisions/`](./docs/decisions) — ADRs for load-bearing decisions
- [`CONTRIBUTING.md`](./CONTRIBUTING.md) — workflow, commit/branch/PR conventions

## Development

```bash
./gradlew build   # build + test
```

See [`CONTRIBUTING.md`](./CONTRIBUTING.md) for the full workflow (roadmap-driven, one item per
commit/PR) and [`CLAUDE.md`](./CLAUDE.md) for architecture rules.

## Contributing

See [`CONTRIBUTING.md`](./CONTRIBUTING.md). Short version: pick the next unchecked box in
`roadmap.md`, one item per commit/PR, Conventional Commits.

## A note on how this project is being built

The design in `spec.md`, the checklist in `roadmap.md`, and this repository's scaffolding were put
together with [Claude Code](https://claude.com/claude-code), as a deliberate experiment in carrying a
fully-specified library from concept to implementation with an AI coding agent. The concept was
worked out and reviewed before any implementation started, and the same bar applies regardless of how
the code gets written: full implementation over partial, tested, documented. `roadmap.md` is what
keeps that honest, item by item.

## License

[MIT](./LICENSE)
