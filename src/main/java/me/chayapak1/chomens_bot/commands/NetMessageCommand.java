package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
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
                new String[] { "<message>" },
                new String[] { "networkmessage", "irc" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;
        final List<Bot> bots = bot.bots;

        final String originServerName = bot.getServerString();
        final String originServerAddress = bot.getServerString(false);

        Component serverNameComponent = Component
                .text(originServerName)
                .color(NamedTextColor.GRAY);

        if (!bot.options.hidden) serverNameComponent = serverNameComponent
                .clickEvent(ClickEvent.copyToClipboard(originServerAddress))
                .hoverEvent(
                        HoverEvent.showText(
                                Component.empty()
                                        .append(Component.text(originServerAddress).color(NamedTextColor.GRAY))
                                        .append(Component.newline())
                                        .append(Component.text("Click here to copy the server host and port to your clipboard").color(NamedTextColor.GREEN))
                        )
                );

        final Component component = Component.translatable(
                "[%s]%s%s%s› %s",
                serverNameComponent,
                Component.space(),
                context.sender.displayName == null ?
                        Component.text(context.sender.profile.getName()).color(NamedTextColor.GRAY) :
                        context.sender.displayName.color(NamedTextColor.GRAY),
                Component.space(),
                Component.text(context.getString(true, true)).color(NamedTextColor.GRAY)
        ).color(NamedTextColor.DARK_GRAY);

        for (Bot eachBot : bots) {
            if (!eachBot.loggedIn) continue;
            eachBot.chat.tellraw(component);
        }

        return null;
    }
}
