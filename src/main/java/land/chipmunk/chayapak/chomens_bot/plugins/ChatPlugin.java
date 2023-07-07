package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.chatParsers.CreayunChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.KaboomChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.MinecraftChatParser;
import land.chipmunk.chayapak.chomens_bot.chatParsers.U203aChatParser;
import land.chipmunk.chayapak.chomens_bot.data.chat.ChatParser;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.IllegalCharactersUtilities;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatPlugin extends Bot.Listener {
    public static final Pattern CHAT_SPLIT_PATTERN = Pattern.compile("\\G\\s*([^\\r\\n]{1,254}(?=\\s|$)|[^\\r\\n]{254})"); // thanks HBot for the regex <3
    public static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(&[a-f0-9rlonmk])");

    private final Bot bot;

    private final List<ChatParser> chatParsers;

    private final List<String> queue = new ArrayList<>();

    public final int queueDelay;

    private final List<Listener> listeners = new ArrayList<>();

    public ChatPlugin (Bot bot) {
        this.bot = bot;

        queueDelay = bot.options.chatQueueDelay;

        bot.addListener(this);

        chatParsers = new ArrayList<>();
        chatParsers.add(new MinecraftChatParser(bot));
        chatParsers.add(new KaboomChatParser(bot));
        chatParsers.add(new U203aChatParser(bot));
        chatParsers.add(new CreayunChatParser(bot));

        bot.executor.scheduleAtFixedRate(this::sendChatTick, 0, queueDelay, TimeUnit.MILLISECONDS);
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

        for (Listener listener : listeners) {
            listener.systemMessageReceived(component);
            if (playerMessage != null) listener.playerMessageReceived(playerMessage);
        }
    }

    private String getTranslationByChatType (int chatType) {
        String translation = null;

        // maybe use the registry in the login packet? too lazy.,.,,.
        switch (chatType) {
            case 0 -> translation = "chat.type.text"; // normal vanilla chat message
            case 1 -> translation = "chat.type.emote"; // /me
            case 2 -> translation = "commands.message.display.incoming"; // player that received /w message
            case 3 -> translation = "commands.message.display.outgoing"; // player that sent /w message
            case 4 -> translation = "chat.type.announcement"; // /say
            case 5 -> translation = "chat.type.team.text"; // player that received /teammsg message
            case 6 -> translation = "chat.type.team.sent"; // player that sent /teammsg message
            case 7 -> translation = "%s"; // raw, idk when and where this is used
        }

        return translation;
    }

    public void packetReceived (ClientboundPlayerChatPacket packet) {
        final UUID senderUUID = packet.getSender();

        final MutablePlayerListEntry entry = bot.players.getEntry(senderUUID);

        if (entry == null) return;

        final PlayerMessage playerMessage = new PlayerMessage(
                entry,
                packet.getName(),
                Component.text(packet.getContent())
        );

        for (Listener listener : listeners) {
            listener.playerMessageReceived(playerMessage);

            final Component unsignedContent = packet.getUnsignedContent();

            final String translation = getTranslationByChatType(packet.getChatType());

            if (translation != null && unsignedContent == null) {
                TranslatableComponent component = Component.translatable(translation);

                if (translation.equals("chat.type.team.text") || translation.equals("chat.type.team.sent")) { // ohio
                    component = component.args(
                            Component.empty(), // TODO: fix team name.,.,
                            playerMessage.displayName,
                            playerMessage.contents
                    );
                } else {
                    component = component.args(playerMessage.displayName, playerMessage.contents);
                }

                listener.systemMessageReceived(component);
            } else {
                listener.systemMessageReceived(unsignedContent);
            }
        }
    }

    public void packetReceived (ClientboundDisguisedChatPacket packet) {
        try {
            final String translation = getTranslationByChatType(packet.getChatType());

            final Component component = packet.getMessage();

            PlayerMessage parsedFromMessage = null;

            for (ChatParser parser : chatParsers) {
                parsedFromMessage = parser.parse(component);
                if (parsedFromMessage != null) break;
            }

            if (translation != null && parsedFromMessage == null) {
                final Component name = packet.getName();
                final Component content = packet.getMessage();

                TranslatableComponent translatableComponent = Component.translatable(translation);

                if (translation.equals("chat.type.team.text") || translation.equals("chat.type.team.sent")) { // ohio
                    translatableComponent = translatableComponent.args(
                            Component.empty(), // TODO: fix team name.,.,
                            name,
                            content
                    );
                } else {
                    translatableComponent = translatableComponent.args(name, content);
                }

                for (Listener listener : listeners) {
                    listener.systemMessageReceived(translatableComponent);
                }

                for (ChatParser parser : chatParsers) {
                    final PlayerMessage parsed = parser.parse(translatableComponent);

                    if (parsed == null) continue;

                    final PlayerMessage playerMessage = new PlayerMessage(parsed.sender, packet.getName(), parsed.contents);

                    for (Listener listener : listeners) {
                        listener.playerMessageReceived(playerMessage);
                    }
                }
            } else {
                if (parsedFromMessage == null) return;

                final PlayerMessage playerMessage = new PlayerMessage(parsedFromMessage.sender, packet.getName(), parsedFromMessage.contents);

                for (Listener listener : listeners) {
                    listener.playerMessageReceived(playerMessage);
                    listener.systemMessageReceived(component);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendChatTick () {
        if (queue.isEmpty()) return;

        final String message = queue.get(0);

        if (message.startsWith("/")) {
            String removedMessage = message.substring(1);

            final String[] splittedSpace = removedMessage.split("\\s+"); // [minecraft:test, arg1, arg2, ...]
            final String[] splittedColon = splittedSpace[0].split(":"); // [minecraft, test]
            if (bot.options.removeNamespaces && splittedColon.length >= 2) {
                removedMessage = String.join(":", Arrays.copyOfRange(splittedColon, 1, splittedColon.length));

                if (splittedSpace.length > 1) {
                    removedMessage += " ";
                    removedMessage += String.join(" ", Arrays.copyOfRange(splittedSpace, 1, splittedSpace.length));
                }
            }

            sendCommandInstantly(removedMessage);
        } else {
            sendChatInstantly(message);
        }

        queue.remove(0);
    }

    public void sendCommandInstantly (String command) {
        bot.session.send(new ServerboundChatCommandPacket(
                command,
                Instant.now().toEpochMilli(),
                0L,
                Collections.emptyList(),
                0,
                new BitSet()
        ));
    }

    public void sendChatInstantly (String message) {
        bot.session.send(new ServerboundChatPacket(
                message,
                Instant.now().toEpochMilli(),
                0L,
                null,
                0,
                new BitSet()
        ));
    }

    public void clearQueue () { queue.clear(); }

    public void send (String message) {
        final Matcher splitMatcher = CHAT_SPLIT_PATTERN.matcher(message);

        String lastColor = "";

        boolean isFirst = true;

        // kinda broken but whatever
        while (splitMatcher.find()) {
            final String eachMessage = splitMatcher.group(1);

            if (
                    eachMessage.trim().isEmpty() ||
                            IllegalCharactersUtilities.containsIllegalCharacters(eachMessage)
            ) continue;

            if (!isFirst) {
                final Matcher colorCodeMatcher = COLOR_CODE_PATTERN.matcher(message);
                while (colorCodeMatcher.find()) lastColor = colorCodeMatcher.group();
            }

            queue.add(
                    lastColor + eachMessage // the regex has 254 (comes from 256 - 2 (color code length)) so we can do this here
            );

            isFirst = false;
        }
    }

    public void tellraw (Component component, String targets) {
        if (bot.options.useChat) {
            if (!targets.equals("@a")) return; // worst fix of all time!1!

            final String stringified = ComponentUtilities.stringifyMotd(component).replace("§", "&");
            send(stringified);
        } else {
            bot.core.run("minecraft:tellraw " + targets + " " + GsonComponentSerializer.gson().serialize(component));
        }
    }

    public void tellraw (Component component, UUID uuid) { tellraw(component, UUIDUtilities.selector(uuid)); }

    public void tellraw (Component component) { tellraw(component, "@a"); }

    public void actionBar (Component component, String targets) {
        if (bot.options.useChat) return;
        bot.core.run("minecraft:title " + targets + " actionbar " + GsonComponentSerializer.gson().serialize(component));
    }

    public void actionBar (Component component, UUID uuid) { actionBar(component, UUIDUtilities.selector(uuid)); }

    public void actionBar (Component component) { actionBar(component, "@a"); }

    public void addListener (Listener listener) { listeners.add(listener); }

    public static class Listener {
        public void playerMessageReceived (PlayerMessage message) {}
        public void systemMessageReceived (Component component) {}
    }
}
