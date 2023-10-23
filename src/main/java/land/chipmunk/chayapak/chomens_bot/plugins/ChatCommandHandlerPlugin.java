package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.command.PlayerCommandContext;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;

import java.util.List;

public class ChatCommandHandlerPlugin extends ChatPlugin.Listener {
    public final Bot bot;

    public final List<String> prefixes;
    public final List<String> commandSpyPrefixes;

    public ChatCommandHandlerPlugin(Bot bot) {
        this.bot = bot;

        this.prefixes = bot.config.prefixes;
        this.commandSpyPrefixes = bot.config.commandSpyPrefixes;

        bot.chat.addListener(this);

        bot.commandSpy.addListener(new CommandSpyPlugin.Listener() {
            @Override
            public void commandReceived(PlayerEntry sender, String command) {
                ChatCommandHandlerPlugin.this.commandSpyMessageReceived(sender, command);
            }
        });
    }

    @Override
    public boolean playerMessageReceived (PlayerMessage message) {
        try {
            if (message.sender.profile.getId().equals(bot.profile.getId())) return true;
        } catch (Exception ignored) {} // kinda sus ngl

        final Component displayNameComponent = message.displayName;
        final Component messageComponent = message.contents;
        if (displayNameComponent == null || messageComponent == null) return true;

        final String displayName = ComponentUtilities.stringify(displayNameComponent);
        final String contents = ComponentUtilities.stringify(messageComponent);

        String prefix = null;

        for (String eachPrefix : prefixes) {
            if (!contents.startsWith(eachPrefix)) continue;
            prefix = eachPrefix;
        }

        if (prefix == null) return true;

        final String commandString = contents.substring(prefix.length());

        final PlayerCommandContext context = new PlayerCommandContext(bot, displayName, prefix, "@a", message.sender);

        bot.executorService.submit(() -> {
            final Component output = bot.commandHandler.executeCommand(commandString, context, null);

            if (output != null) context.sendOutput(output);
        });

        return true;
    }

    public void commandSpyMessageReceived (PlayerEntry sender, String command) {
        try {
            if (sender.profile.getId().equals(bot.profile.getId())) return;
        } catch (Exception ignored) {
        } // kinda sus ngl

        final Component displayNameComponent = Component.text(sender.profile.getName());
        final Component messageComponent = Component.text(command);

        final String displayName = ComponentUtilities.stringify(displayNameComponent);
        final String contents = ComponentUtilities.stringify(messageComponent);

        String prefix = null;

        for (String eachPrefix : commandSpyPrefixes) {
            if (!contents.startsWith(eachPrefix)) continue;
            prefix = eachPrefix;
        }

        if (prefix == null) return;

        final String commandString = contents.substring(prefix.length());

        final String selector = UUIDUtilities.selector(sender.profile.getId());

        final PlayerCommandContext context = new PlayerCommandContext(bot, displayName, prefix, selector, sender);

        bot.executorService.submit(() -> {
            final Component output = bot.commandHandler.executeCommand(commandString, context, null);

            if (output != null) context.sendOutput(output);
        });
    }
}
