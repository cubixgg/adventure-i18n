package gg.cubix.adventurei18n;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    @Test
    void installAddsAndUninstallRemovesFromGlobalTranslatorSources() {
        KeyedTranslator translator = translatorWith(Key.key("test", "install"),
                Locale.US, Map.of("greeting", "Hello"));

        assertFalse(isInstalled(translator));
        Messages.install(translator);
        try {
            assertTrue(isInstalled(translator));
        } finally {
            Messages.uninstall(translator);
        }
        assertFalse(isInstalled(translator));
    }

    @Test
    void lazyRenderBuildsAnUnresolvedTranslatableComponent() {
        Component component = Messages.render("greeting", Argument.string("name", "World"));

        assertTrue(component instanceof TranslatableComponent);
        assertEquals("greeting", ((TranslatableComponent) component).key());
    }

    @Test
    void eagerRenderResolvesImmediatelyAgainstExplicitLocale() {
        KeyedTranslator translator = translatorWith(Key.key("test", "eager"),
                Locale.US, Map.of("greeting", "Hello <name>!"));

        Messages.install(translator);
        try {
            Component result = Messages.render(Locale.US, "greeting", Argument.string("name", "World"));
            assertEquals("Hello World!", MiniMessage.miniMessage().serialize(result));
        } finally {
            Messages.uninstall(translator);
        }
    }

    @Test
    void localeSourceRenderUsesTheResolvedLocaleWhenPresent() {
        KeyedTranslator translator = KeyedTranslator.builder(Key.key("test", "locale-source-present"))
                .source(() -> Map.of(
                        Locale.US, Map.of("greeting", "Hello (US)"),
                        Locale.UK, Map.of("greeting", "Hiya (UK)")))
                .fallback(Locale.UK)
                .build();

        UUID recipient = UUID.randomUUID();
        LocaleSource locales = id -> Optional.of(Locale.UK);

        Messages.install(translator);
        try {
            Component result = Messages.render(locales, recipient, "greeting");
            assertEquals("Hiya (UK)", MiniMessage.miniMessage().serialize(result));
        } finally {
            Messages.uninstall(translator);
        }
    }

    /**
     * When the {@link LocaleSource} has no opinion, {@code Messages} must fall back to
     * {@link KeyedTranslator#systemFallback()} (Locale.US) - not to whatever fallback locale the
     * installed translator itself happens to be configured with. The translator here is
     * deliberately configured with a *different* fallback (UK) so the two are distinguishable.
     */
    @Test
    void localeSourceRenderFallsBackToSystemFallbackWhenSourceHasNoOpinion() {
        KeyedTranslator translator = KeyedTranslator.builder(Key.key("test", "locale-source-empty"))
                .source(() -> Map.of(
                        Locale.US, Map.of("greeting", "Hello (US, systemFallback)"),
                        Locale.UK, Map.of("greeting", "Hiya (UK, translator fallback)")))
                .fallback(Locale.UK)
                .build();

        LocaleSource locales = id -> Optional.empty();

        Messages.install(translator);
        try {
            Component result = Messages.render(locales, UUID.randomUUID(), "greeting");
            assertEquals("Hello (US, systemFallback)", MiniMessage.miniMessage().serialize(result));
        } finally {
            Messages.uninstall(translator);
        }
    }

    private static KeyedTranslator translatorWith(Key namespace, Locale fallback, Map<String, String> bundle) {
        return KeyedTranslator.builder(namespace)
                .source(() -> Map.of(fallback, bundle))
                .fallback(fallback)
                .build();
    }

    private static boolean isInstalled(Translator translator) {
        for (Translator source : GlobalTranslator.translator().sources()) {
            if (source == translator) {
                return true;
            }
        }
        return false;
    }
}
