package gg.cubix.adventurei18n.minestomdemo;

import gg.cubix.adventurei18n.KeyedTranslator;
import gg.cubix.adventurei18n.LocaleCodes;
import gg.cubix.adventurei18n.Messages;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * {@code /lang [reset|<code>]} - the demo's showcase for {@link gg.cubix.adventurei18n.LocaleSource}:
 * every response here is rendered via {@link Messages#render(gg.cubix.adventurei18n.LocaleSource,
 * java.util.UUID, String, net.kyori.adventure.text.ComponentLike...)}, i.e. against the
 * server-managed override this command itself maintains in {@link PlayerLocaleStore} - never
 * against the client's own reported locale. Compare with {@link MinestomDemoServer}'s join
 * broadcast, which deliberately uses the other path.
 */
public final class LangCommand extends Command {

    public LangCommand(PlayerLocaleStore locales, List<String> availableIds) {
        super("lang");

        String available = String.join(", ", availableIds);
        String example = availableIds.isEmpty() ? LocaleCodes.id(Locale.US) : availableIds.get(0);

        var codeArg = ArgumentType.Word("code");

        setDefaultExecutor((sender, context) -> asPlayer(sender, player ->
                player.sendMessage(Messages.render(locales, player.getUuid(), "demo.lang.usage",
                        Argument.string("example", example),
                        Argument.string("available", available)))));

        addSyntax((sender, context) -> asPlayer(sender, player -> {
            String input = context.get(codeArg);

            if ("reset".equalsIgnoreCase(input)) {
                locales.clear(player.getUuid());
                player.sendMessage(Messages.render(locales, player.getUuid(), "demo.lang.reset",
                        Argument.string("code", LocaleCodes.id(KeyedTranslator.systemFallback()))));
                return;
            }

            Locale parsed = LocaleCodes.parse(input);
            if (parsed == null || !availableIds.contains(LocaleCodes.id(parsed))) {
                player.sendMessage(Messages.render(locales, player.getUuid(), "demo.lang.unknown",
                        Argument.string("input", input),
                        Argument.string("available", available)));
                return;
            }

            locales.set(player.getUuid(), parsed);
            player.sendMessage(Messages.render(locales, player.getUuid(), "demo.lang.set",
                    Argument.string("code", LocaleCodes.id(parsed))));
        }), codeArg);
    }

    private static void asPlayer(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (sender instanceof Player player) {
            action.accept(player);
        } else {
            sender.sendMessage("/lang must be run by a player.");
        }
    }
}
