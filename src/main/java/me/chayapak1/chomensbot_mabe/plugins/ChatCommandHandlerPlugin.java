package me.chayapak1.chomensbot_mabe.plugins;

import lombok.Getter;
import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.chatParsers.data.PlayerMessage;
import me.chayapak1.chomensbot_mabe.command.PlayerCommandContext;
import me.chayapak1.chomensbot_mabe.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class ChatCommandHandlerPlugin extends ChatPlugin.ChatListener {
    @Getter private final String prefix = "j*";

    public final Bot bot;

    public ChatCommandHandlerPlugin(Bot bot) {
        this.bot = bot;

        bot.chat().addListener(this);
    }

    @Override
    public void playerMessageReceived (PlayerMessage message) {
        final Component displayNameComponent = message.parameters().get("sender");
        final Component messageComponent = message.parameters().get("contents");
        if (displayNameComponent == null || messageComponent == null) return;

        final String displayName = ComponentUtilities.stringify(displayNameComponent);
        final String contents = ComponentUtilities.stringify(messageComponent);

        if (!contents.startsWith(prefix)) return;
        final String commandString = contents.substring(prefix.length());

        final PlayerCommandContext context = new PlayerCommandContext(bot, displayName);

        final Component output = CommandHandlerPlugin.executeCommand(commandString, context);
        final String textOutput = ((TextComponent) output).content();

        if (!textOutput.equals("success")) {
            context.sendOutput(output);
        }
    }
}
