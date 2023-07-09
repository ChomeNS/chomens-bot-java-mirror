package land.chipmunk.chayapak.chomens_bot.chatParsers;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.ChatParser;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.List;
import java.util.UUID;

public class KaboomChatParser implements ChatParser {
    private final Bot bot;

    public KaboomChatParser (Bot bot) {
        this.bot = bot;
    }

    @Override
    public PlayerMessage parse (Component message) {
        if (message instanceof TextComponent) return parse((TextComponent) message);
        return null;
    }

    public PlayerMessage parse (TextComponent message) {
        List<Component> children = message.children();

        if (!message.content().equals("") || !message.style().isEmpty() || children.size() < 3) return null;

        final Component prefix = children.get(0);
        Component displayName = Component.empty();
        Component contents = Component.empty();

        if (isSeperatorAt(children, 1)) { // Missing/blank display name
            if (children.size() > 3) contents = children.get(3);
        } else if (isSeperatorAt(children, 2)) {
            displayName = children.get(1);
            if (children.size() > 4) contents = children.get(4);
        } else {
            return null;
        }

        PlayerEntry sender = bot.players.getEntry(Component.empty().append(prefix).append(displayName));
        if (sender == null) sender = bot.players.getEntry(prefix.append(displayName)); // old
        if (sender == null) sender = new PlayerEntry(new GameProfile(new UUID(0L, 0L), null), GameMode.SURVIVAL, 0, displayName, 0L, null, new byte[0], true); // new and currently using

        return new PlayerMessage(sender, displayName, contents);
    }

    private boolean isSeperatorAt (List<Component> children, int start) {
        return (
                children.get(start).equals(Component.text(":")) ||
                        children.get(start).equals(Component.text("§f:"))
        ) && children.get(start + 1).equals(Component.space());
    }
}
