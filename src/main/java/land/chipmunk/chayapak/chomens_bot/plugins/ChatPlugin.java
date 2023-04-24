package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.chatParsers.ChomeNSCustomChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.KaboomChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.MinecraftChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.commandSpy.CommandSpyParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.ChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ChatPlugin extends SessionAdapter {
    private final Bot bot;

    private final List<ChatParser> chatParsers;

    private final CommandSpyParser commandSpyParser;

    @Getter private final List<String> queue = new ArrayList<>();

    private final List<ChatListener> listeners = new ArrayList<>();

    public ChatPlugin (Bot bot) {
        this.bot = bot;

        this.commandSpyParser = new CommandSpyParser(bot);

        bot.addListener(this);

        chatParsers = new ArrayList<>();
        chatParsers.add(new MinecraftChatParser(bot));
        chatParsers.add(new KaboomChatParser(bot));
        chatParsers.add(new ChomeNSCustomChatParser(bot));

        bot.executor().scheduleAtFixedRate(this::sendChatTick, 0, 125, TimeUnit.MILLISECONDS);
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundSystemChatPacket) packetReceived((ClientboundSystemChatPacket) packet);
        else if (packet instanceof ClientboundPlayerChatPacket) packetReceived((ClientboundPlayerChatPacket) packet);
        else if (packet instanceof ClientboundDisguisedChatPacket) packetReceived((ClientboundDisguisedChatPacket) packet);
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

    public void packetReceived (ClientboundPlayerChatPacket packet) {
        final UUID senderUUID = packet.getSender();

        final MutablePlayerListEntry entry = bot.players().getEntry(senderUUID);

        if (entry == null) return;

        final PlayerMessage playerMessage = new PlayerMessage(
                entry,
                packet.getName(),
                Component.text(packet.getContent())
        );

        for (ChatListener listener : listeners) {
            listener.playerMessageReceived(playerMessage);

            if (packet.getChatType() == 4) { // type 4 is /say
                final Component component = Component.translatable(
                        "chat.type.announcement",
                        playerMessage.displayName(),
                        playerMessage.contents()
                );

                listener.systemMessageReceived(
                        ComponentUtilities.stringify(component),
                        component
                );
            } else {
                final Component unsignedContent = packet.getUnsignedContent();

                if (unsignedContent == null) return;

                listener.systemMessageReceived(
                        ComponentUtilities.stringify(unsignedContent),
                        unsignedContent
                );
            }
        }
    }

    public void packetReceived (ClientboundDisguisedChatPacket packet) {
        // totallynotskidded™ from chipmunkbot and modified a bit i guess
        // PlayerMessage parsedFromMessage = null;

        // i think im missing other types
        if (packet.getChatType() == 1) { // type 1 is /me
            final Component name = packet.getName();
            final Component content = packet.getMessage();

            for (ChatParser parser : chatParsers) {
                final Component component = Component.translatable(
                        "chat.type.emote",
                        name,
                        content
                );

                final PlayerMessage parsed = parser.parse(component);

                if (parsed == null) continue;

                final PlayerMessage playerMessage = new PlayerMessage(parsed.sender(), packet.getName(), parsed.contents());

                for (ChatListener listener : listeners) {
                    listener.playerMessageReceived(playerMessage);
                    listener.systemMessageReceived(
                            ComponentUtilities.stringify(component),
                            component
                    );
                }
            }
        }
    }

    private void sendChatTick () {
        try {
            if (queue.size() == 0) return;

            final String message = queue.get(0);

            final String[] splitted = message.split("(?<=\\G.{255})|\\n");

            for (String splitMessage : splitted) {
                if (splitMessage.trim().equals("")) continue;

                if (splitMessage.startsWith("/")) {
                    bot.session().send(new ServerboundChatCommandPacket(
                            splitMessage.substring(1),
                            Instant.now().toEpochMilli(),
                            0L,
                            Collections.emptyList(),
                            0,
                            new BitSet()
                    ));
                } else {
                    // Temporary fix for the bot getting kicked instead of chatting
                    bot.core().run("essentials:sudo " + bot.username() + " c:" + splitMessage);
//                    bot.session().send(new ServerboundChatPacket(
//                            splitMessage,
//                            Instant.now().toEpochMilli(),
//                            0L,
//                            new byte[0],
//                            0,
//                            new BitSet()
//                    ));
                }
            }

            queue.remove(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send (String message) {
        queue.add(message);
    }

    public void tellraw (Component component, String targets) {
        if (bot.options().useChat()) {
            if (!targets.equals("@a")) return; // worst fix of all time!1!

            final String stringified = ComponentUtilities.stringifyMotd(component).replace("§", "&");
            send(stringified);
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
