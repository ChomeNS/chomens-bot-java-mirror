package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class NetMessageCommand extends Command {
    public NetMessageCommand () {
        super(
                "netmsg",
                "Broadcasts a message to all of the servers that the bot is connected",
                new String[] { "<{message}>" },
                new String[] { "networkmessage", "irc" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;
        final List<Bot> bots = bot.bots;

        final String hostAndPort = bot.host + ":" + bot.port;

        final Component component = Component.translatable(
                "[%s]%s%s%s› %s",
                Component
                        .text(hostAndPort)
                        .color(NamedTextColor.GRAY)
                        .clickEvent(ClickEvent.copyToClipboard(hostAndPort))
                        .hoverEvent(
                                HoverEvent.showText(
                                        Component.empty()
                                                .append(Component.text(hostAndPort).color(NamedTextColor.GRAY))
                                                .append(Component.newline())
                                                .append(Component.text("Click here to copy the server host and port to your clipboard").color(NamedTextColor.GREEN))
                                )
                        ),
                Component.text(" "),
                context.sender.displayName == null ? Component.text(context.sender.profile.getName()) : context.sender.displayName.color(NamedTextColor.GRAY),
                Component.text(" "),
                Component.text(String.join(" ", args)).color(NamedTextColor.GRAY)
        ).color(NamedTextColor.DARK_GRAY);

        for (Bot eachBot : bots) {
            if (!eachBot.loggedIn) continue;
            eachBot.chat.tellraw(component);
        }

        return null;
    }
}
