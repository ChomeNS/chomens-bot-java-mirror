package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class CommandSuggestionPlugin implements ChatPlugin.Listener {
    private final Bot bot;

    public final String id = "chomens_bot_request_command_suggestion";

    public CommandSuggestionPlugin (Bot bot) {
        this.bot = bot;
        bot.chat.addListener(this);
    }

    @Override
    public boolean systemMessageReceived(Component component, String string, String ansi) {
        if (!(component instanceof TextComponent idComponent)) return true;

        if (!idComponent.content().equals(id)) return true;

        try {
            final List<Component> children = component.children();

            if (children.size() != 1) return true;

            final String player = ((TextComponent) children.getFirst()).content();

            final List<Component> output = new ArrayList<>();
            output.add(Component.text(id));

            for (Command command : CommandHandlerPlugin.commands) {
                if (command.consoleOnly) continue;

                final boolean hasAliases = command.aliases.length != 0;

                Component outputComponent = Component
                        .text(command.name)
                        .append(Component.text(command.trustLevel.name()))
                        .append(Component.text(hasAliases));

                if (hasAliases) {
                    for (String alias : command.aliases) outputComponent = outputComponent.append(Component.text(alias));
                }

                output.add(outputComponent);
            }

            bot.chat.tellraw(Component.join(JoinConfiguration.noSeparators(), output), player);

            return false;
        } catch (Exception ignored) {}

        return true;
    }
}
