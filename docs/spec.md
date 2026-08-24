# Concept: `adventure-i18n` — a generic Adventure/MiniMessage translation library

## Problem statement

Many Adventure/MiniMessage-based Minecraft projects (Paper, Velocity, Minestom, Sponge) end up
writing the same small piece of infrastructure sooner or later: a class that reads lang files per
locale, keeps them in a map, registers itself as a `GlobalTranslator` source, and resolves `<tag>`
placeholders against a project-specific color palette. That is generic in principle — plain
Adventure/MiniMessage, no platform dependency — but in practice it usually ends up welded to the one
project that needed it first, at three specific points:

1. A static, hand-maintained list of every supported locale, which turns every new language into a
   code change and which needs to know upfront, at boot, which files must exist.
2. The translator class has the namespace `Key` and the color palette hardcoded instead of
   configured.
3. The file format (usually JSON via a specific JSON library) isn't swappable, even though parsing
   a flat `key -> String` object is trivial without a third-party library at all.

Goal of this concept: cleanly separate the generic core (locale resolution, fallback, MiniMessage
rendering, palette mechanics) from exactly these three fixed points, so it can live as a
standalone, project-independent open-source library.

## Guiding decision: automatic locale discovery instead of enumeration

The obvious but inflexible approach is a fixed constant like `SUPPORTED_LOCALES = List.of(EN_US,
DE_DE, …)`, against which every lang file is then checked — a missing file aborts the boot; a new
language has to be added to the constant first before its file does anything at all.

Instead: a `LangSource` scans a directory (classpath resource or filesystem) and derives the locale
from every file name it finds (`de_de.properties` → `Locale.of("de","DE")`). There is no longer any
single place where a language must be registered — a new file in the directory *is* the
registration. The one required piece of configuration is the **fallback locale**, and that is
validated against the bundles that were actually found at build time (fail fast if the fallback
file is missing — the one case where silently losing a language is worse than a boot error).

```java
LangSource source = ClasspathLangSource.scanning("lang"); // finds anything shaped like a locale id
Locale fallback = Locale.US;                               // must be among the discovered bundles
```

## Module and package layout

Two artifacts instead of one, so that "fewer dependencies" is enforced rather than just intended:
whoever pulls only `adventure-i18n-core` gets **no** third runtime dependency beyond
Adventure/SLF4J — not even transitively.

```
adventure-i18n-core/
└── src/main/java/yourorg/i18n/
    ├── LocaleCodes.java              // Locale <-> "en_us"-style id, parsing + rendering
    ├── LangFileFormat.java           // pluggable parser interface
    ├── PropertiesLangFileFormat.java // default implementation, java.util.Properties only
    ├── LangSource.java               // functional interface: Map<Locale, Map<String,String>> load()
    ├── ClasspathLangSource.java      // classpath auto-discovery (file and jar URLs)
    ├── FallbackStrategy.java         // pluggable: which locale serves which gap
    ├── LanguageVariantFallback.java  // default: exact -> same language -> fixed fallback locale
    ├── TranslationIssueListener.java // seam for missing/fallback-served keys
    ├── LoggingTranslationIssueListener.java // default implementation, deduplicated via SLF4J
    ├── TagPalette.java               // generic tag-name -> color object, filled in by the caller
    ├── PrefixPolicy.java             // optional <prefix> splice, configurable key/tag
    ├── KeyedTranslator.java          // MiniMessageTranslator implementation, assembled via builder
    ├── LocaleSource.java             // swappable per-recipient locale source (e.g. a database instead of the client)
    ├── Messages.java                 // install/uninstall/render (+ LocaleSource overload)
    └── Args.java                     // named placeholders for render calls

adventure-i18n-json/            (optional add-on module, separate artifact)
└── src/main/java/yourorg/i18n/json/
    └── JsonLangFileFormat.java       // JSON variant for consumers who need JSON instead of
                                       // Properties for some other reason (e.g. reusing the same
                                       // file in another tool)
```

