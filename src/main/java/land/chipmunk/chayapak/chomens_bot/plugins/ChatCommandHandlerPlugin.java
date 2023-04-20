package land.chipmunk.chayapak.chomens_bot.plugins;

import lombok.Getter;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.command.PlayerCommandContext;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.List;

public class ChatCommandHandlerPlugin extends ChatPlugin.ChatListener {
    public final Bot bot;

    @Getter private final List<String> prefixes;
    @Getter private final List<String> commandSpyPrefixes;

    public ChatCommandHandlerPlugin(Bot bot) {
        this.bot = bot;

        this.prefixes = bot.config().prefixes();
        this.commandSpyPrefixes = bot.config().commandSpyPrefixes();

        bot.chat().addListener(this);
    }

    @Override
    public void playerMessageReceived (PlayerMessage message) { listener(message, false); }

    @Override
    public void commandSpyMessageReceived (PlayerMessage message) { listener(message, true); }

    private void listener (PlayerMessage message, boolean cspy) {
        final Component displayNameComponent = message.displayName();
        final Component messageComponent = message.contents();
        if (displayNameComponent == null || messageComponent == null) return;

        final String displayName = ComponentUtilities.stringify(displayNameComponent);
        final String contents = ComponentUtilities.stringify(messageComponent);

        String prefix = null;

        for (String eachPrefix : (cspy ? commandSpyPrefixes : prefixes)) {
            if (!contents.startsWith(eachPrefix)) continue;
            prefix = eachPrefix;
        }

        if (prefix == null) return;

        final String commandString = contents.substring(prefix.length());

        final String selector = cspy ? UUIDUtilities.selector(message.sender().profile().getId()) : "@a";

        final PlayerCommandContext context = new PlayerCommandContext(bot, displayName, prefix, selector, message.sender(), bot.hashing().hash(), bot.hashing().ownerHash());

        final Component output = bot.commandHandler().executeCommand(commandString, context, true, false, false, bot.hashing().hash(), bot.hashing().ownerHash(), null);
        final String textOutput = ((TextComponent) output).content();

        if (!textOutput.equals("success")) {
            context.sendOutput(output);
        }
    }
}
