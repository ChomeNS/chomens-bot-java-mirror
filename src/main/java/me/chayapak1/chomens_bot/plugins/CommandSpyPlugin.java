package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class CommandSpyPlugin extends ChatPlugin.Listener {
    private final Bot bot;

    private final List<Listener> listeners = new ArrayList<>();

    public CommandSpyPlugin (Bot bot) {
        this.bot = bot;

        bot.chat.addListener(this);
    }

    @Override
    public boolean systemMessageReceived(Component component, String string, String ansi) {
        TextComponent textComponent;

        try {
            textComponent = (TextComponent) component;

            final List<Component> children = textComponent.children();

            if (
                    (
                            textComponent.color() != NamedTextColor.AQUA ||
                                    textComponent.color() != NamedTextColor.YELLOW ||
                                    textComponent.style().isEmpty()
                    ) &&
                            children.size() < 2
            ) return true;

            if (!((TextComponent) children.get(0)).content().equals(": ")) return true;

            final String username = textComponent.content();
            final String command = ComponentUtilities.stringify(children.get(1));

            final PlayerEntry sender = bot.players.getEntry(username);

            if (sender == null) return true;

            for (Listener listener : listeners) listener.commandReceived(sender, command);
        } catch (ClassCastException ignored) {}

        return true;
    }

    public void addListener (Listener listener) { listeners.add(listener); }

    public static class Listener {
        public void commandReceived (PlayerEntry sender, String command) {}
    }
}