Dependencies of `adventure-i18n-core`: `adventure-api`, `adventure-text-minimessage`, `slf4j-api` —
**no Gson, no Jackson, no JSON library at all**. The default parser is `PropertiesLangFileFormat`,
built on `java.util.Properties`. Note that only `PropertyResourceBundle` (via
`ResourceBundle.getBundle`, JEP 226) defaults to UTF-8 since Java 9 — `Properties#load(InputStream)`
itself still assumes ISO-8859-1, since `ClasspathLangSource` hands `LangFileFormat` a raw
`InputStream`, not a `ResourceBundle`. `PropertiesLangFileFormat` therefore explicitly wraps the
stream in a UTF-8 `InputStreamReader` before calling `Properties#load(Reader)`, so UTF-8 lang files
decode correctly regardless of JDK version rather than relying on that JEP 226 default. A line like
`key = <error>Score: <points></error>` reads correctly: only the *first*
unescaped `=`/`:`/whitespace separates key from value, everything after it — including further `:`
or `<`/`>` characters — stays part of the value unchanged. Escaping only matters for backslashes and
leading whitespace in the value; that belongs in `PropertiesLangFileFormat`'s class documentation,
so nobody has to wonder why a `\` in a path placeholder needs doubling.

`adventure-i18n-json` additionally pulls in Gson and provides only `JsonLangFileFormat`. Whoever
doesn't need it never sees the artifact.

## Core classes in detail

### `LocaleCodes`

Parses and renders locale identifiers in the Minecraft-style `en_us` form (`Locale` itself prints
`en_US`). A regex validation (`[A-Za-z]{2,8}([_-][A-Za-z0-9]{2,8})?`) guards against arbitrary
player input (e.g. from a `/lang` command) producing a locale that resolves to nothing — Adventure's
own parser is too lenient for that.

```java
public final class LocaleCodes {
    public static Locale parse(String id);   // null on an invalid format
    public static String id(Locale locale);  // "en_us"-style
}
```

No bundled locale list — which languages exist is decided entirely by which files a `LangSource`
finds.

### `LangSource` + `ClasspathLangSource`

```java
public interface LangSource {
    Map<Locale, Map<String, String>> load();
}
```

`ClasspathLangSource` is the standard implementation, in two shapes:

```java
ClasspathLangSource.scanning("lang");                              // default class loader, PropertiesLangFileFormat
ClasspathLangSource.scanning(loader, "lang", new PropertiesLangFileFormat()); // explicit
```

Scanning logic: `ClassLoader#getResources(directory)` returns one URL per classpath root that
contains the directory. For each URL:

- a `file:` URL (exploded classes / IDE run) → list the directory, check every file against a
  `<identifier>.<extension>` pattern.
- `jar:file:...!/lang` → open the jar as a `JarFile`, filter entries with the `lang/` prefix and the
  extension `LangFileFormat` declares.

A file name that doesn't look like a locale id (`LocaleCodes.parse` returns `null`) is skipped and
logged once — no boot abort, because a directory may well contain a stray `README.md`. Which
regional variant represents a language (e.g. `de_DE` for a client on `de_AT`) is deliberately *not*
decided by scan/read order, but is an explicit rule of the `FallbackStrategy`.

`LangFileFormat` is deliberately small:

```java
public interface LangFileFormat {
    String fileExtension();                              // "properties" (default) or "json"
    Map<String, String> parse(InputStream in, String sourceName);
}
```

`PropertiesLangFileFormat` (core, default) reads via `java.util.Properties` /
`PropertyResourceBundle` — flat by definition, non-string values simply can't be expressed in this
format at all. `JsonLangFileFormat` (optional `adventure-i18n-json` module) parses a flat JSON
object and throws a hard error on nested values or non-strings, instead of silently dropping keys —
a broken lang file should fail at boot, not surface as raw translation keys in front of players.

### `FallbackStrategy`

The actual locale resolution as a swappable object: exact locale → regional variant of the same
language → fixed fallback locale → nothing found. Swappable so that a project can, for instance,
plug in a pure fallback chain (`de_AT → de_DE → en_US`) without touching the translator itself:

```java
public interface FallbackStrategy {
    Optional<Locale> resolve(Locale requested, Set<Locale> available);
}
```

`LanguageVariantFallback` is the default implementation with the behaviour described above; which
regional variant represents a language is sorted explicitly (e.g. alphabetically, or via a priority
list on the builder), never decided implicitly by read order.

### `TagPalette`

Separates two things that often get merged into one: (a) *a specific* color palette, (b) the
generic mechanism "tag name → color, plus a MiniMessage parser that knows about it". (b) is the
library, (a) stays with the project:

```java
public final class TagPalette {
    public static TagPalette of(Map<String, TextColor> tags);
    public TagResolver resolver();
    public MiniMessage miniMessage();     // MiniMessage.builder().editTags(...).build()
    public TextColor color(String tag);
}
```

A project just writes its own constants and calls `TagPalette.of(Map.of("error", ERROR, "success",
SUCCESS, …))` — the library has no notion of "error" or "success" as concepts.

### `PrefixPolicy`

A recurring pattern: a `<prefix>` tag in lang templates whose value is itself a translation (e.g.
`[ProjectName]`), spliced in textually *before* MiniMessage parses — so the prefix's own tags get
parsed in the same pass, with a guard against self-reference in case the prefix key itself contains
`<prefix>`. That's clever enough to be worth keeping generic — only the key name and tag string
become configurable instead of hardcoded:

```java
public record PrefixPolicy(String templateKey, String tag) {
    public static PrefixPolicy of(String key, String tag) { … }
}
```

Optional on the builder — a project without "one word in front of every chat line" simply leaves it
out.

