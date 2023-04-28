package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

public class CommandSuggestionPlugin extends ChatPlugin.ChatListener {
    private final Bot bot;

    @Getter @Setter private String id = "chomens_bot_command_suggestion";

    public CommandSuggestionPlugin (Bot bot) {
        this.bot = bot;
        bot.chat().addListener(this);
    }

    @Override
    public void systemMessageReceived(Component component) {
        try {
            final String tag = ((TextComponent) component).content();

            if (!tag.equals(id)) return;

            final List<Component> children = component.children();

            if (children.size() < 2) return;

            final int transactionId = Integer.parseInt(((TextComponent) children.get(0)).content());
            final String player = ((TextComponent) children.get(1)).content();

            Component inputComponent;
            try {
                inputComponent = children.get(2);
            } catch (IndexOutOfBoundsException e) {
                inputComponent = Component.text("");
            }
            final String input = ((TextComponent) inputComponent).content();

            System.out.println("input is \"" + input + "\"");

            final List<Component> output = new ArrayList<>();
            output.add(Component.text(id));
            output.add(Component.text(transactionId));

            for (Command command : bot.commandHandler().commands()) {
                if (!command.name().startsWith(input)) continue;

                output.add(Component.text(command.name()));
            }

            bot.chat().tellraw(Component.join(JoinConfiguration.noSeparators(), output), player);
        } catch (Exception ignored) {}
    }
}
