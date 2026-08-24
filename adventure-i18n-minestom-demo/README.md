# adventure-i18n-minestom-demo

A runnable [Minestom](https://minestom.net) server that exercises every public piece of
`adventure-i18n` end to end. **Not published, not part of the library's public API** - Minestom
adapters are an explicit non-goal for the library itself (see `CLAUDE.md`'s "Explicit non-goals");
this module exists purely as a working example a consumer project can read alongside the top-level
`README.md`'s quickstart.

## Running it

```
./gradlew :adventure-i18n-minestom-demo:run
```

(from the repository root - or `./gradlew :adventure-i18n-minestom-demo:shadowJar` and run the
resulting `build/libs/adventure-i18n-minestom-demo-*-all.jar` standalone.)

Requires Java 25 (Minestom's own minimum - this module pins its own toolchain, see its
`build.gradle.kts`). The server listens on `0.0.0.0:25565`; connect with a `1.21.11`-compatible
client (unauthenticated / offline-mode, like any bare Minestom server).

## What's demonstrated, and where

| Piece | Where |
|---|---|
| `TagPalette` (`accent`/`success`/`error`/`info`/`muted`) | `MinestomDemoServer`, shared by both translators |
| Two `KeyedTranslator`s, two `LangFileFormat`s at once | `demo.core` (`src/main/resources/lang`, default `PropertiesLangFileFormat`) and `demo.shop` (`src/main/resources/lang-shop`, `adventure-i18n-json`'s `JsonLangFileFormat`) - both installed on the same `GlobalTranslator`, proving the format is a per-translator choice |
| `PrefixPolicy` | every `demo.core` message splices in `demo.prefix` (`[Demo]`) |
| `LanguageVariantFallback` (regional variant) | `demo.core` bundles `en_us` (fallback), `no_no`, and `fr_fr` only - a client reporting e.g. `fr_CA` is served the `fr_fr` bundle, never `en_us` |
| `LangFileConsistency` | asserted for both translators' bundles before either is built (`MinestomDemoServer.main`) |
| A custom `TranslationIssueListener` | `DemoTranslationIssues`, wired into both translators in place of the default `.logging()` |
| `KeyedTranslator#register` (runtime bundle extension) | `demo.runtime` isn't in any lang file - it's added to the `demo.core` translator's `en_us` bundle after `build()`; try `/runtime` |
| The lazy, client-locale-automatism render path | join broadcast (`PlayerSpawnEvent`) and `/shop` - one unresolved `Component`, sent to every recipient, each rendered in their own client-reported locale by Minestom itself |
| The eager, `LocaleSource`-managed render path | `/lang` and `/score`, via `PlayerLocaleStore` - a server-managed override independent of the client's own setting |

## Try this once connected

- `/lang` - lists the discovered locales; `/lang no_no`, `/lang fr_fr` switch your **server-managed**
  override (independent of your client's own language setting); `/lang reset` clears it.
- `/score` - increments and shows a counter, rendered via the same `LocaleSource` override as `/lang`.
- `/shop`, `/shop sword` - the JSON-backed translator, rendered via your **client's** reported locale
  instead.
- `/runtime` - the key added at startup via `KeyedTranslator#register(...)`. Try it after switching
  to a language other than `en_us`/`no_no`/`fr_fr` with your client (or after `/lang reset`, on a
  client not reporting one of the three bundled locales) - it was only registered for `en_us`, so a
  request served from another exact-match bundle without that key logs a `missingKey` issue via
  `DemoTranslationIssues` instead of silently falling further back. That's intentional, not a bug -
  it's the same `missingKey` seam `TranslationIssueListener` exists for.
- Have two clients set to different languages join at once and watch the join broadcast - both see
  it in their own language from the exact same `Messages.render(...)` call.

## What this module is *not*

It isn't a template for a real server (no persistence, no permissions, no auth), and it isn't a
Minestom platform adapter for the library - see `CLAUDE.md`'s non-goals list for why the latter
isn't in scope for `adventure-i18n` itself. It exists to be read, and to be connected to.
