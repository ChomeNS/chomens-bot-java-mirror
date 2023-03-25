package me.chayapak1.chomens_bot.chatParsers;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chatParsers.data.ChatParser;
import me.chayapak1.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import me.chayapak1.chomens_bot.chatParsers.data.PlayerMessage;
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

        // try to find the sender then make it a player list entry
        final HoverEvent<?> hoverEvent = senderComponent.hoverEvent();
        if (hoverEvent == null || !hoverEvent.action().equals(HoverEvent.Action.SHOW_ENTITY)) return null;
        HoverEvent.ShowEntity entityInfo = (HoverEvent.ShowEntity) hoverEvent.value();
        final UUID senderUUID = entityInfo.id();

        MutablePlayerListEntry sender = bot.players().getEntry(senderUUID);
        if (sender == null) sender = new MutablePlayerListEntry(new GameProfile(senderUUID, null), GameMode.SURVIVAL, 0, entityInfo.name(), 0L, null, new byte[0]);

        parameters.put("sender", senderComponent);
        parameters.put("contents", contents);

        return new PlayerMessage(parameters, sender);
    }
}
