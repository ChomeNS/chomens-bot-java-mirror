package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class DiscordCommand extends Command {
    public DiscordCommand () {
        super(
                "discord",
                "Shows the Discord invite",
                new String[] {},
                new String[] {},
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) {
        final Bot bot = context.bot;

        final String link = bot.config.discord.inviteLink;
        return Component.empty()
                .append(Component.text("The Discord invite is ").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)))
                .append(
                        Component
                                .text(link)
                                .clickEvent(ClickEvent.openUrl(link))
                                .color(NamedTextColor.BLUE)
                );
    }
}
