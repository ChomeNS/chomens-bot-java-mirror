package land.chipmunk.chayapak.chomens_bot.chatParsers;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.ChatParser;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

import java.util.List;

// Might be a confusing name, but I mean the [Chat] chayapak custom chat thing or any other
// custom chat that uses the `[%s] %s › %s` translation or %s %s › %s
public class ChomeNSCustomChatParser implements ChatParser {
    private final Bot bot;

    public ChomeNSCustomChatParser (Bot bot) {
        this.bot = bot;
    }

    @Override
    public PlayerMessage parse (Component message) {
        if (message instanceof TranslatableComponent) return parse((TranslatableComponent) message);
        return null;
    }

    // very similar to MinecraftChatParser
    public PlayerMessage parse (TranslatableComponent message) {
        final List<Component> args = message.args();
        if (args.size() < 3 || (!message.key().equals("[%s] %s › %s") && !message.key().equals("%s %s › %s"))) return null;

        final Component username = args.get(1);
        final Component contents = args.get(2);

        final String stringUsername = ComponentUtilities.stringify(username);
        MutablePlayerListEntry sender = bot.players().getEntry(stringUsername);

        if (sender == null) return null;

        return new PlayerMessage(sender, username, contents);
    }
}
