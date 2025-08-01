package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.util.ChatMessageUtilities;
import me.chayapak1.chomens_bot.util.I18nUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class NetMessageCommand extends Command {
    public NetMessageCommand () {
        super(
                "netmsg",
                new String[] { "<message>" },
                new String[] { "networkmessage", "irc" },
                TrustLevel.PUBLIC,
                false,
                new ChatPacketType[] { ChatPacketType.SYSTEM, ChatPacketType.DISGUISED }
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;
        final List<Bot> bots = bot.bots;

        final String originServerName = bot.getServerString();
        final String originServerAddress = bot.getServerString(false);

        final TextComponent.Builder serverNameComponent = Component.text()
                .content(originServerName)
                .color(NamedTextColor.GRAY);

        if (!bot.options.hidden) serverNameComponent
                .clickEvent(ClickEvent.copyToClipboard(originServerAddress))
                .hoverEvent(
                        HoverEvent.showText(
                                Component.empty()
                                        .append(Component.text(originServerAddress, NamedTextColor.GRAY))
                                        .append(Component.newline())
                                        .append(Component.translatable("commands.netmsg.hover.copy_server_to_clipboard", NamedTextColor.GREEN))
                        )
                );

        final String rawMessage = context.getString(true, true);

        final Component stylizedMessage = ChatMessageUtilities.applyChatMessageStyling(rawMessage);

        final Component component = Component.translatable(
                "[%s]%s%s%s› %s", NamedTextColor.DARK_GRAY,
                serverNameComponent,
                Component.space(),
                context.sender.displayName == null ?
                        Component.text(context.sender.profile.getName(), NamedTextColor.GRAY) :
                        context.sender.displayName.colorIfAbsent(NamedTextColor.GRAY),
                Component.space(),
                Component.empty()
                        .append(stylizedMessage)
                        .color(NamedTextColor.GRAY)
                        .clickEvent(ClickEvent.copyToClipboard(rawMessage))
                        .hoverEvent(
                                HoverEvent.showText(
                                        Component.translatable(
                                                "commands.generic.click_to_copy_message",
                                                NamedTextColor.GREEN
                                        )
                                )
                        )
        );

        for (final Bot eachBot : bots) eachBot.chat.tellraw(I18nUtilities.render(component));

        return null;
    }
}
