package gg.cubix.adventurei18n.minestomdemo;

import gg.cubix.adventurei18n.Messages;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code /shop [item]} - backed by the {@code demo.shop} {@link gg.cubix.adventurei18n.KeyedTranslator},
 * which loads its bundles via {@code adventure-i18n-json}'s {@code JsonLangFileFormat} instead of
 * the default {@code PropertiesLangFileFormat} used by {@link MinestomDemoServer}'s {@code
 * demo.core} translator. Both are installed on the same {@link net.kyori.adventure.translation.GlobalTranslator}
 * at once, proving the lang file format is a per-translator choice, not a project-wide one.
 *
 * <p>Every response here goes through {@link Messages#render(String, net.kyori.adventure.text.ComponentLike...)}
 * - the client-locale automatism - rather than {@link PlayerLocaleStore}, unlike {@link LangCommand}
 * and {@link ScoreCommand}. Both paths render the exact same kind of message; which one applies is
 * a per-call-site choice, not a per-translator one.
 */
public final class ShopCommand extends Command {

    private static final Map<String, Integer> CATALOG = new LinkedHashMap<>();

    static {
        CATALOG.put("sword", 25);
        CATALOG.put("shield", 15);
        CATALOG.put("apple", 3);
    }

    public ShopCommand() {
        super("shop");

        Map<UUID, Integer> balances = new ConcurrentHashMap<>();
        String catalog = String.join(", ", CATALOG.keySet());
        var itemArg = ArgumentType.Word("item");

        setDefaultExecutor((sender, context) -> {
            if (sender instanceof Player player) {
                player.sendMessage(Messages.render("shop.title", Argument.string("catalog", catalog)));
            } else {
                sender.sendMessage("/shop must be run by a player.");
            }
        });

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("/shop must be run by a player.");
                return;
            }

            String item = context.get(itemArg);
            Integer price = CATALOG.get(item);
            if (price == null) {
                player.sendMessage(Messages.render("shop.unknown.item",
                        Argument.string("item", item),
                        Argument.string("catalog", catalog)));
                return;
            }

            int balance = balances.computeIfAbsent(player.getUuid(), id -> 100);
            if (balance < price) {
                player.sendMessage(Messages.render("shop.buy.error",
                        Argument.string("item", item),
                        Argument.numeric("price", price),
                        Argument.numeric("balance", balance)));
                return;
            }

            balances.put(player.getUuid(), balance - price);
            player.sendMessage(Messages.render("shop.buy.success",
                    Argument.string("item", item),
                    Argument.numeric("price", price),
                    Argument.numeric("balance", balance - price)));
        }, itemArg);
    }
}
