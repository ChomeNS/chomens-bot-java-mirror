package land.chipmunk.chayapak.chomens_bot.chatParsers;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.ChatParser;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MinecraftChatParser implements ChatParser {
    private final Bot bot;

    // ? Is such a mapping necessary?
    private static final Map<String, String> typeMap = new HashMap<>();
    static {
        typeMap.put("chat.type.text", "minecraft:chat");
        typeMap.put("chat.type.announcement", "minecraft:say_command");
        typeMap.put("commands.message.display.incoming", "minecraft:msg_command");
        typeMap.put("chat.type.team.text", "minecraft:team_msg_command");
        typeMap.put("chat.type.emote", "minecraft:emote_command");
    }

    public MinecraftChatParser (Bot bot) {
        this.bot = bot;
    }

    @Override
    public PlayerMessage parse (Component message) {
        if (message instanceof TranslatableComponent) return parse((TranslatableComponent) message);
        return null;
    }

    public PlayerMessage parse (TranslatableComponent message) {
        final List<Component> args = message.args();
        final String key = message.key();
        if (args.size() < 2 || !typeMap.containsKey(key)) return null;

        final Component senderComponent = args.get(0);
        final Component contents = args.get(1);

        MutablePlayerListEntry sender;

        final HoverEvent<?> hoverEvent = senderComponent.hoverEvent();
        if (hoverEvent != null && hoverEvent.action().equals(HoverEvent.Action.SHOW_ENTITY)) {
            HoverEvent.ShowEntity entityInfo = (HoverEvent.ShowEntity) hoverEvent.value();
            final UUID senderUUID = entityInfo.id();
            sender = bot.players().getEntry(senderUUID);
        } else {
            final String stringUsername = ComponentUtilities.stringify(senderComponent);
            sender = bot.players().getEntry(stringUsername);
        }

        if (sender == null) return null;

        return new PlayerMessage(sender, senderComponent, contents);
    }
}
