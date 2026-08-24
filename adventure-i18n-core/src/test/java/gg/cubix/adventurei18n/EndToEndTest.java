package gg.cubix.adventurei18n;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.Argument;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the full pipeline against real lang files on the classpath
 * ({@code src/test/resources/e2e-lang}): {@code ClasspathLangSource} scanning + parsing,
 * {@code KeyedTranslator} built via its builder, prefix splicing, {@code LangFileConsistency}
 * validation, and rendering through {@code Messages} - not a synthetic in-memory {@code LangSource}
 * like the other unit tests use.
 */
class EndToEndTest {

    private static final Key NAMESPACE = Key.key("e2e", "i18n");

    @Test
    void fullPipelineFromClasspathLangFilesToRenderedComponent() {
        LangFileConsistency.assertConsistent(ClasspathLangSource.scanning("e2e-lang").load(), Locale.US);

        KeyedTranslator translator = KeyedTranslator.builder(NAMESPACE)
                .source(ClasspathLangSource.scanning("e2e-lang"))
                .fallback(Locale.US)
                .prefix(PrefixPolicy.of("server.prefix", "<prefix>"))
                .build();

        Messages.install(translator);
        try {
            Component german = Messages.render(Locale.GERMANY, "server.welcome", Argument.string("name", "World"));
            assertEquals("[MeinServer] Willkommen, World!", MiniMessage.miniMessage().serialize(german));

            // French wasn't discovered at all - falls all the way back to the US bundle.
            Component french = Messages.render(Locale.FRANCE, "server.welcome", Argument.string("name", "World"));
            assertEquals("[MyServer] Welcome, World!", MiniMessage.miniMessage().serialize(french));
        } finally {
            Messages.uninstall(translator);
        }
    }
}
