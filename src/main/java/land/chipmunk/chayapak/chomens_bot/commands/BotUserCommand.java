package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class BotUserCommand implements Command {
    public String name() { return "botuser"; }

    public String description() {
        return "Shows the bot's username and UUID";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        final String username = bot.username();
        final String uuid = bot.players().getBotEntry().profile().getIdAsString();

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
                        .color(NamedTextColor.GOLD),
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
                        .color(NamedTextColor.AQUA)
        );
    }
}
