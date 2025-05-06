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
                    "commands.uuid.other",
                    NamedTextColor.GREEN,
                    Component.text(name),
                    Component
                            .text(uuid, bot.colorPalette.uuid)
                            .hoverEvent(
                                    HoverEvent.showText(
                                            Component.translatable(
                                                    "commands.generic.click_to_copy_uuid",
                                                    NamedTextColor.GREEN
                                            )
                                    )
                            )
                            .clickEvent(
                                    ClickEvent.copyToClipboard(uuid)
                            )
            );
        } else {
            final PlayerEntry entry = context.sender;

            final String uuid = entry.profile.getIdAsString();

            return Component.translatable(
                    "commands.uuid.self",
                    NamedTextColor.GREEN,
                    Component
                            .text(uuid, bot.colorPalette.uuid)
                            .hoverEvent(
                                    HoverEvent.showText(
                                            Component.translatable(
                                                    "commands.generic.click_to_copy_uuid",
                                                    NamedTextColor.GREEN
                                            )
                                    )
                            )
                            .clickEvent(
                                    ClickEvent.copyToClipboard(uuid)
                            )
            );
        }
    }
}
