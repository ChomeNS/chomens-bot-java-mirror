package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class UUIDCommand extends Command {
    public UUIDCommand () {
        super(
                "uuid",
                "Shows your UUID or other player's UUID",
                new String[] { "[username]" },
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String player = context.getString(true, false);

        if (!player.isEmpty()) {
            final PlayerEntry entry = bot.players.getEntry(player);

            final String name;
            final String uuid;

            if (entry == null) {
                name = player;
                uuid = UUIDUtilities.getOfflineUUID(player).toString();
            } else {
                name = entry.profile.getName();
                uuid = entry.profile.getIdAsString();
            }

            return Component.translatable(
                    "%s's UUID: %s",
                    Component.text(name),
                    Component
                            .text(uuid)
                            .hoverEvent(
                                    HoverEvent.showText(
                                            Component.text("Click here to copy the UUID to your clipboard").color(NamedTextColor.GREEN)
                                    )
                            )
                            .clickEvent(
                                    ClickEvent.copyToClipboard(uuid)
                            )
                            .color(bot.colorPalette.uuid)
            ).color(NamedTextColor.GREEN);
        } else {
            final PlayerEntry entry = context.sender;

            final String uuid = entry.profile.getIdAsString();

            return Component.translatable(
                    "Your UUID: %s",
                    Component
                            .text(uuid)
                            .hoverEvent(
                                    HoverEvent.showText(
                                            Component.text("Click here to copy the UUID to your clipboard").color(NamedTextColor.GREEN)
                                    )
                            )
                            .clickEvent(
                                    ClickEvent.copyToClipboard(uuid)
                            )
                            .color(bot.colorPalette.uuid)
            ).color(NamedTextColor.GREEN);
        }
    }
}
