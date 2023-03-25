package me.chayapak1.chomens_bot.chatParsers;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chatParsers.data.ChatParser;
import me.chayapak1.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import me.chayapak1.chomens_bot.chatParsers.data.PlayerMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.Style;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KaboomChatParser implements ChatParser {
    private final Bot bot;

    public KaboomChatParser (Bot bot) {
        this.bot = bot;
    }

    private static final Style empty = Style.empty();
    private static final Component SEPERATOR_COLON = Component.text(":");
    private static final Component SEPERATOR_SPACE = Component.space();

    @Override
    public PlayerMessage parse (Component message) {
        if (message instanceof TextComponent) return parse((TextComponent) message);
        if (message instanceof TranslatableComponent) return parse((TranslatableComponent) message);
        return null;
    }

    public PlayerMessage parse (TranslatableComponent message) {
        if (message.key().equals("%s")) {
            message.args();
            if (message.args().size() == 1 && message.style().equals(empty)) return parse(message.args().get(0));
        }
        return null;
    }

    public PlayerMessage parse (TextComponent message) {
        List<Component> children = message.children();

        if (!message.content().equals("") || !message.style().equals(empty) || children.size() < 3) return null;

        final Map<String, Component> parameters = new HashMap<>();

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

        MutablePlayerListEntry sender = bot.players().getEntry(Component.empty().append(prefix).append(displayName));
        if (sender == null) sender = bot.players().getEntry(prefix.append(displayName)); // old
        if (sender == null) sender = new MutablePlayerListEntry(new GameProfile(new UUID(0L, 0L), null), GameMode.SURVIVAL, 0, displayName, 0L, null, new byte[0]); // new and currently using

        parameters.put("sender", displayName);
        parameters.put("prefix", prefix);
        parameters.put("contents", contents);

        return new PlayerMessage(parameters, sender);
    }

    private boolean isSeperatorAt (List<Component> children, int start) {
        return children.get(start).equals(SEPERATOR_COLON) && children.get(start + 1).equals(SEPERATOR_SPACE);
    }
}
