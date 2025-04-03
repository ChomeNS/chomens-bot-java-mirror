package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.contexts.PlayerCommandContext;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;

import java.util.List;

public class ChatCommandHandlerPlugin implements ChatPlugin.Listener, CommandSpyPlugin.Listener {
    public final Bot bot;

    public final List<String> prefixes;
    public final List<String> commandSpyPrefixes;

    public ChatCommandHandlerPlugin (Bot bot) {
        this.bot = bot;

        this.prefixes = bot.config.prefixes;
        this.commandSpyPrefixes = bot.config.commandSpyPrefixes;

        bot.chat.addListener(this);
        bot.commandSpy.addListener(this);
    }

    @Override
    public boolean playerMessageReceived (PlayerMessage message) {
        if (
                message.sender() != null &&
                        bot.profile != null &&
                        message.sender().profile.getId().equals(bot.profile.getId())
        ) return true;

        final Component displayNameComponent = message.displayName();
        final Component messageComponent = message.contents();
        if (displayNameComponent == null || messageComponent == null) return true;

        handle(displayNameComponent, messageComponent, message.sender(), "@a", prefixes);

        return true;
    }

    @Override
    public void commandReceived (PlayerEntry sender, String command) {
        if (
                sender.profile != null &&
                        bot.profile != null &&
                        sender.profile.getId().equals(bot.profile.getId())
        ) return;

        if (sender.profile == null) return;

        final Component displayNameComponent = Component.text(sender.profile.getName());
        final Component messageComponent = Component.text(command);

        handle(displayNameComponent, messageComponent, sender, UUIDUtilities.selector(sender.profile.getId()), commandSpyPrefixes);
    }

    private void handle (
            Component displayNameComponent,
            Component messageComponent,
            PlayerEntry sender,
            String selector,
            List<String> prefixes
    ) {
        final String displayName = ComponentUtilities.stringify(displayNameComponent);
        final String contents = ComponentUtilities.stringify(messageComponent);

        String prefix = null;

        for (String eachPrefix : prefixes) {
            if (!contents.startsWith(eachPrefix)) continue;
            prefix = eachPrefix;
        }

        if (prefix == null) return;

        final String commandString = contents.substring(prefix.length());

        final PlayerCommandContext context = new PlayerCommandContext(bot, displayName, prefix, selector, sender);

        bot.commandHandler.executeCommand(commandString, context, null);
    }
}
