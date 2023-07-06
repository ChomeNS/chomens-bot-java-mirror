package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
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
    public void systemMessageReceived(Component component) {
        TextComponent textComponent = null;

        try {
            textComponent = (TextComponent) component;
        } catch (ClassCastException e) {
            return;
        }

        final List<Component> children = textComponent.children();

        if (
                (
                        textComponent.color() != NamedTextColor.AQUA ||
                                textComponent.color() != NamedTextColor.YELLOW ||
                                textComponent.style().isEmpty()
                ) &&
                        children.size() < 2
        ) return;

        final String username = textComponent.content();
        final String command = ComponentUtilities.stringify(children.get(1));

        final MutablePlayerListEntry sender = bot.players.getEntry(username);

        if (sender == null) return;

        for (Listener listener : listeners) listener.commandReceived(sender, command);
    }

    public void addListener (Listener listener) { listeners.add(listener); }

    public static class Listener {
        public void commandReceived (MutablePlayerListEntry sender, String command) {}
    }
}
