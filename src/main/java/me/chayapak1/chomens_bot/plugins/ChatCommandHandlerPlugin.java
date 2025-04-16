package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.contexts.PlayerCommandContext;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;

import java.util.List;

public class ChatCommandHandlerPlugin implements Listener {
    public final Bot bot;

    public final List<String> prefixes;
    public final List<String> commandSpyPrefixes;

    public ChatCommandHandlerPlugin (final Bot bot) {
        this.bot = bot;

        this.prefixes = bot.config.prefixes;
        this.commandSpyPrefixes = bot.config.commandSpyPrefixes;

        bot.listener.addListener(this);
    }

    @Override
    public boolean onPlayerMessageReceived (final PlayerMessage message, final ChatPacketType packetType) {
        if (
                message.sender() != null &&
                        bot.profile != null &&
                        message.sender().profile.getId().equals(bot.profile.getId())
        ) return true;

        final Component displayNameComponent = message.displayName();
        final Component messageComponent = message.contents();
        if (displayNameComponent == null || messageComponent == null) return true;

        handle(displayNameComponent, messageComponent, message.sender(), "@a", prefixes, packetType);

        return true;
    }

    @Override
    public void onCommandSpyMessageReceived (final PlayerEntry sender, final String command) {
        if (
                sender.profile != null &&
                        bot.profile != null &&
                        sender.profile.getId().equals(bot.profile.getId())
        ) return;

        if (sender.profile == null) return;

        final Component displayNameComponent = Component.text(sender.profile.getName());
        final Component messageComponent = Component.text(command);

        handle(
                displayNameComponent,
                messageComponent,
                sender,
                UUIDUtilities.selector(sender.profile.getId()),
                commandSpyPrefixes,
                ChatPacketType.SYSTEM
        );
    }

    private void handle (
            final Component displayNameComponent,
            final Component messageComponent,
            final PlayerEntry sender,
            final String selector,
            final List<String> prefixes,
            final ChatPacketType packetType
    ) {
        final String displayName = ComponentUtilities.stringify(displayNameComponent);
        final String contents = ComponentUtilities.stringify(messageComponent);

        String prefix = null;

        for (final String eachPrefix : prefixes) {
            if (!contents.startsWith(eachPrefix)) continue;
            prefix = eachPrefix;
        }

        if (prefix == null) return;

        final String commandString = contents.substring(prefix.length());

        final PlayerCommandContext context = new PlayerCommandContext(
                bot,
                displayName,
                prefix,
                selector,
                sender,
                packetType
        );

        bot.commandHandler.executeCommand(commandString, context);
    }
}
