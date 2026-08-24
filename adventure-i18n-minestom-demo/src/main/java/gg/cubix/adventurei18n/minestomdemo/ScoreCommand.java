package gg.cubix.adventurei18n.minestomdemo;

import gg.cubix.adventurei18n.Messages;
import net.kyori.adventure.text.minimessage.translation.Argument;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code /score} - a second {@link gg.cubix.adventurei18n.LocaleSource} showcase, next to
 * {@link LangCommand}: it renders eagerly for the invoking player via the same
 * {@link PlayerLocaleStore}, and uses {@link Argument#numeric} rather than
 * {@link Argument#string}, so the same lang key ({@code demo.score}) shows up correctly formatted
 * in every bundled locale.
 */
public final class ScoreCommand extends Command {

    public ScoreCommand(PlayerLocaleStore locales) {
        super("score");

        Map<UUID, AtomicInteger> scores = new ConcurrentHashMap<>();

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("/score must be run by a player.");
                return;
            }

            int points = scores.computeIfAbsent(player.getUuid(), id -> new AtomicInteger()).incrementAndGet();
            player.sendMessage(Messages.render(locales, player.getUuid(), "demo.score",
                    Argument.numeric("points", points)));
        });
    }
}
