package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class CommandSpyPlugin implements ChatPlugin.Listener {
    private final Bot bot;

    private final List<Listener> listeners = new ArrayList<>();

    public CommandSpyPlugin (Bot bot) {
        this.bot = bot;

        bot.chat.addListener(this);
    }

    @Override
    public boolean systemMessageReceived (Component component, String string, String ansi) {
        final List<Component> children = component.children();

        if (
                !(component instanceof TextComponent textComponent) ||
                        children.size() != 2 ||
                        textComponent.style().isEmpty() ||
                        (
                                textComponent.color() != NamedTextColor.AQUA &&
                                textComponent.color() != NamedTextColor.YELLOW
                        ) ||
                        !(children.getFirst() instanceof TextComponent) ||
                        !(children.getLast() instanceof TextComponent)
        ) return true;

        final String username = textComponent.content();
        final String command = ComponentUtilities.stringify(children.getLast());

        final PlayerEntry sender = bot.players.getEntry(username);

        if (sender == null) return true;

        for (Listener listener : listeners) listener.commandReceived(sender, command);

        return true;
    }

    public void addListener (Listener listener) { listeners.add(listener); }

    public interface Listener {
        default void commandReceived (PlayerEntry sender, String command) {}
    }
}
