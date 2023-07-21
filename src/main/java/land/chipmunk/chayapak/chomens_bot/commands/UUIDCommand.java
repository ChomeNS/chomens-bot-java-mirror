package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class UUIDCommand extends Command {
    public UUIDCommand () {
        super(
                "uuid",
                "Shows your UUID or other player's UUID",
                new String[] { "[{username}]" },
                new String[] {},
                TrustLevel.PUBLIC,
false
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        if (args.length > 0) {
            final PlayerEntry entry = bot.players.getEntry(String.join(" ", args));

            if (entry == null) return Component.text("Invalid player name").color(NamedTextColor.RED);

            final String name = entry.profile.getName();
            final String uuid = entry.profile.getIdAsString();

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
                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.uuid))
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
                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.uuid))
            ).color(NamedTextColor.GREEN);
        }
    }
}