### `KeyedTranslator`

A `MiniMessageTranslator` implementation, thread-safe (lookups can happen from tick and IO threads
alike), assembled via a builder instead of hardcoding a namespace and palette:

```java
Key namespace = Key.key("myproject", "i18n");

KeyedTranslator translator = KeyedTranslator.builder(namespace)
    .source(ClasspathLangSource.scanning("lang"))
    .fallback(Locale.US)                         // must be among the discovered bundles, or IllegalStateException
    .fallbackStrategy(new LanguageVariantFallback()) // optional, already the default
    .miniMessage(myPalette.miniMessage())        // optional, default: MiniMessage.miniMessage()
    .prefix(PrefixPolicy.of("myproject.prefix", "<prefix>")) // optional
    .issues(TranslationIssueListener.logging())  // optional, default: logging()
    .build();

Messages.install(translator);
```

`register(Locale, Map<String,String>)` allows extension bundles to be loaded at runtime (e.g. from
plugins that bring their own keys) — keys that already exist for that locale get overwritten.

### `LocaleSource` — making locale origin swappable

Without any further effort you end up with the usual Adventure behaviour: a raw
`Component.translatable(key, args)` gets resolved on send, automatically, against the **locale
reported by the client** (`GlobalTranslator` reads the locale off the `Audience`, and on
Minestom/Paper/Velocity that is exactly the client's own setting). For a project that wants to
manage language itself instead (a player picks it once, it lands in a database, switching Minecraft
clients doesn't change it), the library needs to make that automatism **bypassable**, not replace
it.

The core only needs one small, platform-free interface for that:

```java
public interface LocaleSource {
    Optional<Locale> localeOf(UUID recipientId);
}
```

No reference to `Player`/`Audience` — just a UUID, because that's the one identifier every
Minecraft project already has. A project implements this against its own cache (populated from the
database on login, updated on an explicit language change) — the library prescribes neither caching
nor database access, that stays the consumer's job. A `LocaleSource` that runs a database query per
message is a bug in the consumer, not a problem of the library — but that belongs explicitly in the
class documentation, so nobody builds it wrong.

The decisive consequence for the send path: **whoever uses their own `LocaleSource` must never send
the player a raw `Component.translatable(...)`** (the client locale would win again via
`GlobalTranslator`), and must instead resolve eagerly on the server. `Messages` gets a third
overload for that, built on top of `LocaleSource`:

```java
public static Component render(LocaleSource locales, UUID recipientId, String key, ComponentLike... args) {
    Locale locale = locales.localeOf(recipientId).orElse(KeyedTranslator.systemFallback());
    return render(locale, key, args); // the already-existing eager variant
}
```

A project with its own player API simply instantiates its `LocaleSource` implementation (e.g.
`dbBackedLocales::get`) and consistently calls this overload instead of relying on the client-locale
automatism. Whoever wants the client locale instead just does nothing — `Messages.install(translator)`
plus `player.sendMessage(Messages.render(key, args))` remains the default path and keeps working
unchanged. Both paths are therefore usable side by side, decidable per message, not an
exclusive project-wide choice.

### `Messages` and `Args`

`Messages` installs/uninstalls the translator on the `GlobalTranslator` and either builds a
`TranslatableComponent` (resolved lazily) or renders immediately against an explicit locale. `Args`
provides named placeholders (text, number, boolean, nested components) — text values are always
inserted verbatim, never parsed as MiniMessage, so player input (e.g. a display name) can never
inject colors or click events. Both are already fully platform- and project-independent and carry
over into the library essentially unchanged; `Args` could optionally be renamed to `Placeholders` if
that reads more clearly outside a Minecraft context.

## Validation / testability

- `TranslationIssueListener` is the seam through which fallback/error behaviour is testable without
  scraping log output.
- A generic `LangFileConsistency` utility: "do all discovered locales have the same key set as the
  fallback locale?", usable as a JUnit assertion or a Gradle task in any project that adopts the
  library.
- Test the scanning logic against **both** classpath shapes (an exploded resource directory in a
  unit test, and once against a real built jar) — jar scanning is the part with the greatest risk
  of platform-dependent behaviour.

## Non-goals (deliberately out of scope)

- No plural rules / ICU `MessageFormat` — MiniMessage choice tags plus client-side number formatting
  (`Args.number`) cover most cases; real CLDR pluralization would be a separate second effort, not
  part of v1.
- No YAML support in v1 — only provide the `LangFileFormat` interface so it can be added later
  without touching `KeyedTranslator` (Properties is the default, JSON the one planned add-on
  module).
- No Bukkit/Velocity/Minestom adapter classes in the library itself — installation
  (`Messages.install`) is already platform-neutral via `GlobalTranslator`; a per-platform adapter
  module, if one is ever needed, belongs in its own add-on module, not in the core.
