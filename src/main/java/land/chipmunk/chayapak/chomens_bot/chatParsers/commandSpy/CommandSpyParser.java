package land.chipmunk.chayapak.chomens_bot.chatParsers.commandSpy;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.ChatParser;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class CommandSpyParser implements ChatParser {
    private final Bot bot;

    public CommandSpyParser (Bot bot) {
        this.bot = bot;
    }

    @Override
    public PlayerMessage parse (Component message) {
        if (message instanceof TextComponent) return parse((TextComponent) message);
        return null;
    }

    public PlayerMessage parse (TextComponent message) {
        final List<Component> children = message.children();

        if (
                (
                        message.color() != NamedTextColor.AQUA ||
                        message.color() != NamedTextColor.YELLOW
                ) &&
                children.size() < 2
        ) return null;

        final Component username = Component.text(message.content());
        final Component command = children.get(1);

        final String stringUsername = ComponentUtilities.stringify(username);
        MutablePlayerListEntry sender = bot.players().getEntry(stringUsername);

        if (sender == null) return null;

        return new PlayerMessage(sender, username, command);
    }
}
