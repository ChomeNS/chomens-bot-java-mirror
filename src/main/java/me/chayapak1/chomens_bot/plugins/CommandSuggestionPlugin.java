package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.listener.Listener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class CommandSuggestionPlugin implements Listener {
    private final Bot bot;

    private final String id;

    public CommandSuggestionPlugin (final Bot bot) {
        this.bot = bot;
        this.id = bot.config.namespace + "_request_command_suggestion"; // chomens_bot_request_command_suggestion

        bot.listener.addListener(this);
    }

    @Override
    public boolean onSystemMessageReceived (
            final Component component,
            final ChatPacketType packetType,
            final String string,
            final String ansi
    ) {
        final List<Component> children = component.children();

        if (
                packetType != ChatPacketType.SYSTEM
                        || !(component instanceof final TextComponent idComponent)
                        || !idComponent.content().equals(id)
                        || children.size() != 1
                        || !(children.getFirst() instanceof final TextComponent playerComponent)
        ) return true;

        final String player = playerComponent.content();

        final List<Component> output = new ArrayList<>();
        output.add(Component.text(id));

        for (final Command command : CommandHandlerPlugin.COMMANDS) {
            if (command.consoleOnly) continue;

            final boolean hasAliases = command.aliases.length != 0;

            final TextComponent.Builder outputComponent = Component
                    .text()
                    .content(command.name)
                    .append(Component.text(command.trustLevel.name()))
                    .append(Component.text(hasAliases));

            if (hasAliases) {
                for (final String alias : command.aliases) outputComponent.append(Component.text(alias));
            }

            output.add(outputComponent.build());
        }

        bot.chat.tellraw(Component.join(JoinConfiguration.noSeparators(), output), player);

        return false;
    }
}
