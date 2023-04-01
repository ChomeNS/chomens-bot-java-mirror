package land.chipmunk.chayapak.chomens_bot.chatParsers;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.ChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MinecraftChatParser implements ChatParser {
    private final Bot bot;

    // ? Is such a mapping necessary?
    private static final Map<String, String> typeMap = new HashMap<>();
    static {
        typeMap.put("chat.type.text", "minecraft:chat");
        typeMap.put("chat.type.announcement", "minecraft:say_command");
        typeMap.put("chat.type.command", "minecraft:msg_command");
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

        final Map<String, Component> parameters = new HashMap<>();

        final Component senderComponent = args.get(0);
        final Component contents = args.get(1);

        final String stringUsername = ComponentUtilities.stringify(senderComponent);
        MutablePlayerListEntry sender = bot.players().getEntry(stringUsername);

        if (sender == null) return null;

        parameters.put("sender", senderComponent);
        parameters.put("contents", contents);

        return new PlayerMessage(parameters, sender);
    }
}
