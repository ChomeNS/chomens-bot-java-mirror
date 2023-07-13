package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandSuggestionPlugin extends ChatPlugin.Listener {
    private final Bot bot;

    public final String id = "chomens_bot_request_command_suggestion";

    public CommandSuggestionPlugin (Bot bot) {
        this.bot = bot;
        bot.chat.addListener(this);
    }

    @Override
    public void systemMessageReceived(Component component, boolean isCommandSuggestions) {
        if (!isCommandSuggestions) return;

        try {
            final List<Component> children = component.children();

            if (children.size() != 1) return;

            final String player = ((TextComponent) children.get(0)).content();

            final List<Component> output = new ArrayList<>();
            output.add(Component.text(id));

            for (Command command : bot.commandHandler.commands) {
                output.add(
                        Component
                                .text(command.name)
                                .append(
                                        Component.join(
                                                JoinConfiguration.noSeparators(),
                                                Arrays.stream(command.aliases)
                                                        .map(Component::text)
                                                        .toList()
                                        )
                                )
                                .append(Component.text(command.trustLevel.name()))
                );
            }

            bot.chat.tellraw(Component.join(JoinConfiguration.noSeparators(), output), player);
        } catch (Exception ignored) {}
    }
}
