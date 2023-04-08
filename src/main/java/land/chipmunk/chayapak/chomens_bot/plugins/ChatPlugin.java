package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.chatParsers.ChomeNSCustomChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.KaboomChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.MinecraftChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.commandSpy.CommandSpyParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.ChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ChatPlugin extends SessionAdapter {
    private final Bot bot;

    private final List<ChatParser> chatParsers;

    private final CommandSpyParser commandSpyParser;

    private final List<ChatListener> listeners = new ArrayList<>();

    public ChatPlugin (Bot bot) {
        this.bot = bot;

        this.commandSpyParser = new CommandSpyParser(bot);

        bot.addListener(this);

        chatParsers = new ArrayList<>();
        chatParsers.add(new MinecraftChatParser(bot));
        chatParsers.add(new KaboomChatParser(bot));
        chatParsers.add(new ChomeNSCustomChatParser(bot));
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

        PlayerMessage commandSpyMessage;
        commandSpyMessage = commandSpyParser.parse(component);

        final String message = ComponentUtilities.stringify(component);
        for (ChatListener listener : listeners) {
            listener.systemMessageReceived(message, component);
            if (playerMessage != null) listener.playerMessageReceived(playerMessage);
            if (commandSpyMessage != null) listener.commandSpyMessageReceived(commandSpyMessage);
        }
    }

    public void send (String _message, String prefix) {
        final String message = prefix + _message;

        final String[] splitted = message.split("(?<=\\G.{100})|\\n");

        int i = 200;
        for (String splitMessage : splitted) {
            if (splitMessage.trim().equals("")) continue;

            bot.executor().schedule(() -> {
                if (splitMessage.startsWith("/")) {
                    bot.session().send(new ServerboundChatCommandPacket(
                            splitMessage.substring(1),
                            Instant.now().toEpochMilli(),
                            0L,
                            new ArrayList<>(),
                            false,
                            new ArrayList<>(),
                            null
                    ));
                } else {
                    bot.session().send(new ServerboundChatPacket(
                            splitMessage,
                            Instant.now().toEpochMilli(),
                            0L,
                            new byte[0],
                            false,
                            new ArrayList<>(),
                            null
                    ));
                }
            }, i, TimeUnit.MILLISECONDS);

            i = i + 200;
        }
    }

    public void send (String message) {
        send(message, "");
    }

    public void whisper (String targets, String message) {
        send(message, "/tell " + targets + " ");
    }

    public void tellraw (Component component, String targets) {
        if (bot.useChat()) {
            if (targets.equals("@a")) {
                final String stringified = ComponentUtilities.stringifyMotd(component).replace("§", "&");
                send(stringified);
            } else {
                final String stringified = ComponentUtilities.stringify(component);
                whisper(targets, stringified);
            }
        } else {
            bot.core().run("minecraft:tellraw " + targets + " " + GsonComponentSerializer.gson().serialize(component));
        }
    }

    public void tellraw (Component component, UUID uuid) { tellraw(component, UUIDUtilities.selector(uuid)); }

    public void tellraw (Component component) { tellraw(component, "@a"); }

    public void addListener (ChatListener listener) { listeners.add(listener); }

    public static class ChatListener {
        public void playerMessageReceived (PlayerMessage message) {}
        public void commandSpyMessageReceived (PlayerMessage message) {}
        public void systemMessageReceived (String message, Component component) {}
    }
}
