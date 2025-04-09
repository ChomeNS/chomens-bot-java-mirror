package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class CommandSuggestionPlugin implements ChatPlugin.Listener {
    public static final String ID = "chomens_bot_request_command_suggestion";

    private final Bot bot;

    public CommandSuggestionPlugin (final Bot bot) {
        this.bot = bot;

        bot.chat.addListener(this);
    }

    @Override
    public boolean systemMessageReceived (final Component component, final String string, final String ansi) {
        final List<Component> children = component.children();

        if (
                !(component instanceof final TextComponent idComponent) ||
                        !idComponent.content().equals(ID) ||
                        children.size() != 1 ||
                        !(children.getFirst() instanceof final TextComponent playerComponent)
        ) return true;

        final String player = playerComponent.content();

        final List<Component> output = new ArrayList<>();
        output.add(Component.text(ID));

        for (final Command command : CommandHandlerPlugin.COMMANDS) {
            if (command.consoleOnly) continue;

            final boolean hasAliases = command.aliases.length != 0;

            Component outputComponent = Component
                    .text(command.name)
                    .append(Component.text(command.trustLevel.name()))
                    .append(Component.text(hasAliases));

            if (hasAliases) {
                for (final String alias : command.aliases) outputComponent = outputComponent.append(Component.text(alias));
            }

            output.add(outputComponent);
        }

        bot.chat.tellraw(Component.join(JoinConfiguration.noSeparators(), output), player);

        return false;
    }
}
