package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class CommandSpyPlugin implements Listener {
    private final Bot bot;

    public CommandSpyPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    @Override
    public boolean onSystemMessageReceived (final Component component, final String string, final String ansi) {
        final List<Component> children = component.children();

        if (
                !(component instanceof final TextComponent textComponent) ||
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

        bot.listener.dispatch(listener -> listener.onCommandSpyMessageReceived(sender, command));

        return true;
    }
}
