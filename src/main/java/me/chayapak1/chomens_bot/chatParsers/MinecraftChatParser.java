package me.chayapak1.chomens_bot.chatParsers;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.chat.ChatParser;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MinecraftChatParser implements ChatParser {
    private final Bot bot;

    private static final List<String> keys = new ArrayList<>();

    static {
        keys.add("chat.type.text");
        keys.add("chat.type.announcement");
        keys.add("commands.message.display.incoming");
        keys.add("chat.type.team.text");
        keys.add("chat.type.emote");
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
        final List<TranslationArgument> args = message.arguments();
        final String key = message.key();
        if (args.size() < 2 || !keys.contains(key)) return null;

        final Component senderComponent = args.getFirst().asComponent();
        final Component contents = args.get(1).asComponent();

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
