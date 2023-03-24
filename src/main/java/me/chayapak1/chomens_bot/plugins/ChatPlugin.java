package me.chayapak1.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chatParsers.ChomeNSCustomChatParser;
import me.chayapak1.chomens_bot.chatParsers.KaboomChatParser;
import me.chayapak1.chomens_bot.chatParsers.MinecraftChatParser;
import me.chayapak1.chomens_bot.chatParsers.data.ChatParser;
import me.chayapak1.chomens_bot.chatParsers.data.PlayerMessage;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatPlugin extends SessionAdapter {
    private final Bot bot;

    private final List<ChatParser> chatParsers;

    private final List<ChatListener> listeners = new ArrayList<>();

    public ChatPlugin (Bot bot) {
        this.bot = bot;
        bot.addListener(this);

        chatParsers = new ArrayList<>();
        chatParsers.add(new MinecraftChatParser());
        chatParsers.add(new KaboomChatParser());
        chatParsers.add(new ChomeNSCustomChatParser());
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundSystemChatPacket) {
            packetReceived((ClientboundSystemChatPacket) packet);
        }
    }

    public void packetReceived (ClientboundSystemChatPacket packet) {
        final Component component = packet.getContent();

        try {
            final String key = ((TranslatableComponent) component).key();

            if (
                    key.equals("advMode.setCommand.success") ||
                    key.equals("advMode.notAllowed")
            ) return;
        } catch (ClassCastException ignored) {}

        PlayerMessage playerMessage = null;

        for (ChatParser parser : chatParsers) {
            playerMessage = parser.parse(component);
            if (playerMessage != null) break;
        }

        final String message = ComponentUtilities.stringify(component);
        for (ChatListener listener : listeners) {
            listener.systemMessageReceived(message, component);
            if (playerMessage != null) listener.playerMessageReceived(playerMessage);
        }
    }

    public void send (String message) {
        if (message.startsWith("/")) {
            bot.session().send(new ServerboundChatCommandPacket(
                    message.substring(1),
                    Instant.now().toEpochMilli(),
                    0L,
                    new ArrayList<>(),
                    false,
                    new ArrayList<>(),
                    null
            ));
        } else {
            bot.session().send(new ServerboundChatPacket(
                    message,
                    Instant.now().toEpochMilli(),
                    0L,
                    new byte[0],
                    false,
                    new ArrayList<>(),
                    null
            ));
        }
    }

    public void tellraw (Component component, String targets) {
        bot.core().run("minecraft:tellraw " + targets + " " + GsonComponentSerializer.gson().serialize(component));
    }

    public void tellraw (Component component, UUID uuid) { tellraw(component, UUIDUtilities.selector(uuid)); }

    public void tellraw (Component component) { tellraw(component, "@a"); }

    public void addListener (ChatListener listener) { listeners.add(listener); }

    public static class ChatListener {
        public void playerMessageReceived (PlayerMessage message) {}
        public void systemMessageReceived (String message, Component component) {}
    }
}
