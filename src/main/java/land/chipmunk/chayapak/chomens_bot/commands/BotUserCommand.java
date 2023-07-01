package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class BotUserCommand extends Command {
    public BotUserCommand () {
        super(
                "botuser",
                "Shows the bot's username and UUID",
                new String[] {},
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        final String username = bot.username();
        final String uuid = bot.profile().getIdAsString();

        return Component.translatable(
                "The bot's username is: %s and the UUID is: %s",
                Component
                        .text(username)
                        .hoverEvent(
                                HoverEvent.showText(
                                        Component
                                                .text("Click here to copy the username to your clipboard")
                                                .color(NamedTextColor.GREEN)
                                )
                        )
                        .clickEvent(
                                ClickEvent.copyToClipboard(username)
                        )
                        .color(ColorUtilities.getColorByString(bot.config().colorPalette().username())),
                Component
                        .text(uuid)
                        .hoverEvent(
                                HoverEvent.showText(
                                        Component
                                                .text("Click here to copy the UUID to your clipboard")
                                                .color(NamedTextColor.GREEN)
                                )
                        )
                        .clickEvent(
                                ClickEvent.copyToClipboard(uuid)
                        )
                        .color(ColorUtilities.getColorByString(bot.config().colorPalette().uuid()))
        ).color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()));
    }
}
