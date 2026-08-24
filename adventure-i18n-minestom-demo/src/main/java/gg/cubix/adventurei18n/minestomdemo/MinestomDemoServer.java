package gg.cubix.adventurei18n.minestomdemo;

import gg.cubix.adventurei18n.ClasspathLangSource;
import gg.cubix.adventurei18n.KeyedTranslator;
import gg.cubix.adventurei18n.LangFileConsistency;
import gg.cubix.adventurei18n.LocaleCodes;
import gg.cubix.adventurei18n.Messages;
import gg.cubix.adventurei18n.PrefixPolicy;
import gg.cubix.adventurei18n.TagPalette;
import gg.cubix.adventurei18n.json.JsonLangFileFormat;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Runnable Minestom server demonstrating every public piece of {@code adventure-i18n} end to end:
 *
 * <ul>
 *   <li>{@link TagPalette} - a project-defined color palette ({@code accent}/{@code success}/
 *       {@code error}/{@code info}/{@code muted}), shared by both translators below.</li>
 *   <li>Two {@link KeyedTranslator}s installed at once, on two different lang file formats -
 *       {@code demo.core} ({@code src/main/resources/lang}, the default {@code PropertiesLangFileFormat})
 *       and {@code demo.shop} ({@code src/main/resources/lang-shop}, {@code adventure-i18n-json}'s
 *       {@code JsonLangFileFormat}) - proving the format is a per-translator choice, never a
 *       project-wide one (see {@link ShopCommand}).</li>
 *   <li>{@link PrefixPolicy} splicing a {@code <prefix>} tag into every {@code demo.core} message.</li>
 *   <li>{@link LangFileConsistency}, asserted for both translators' bundles before either is built.</li>
 *   <li>A custom {@link gg.cubix.adventurei18n.TranslationIssueListener} ({@link DemoTranslationIssues}),
 *       wired into both translators instead of the default {@code TranslationIssueListener#logging()}.</li>
 *   <li>{@link KeyedTranslator#register} - {@code demo.runtime} is added to the {@code demo.core}
 *       bundle after {@code build()}, not present in any lang file, to prove runtime registration
 *       (see {@code /runtime}).</li>
 *   <li>Both {@code Messages} render paths, side by side: the join broadcast and {@code /shop} use
 *       the lazy, client-locale automatism ({@link Messages#render(String,
 *       net.kyori.adventure.text.ComponentLike...)}); {@code /lang} and {@code /score} use the
 *       eager, {@link gg.cubix.adventurei18n.LocaleSource}-managed path
 *       ({@link Messages#render(gg.cubix.adventurei18n.LocaleSource, java.util.UUID, String,
 *       net.kyori.adventure.text.ComponentLike...)}) via {@link PlayerLocaleStore}.</li>
 * </ul>
 *
 * <p>Not part of the library's public API and not published - see this module's own {@code
 * README.md} for how to run it and what to try once connected.
 */
public final class MinestomDemoServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinestomDemoServer.class);

    private MinestomDemoServer() {
    }

    public static void main(String[] args) {
        TagPalette palette = TagPalette.of(Map.of(
                "accent", NamedTextColor.GOLD,
                "success", NamedTextColor.GREEN,
                "error", NamedTextColor.RED,
                "info", NamedTextColor.AQUA,
                "muted", NamedTextColor.GRAY));

        ClasspathLangSource coreSource = ClasspathLangSource.scanning("lang");
        Map<Locale, Map<String, String>> coreBundles = coreSource.load();
        LangFileConsistency.assertConsistent(coreBundles, Locale.US);
        List<String> availableIds = coreBundles.keySet().stream()
                .map(LocaleCodes::id)
                .sorted()
                .toList();

        ClasspathLangSource shopSource = ClasspathLangSource.scanning(
                MinestomDemoServer.class.getClassLoader(), "lang-shop", new JsonLangFileFormat());
        LangFileConsistency.assertConsistent(shopSource.load(), Locale.US);

        KeyedTranslator coreTranslator = KeyedTranslator.builder(Key.key("demo", "core"))
                .source(coreSource)
                .fallback(Locale.US)
                .miniMessage(palette.miniMessage())
                .prefix(PrefixPolicy.of("demo.prefix", "<prefix>"))
                .issues(new DemoTranslationIssues())
                .build();
        coreTranslator.register(Locale.US, Map.of("demo.runtime",
                "<prefix> <info>This key isn't in any lang file - it was added at runtime via "
                        + "KeyedTranslator#register(...).</info>"));

        KeyedTranslator shopTranslator = KeyedTranslator.builder(Key.key("demo", "shop"))
                .source(shopSource)
                .fallback(Locale.US)
                .miniMessage(palette.miniMessage())
                .issues(new DemoTranslationIssues())
                .build();

        Messages.install(coreTranslator);
        Messages.install(shopTranslator);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Messages.uninstall(coreTranslator);
            Messages.uninstall(shopTranslator);
        }));

        PlayerLocaleStore locales = new PlayerLocaleStore();

        MinecraftServer server = MinecraftServer.init();

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0, 42, 0));
        });
        events.addListener(PlayerSpawnEvent.class, event -> {
            if (!event.isFirstSpawn()) {
                return;
            }
            onFirstSpawn(event.getPlayer());
        });

        Command runtimeCommand = new Command("runtime");
        runtimeCommand.setDefaultExecutor((sender, context) -> {
            if (sender instanceof Player player) {
                player.sendMessage(Messages.render("demo.runtime"));
            }
        });

        MinecraftServer.getCommandManager().register(new LangCommand(locales, availableIds));
        MinecraftServer.getCommandManager().register(new ScoreCommand(locales));
        MinecraftServer.getCommandManager().register(new ShopCommand());
        MinecraftServer.getCommandManager().register(runtimeCommand);

        LOGGER.info("adventure-i18n Minestom demo starting on 0.0.0.0:25565 - lang bundles: {}", availableIds);
        server.start("0.0.0.0", 25565);
    }

    /**
     * Sends {@code demo.welcome} to the joining player, then broadcasts {@code demo.join} to
     * everyone currently online - both via the lazy client-locale path: {@code broadcast} is a
     * single unresolved {@link Component}, sent as-is to every recipient, and each one sees it
     * rendered in their own client-reported locale. There is no per-recipient branching here; that
     * is the whole point of {@link Messages#render(String, net.kyori.adventure.text.ComponentLike...)}.
     */
    private static void onFirstSpawn(Player player) {
        player.sendMessage(Messages.render("demo.welcome", Argument.string("player", player.getUsername())));

        Component broadcast = Messages.render("demo.join", Argument.string("player", player.getUsername()));
        for (Player online : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            online.sendMessage(broadcast);
        }
    }
}
