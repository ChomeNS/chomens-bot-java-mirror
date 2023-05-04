package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.packetlib.Session;
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

public class ChatPlugin extends Bot.Listener {
    private final Bot bot;

    private final List<ChatParser> chatParsers;

    private final CommandSpyParser commandSpyParser;

    @Getter private final List<String> queue = new ArrayList<>();
    private final List<String> _queue = new ArrayList<>();

    private final List<Listener> listeners = new ArrayList<>();

    public ChatPlugin (Bot bot) {
        this.bot = bot;

        this.commandSpyParser = new CommandSpyParser(bot);

        bot.addListener(this);

        chatParsers = new ArrayList<>();
        chatParsers.add(new MinecraftChatParser(bot));
        chatParsers.add(new KaboomChatParser(bot));
        chatParsers.add(new ChomeNSCustomChatParser(bot));

        bot.executor().scheduleAtFixedRate(this::_sendChatTick, 0, 1, TimeUnit.MILLISECONDS);
        bot.executor().scheduleAtFixedRate(this::sendChatTick, 0, bot.config().chatQueueDelay(), TimeUnit.MILLISECONDS);
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

        for (Listener listener : listeners) {
            listener.systemMessageReceived(component);
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

        for (Listener listener : listeners) {
            listener.playerMessageReceived(playerMessage);

            if (packet.getChatType() == 4) { // type 4 is /say
                final Component component = Component.translatable(
                        "chat.type.announcement",
                        playerMessage.displayName(),
                        playerMessage.contents()
                );

                listener.systemMessageReceived(component);
            } else {
                final Component unsignedContent = packet.getUnsignedContent();

                if (unsignedContent == null) return;

                listener.systemMessageReceived(unsignedContent);
            }
        }
    }

    public void packetReceived (ClientboundDisguisedChatPacket packet) {
        // totallynotskidded™ from chipmunkbot and modified i guess

        final int type = packet.getChatType();

        // i think im missing other types
        if (type == 1 || type == 4 || type == 2) { // type 1 is /me, type 4 is /say, type 2 is /msg /tell or whatever thing
            final Component name = packet.getName();
            final Component content = packet.getMessage();

            for (ChatParser parser : chatParsers) {
                String translate;

                switch (type) {
                    case 1 -> translate = "chat.type.emote";
                    case 4 -> translate = "chat.type.announcement";
                    case 2 -> translate = "commands.message.display.incoming";
                    default -> {
                        continue;
                    }
                }

                final Component component = Component.translatable(
                        translate,
                        name,
                        content
                );

                final PlayerMessage parsed = parser.parse(component);

                if (parsed == null) continue;

                final PlayerMessage playerMessage = new PlayerMessage(parsed.sender(), packet.getName(), parsed.contents());

                for (Listener listener : listeners) {
                    listener.playerMessageReceived(playerMessage);
                    listener.systemMessageReceived(component);
                }
            }
        } else {
            final Component component = packet.getMessage();

            PlayerMessage parsedFromMessage = null;

            for (ChatParser parser : chatParsers) {
                parsedFromMessage = parser.parse(component);
                if (parsedFromMessage != null) break;
            }

            if (parsedFromMessage == null) return;

            final PlayerMessage playerMessage = new PlayerMessage(parsedFromMessage.sender(), packet.getName(), parsedFromMessage.contents());

            for (Listener listener : listeners) {
                listener.playerMessageReceived(playerMessage);
                listener.systemMessageReceived(component);
            }
        }
    }

    // TODO: Probably improve this code
    private void _sendChatTick () {
        try {
            if (queue.size() == 0) return;

            final String message = queue.get(0);

            final String[] splitted = message.split("(?<=\\G.{255})|\\n");

            for (String subMessage : splitted) {
                if (subMessage.trim().equals("")) continue;

                _queue.add(subMessage);
            }

            queue.remove(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendChatTick () {
        if (_queue.size() == 0) return;

        final String message = _queue.get(0);

        if (message.startsWith("/")) {
            bot.session().send(new ServerboundChatCommandPacket(
                    message.substring(1),
                    Instant.now().toEpochMilli(),
                    0L,
                    Collections.emptyList(),
                    0,
                    new BitSet()
            ));
        } else {
            bot.session().send(new ServerboundChatPacket(
                    message,
                    Instant.now().toEpochMilli(),
                    0L,
                    null,
                    0,
                    new BitSet()
            ));
        }

        _queue.remove(0);
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

    public void addListener (Listener listener) { listeners.add(listener); }

    public static class Listener {
        public void playerMessageReceived (PlayerMessage message) {}
        public void commandSpyMessageReceived (PlayerMessage message) {}
        public void systemMessageReceived (Component component) {}
    }
}
