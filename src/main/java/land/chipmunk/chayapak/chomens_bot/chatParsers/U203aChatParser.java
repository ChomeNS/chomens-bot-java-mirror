package land.chipmunk.chayapak.chomens_bot.chatParsers;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.ChatParser;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;
import java.util.UUID;

// parses `[%s] %s › %s` translation or `%s %s › %s`
public class U203aChatParser implements ChatParser {
    private final Bot bot;

    public U203aChatParser(Bot bot) {
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

        final Component senderComponent = args.get(1);
        final Component contents = args.get(2);

        PlayerEntry sender;
        final HoverEvent<?> hoverEvent = senderComponent.hoverEvent();
        if (hoverEvent != null && hoverEvent.action().equals(HoverEvent.Action.SHOW_ENTITY)) {
            HoverEvent.ShowEntity entityInfo = (HoverEvent.ShowEntity) hoverEvent.value();
            final UUID senderUUID = entityInfo.id();
            sender = bot.players.getEntry(senderUUID);
        } else {
            final String stringUsername = ComponentUtilities.stringify(senderComponent);
            sender = bot.players.getEntry(stringUsername);
        }

        if (sender == null) return null;

        return new PlayerMessage(sender, senderComponent, contents);
    }
}
