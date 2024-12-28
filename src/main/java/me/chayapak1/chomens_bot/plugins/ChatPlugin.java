package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chatParsers.CreayunChatParser;
import me.chayapak1.chomens_bot.chatParsers.KaboomChatParser;
import me.chayapak1.chomens_bot.chatParsers.MinecraftChatParser;
import me.chayapak1.chomens_bot.chatParsers.U203aChatParser;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.data.Team;
import me.chayapak1.chomens_bot.data.chat.ChatParser;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.IllegalCharactersUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatPlugin extends Bot.Listener {
    public final Pattern CHAT_SPLIT_PATTERN;
    public static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(&[a-f0-9rlonmk])", Pattern.MULTILINE);
    public static final Pattern COLOR_CODE_END_PATTERN = Pattern.compile("^.*&[a-f0-9rlonmk]$", Pattern.MULTILINE);

    private final Bot bot;

    private final List<ChatParser> chatParsers;

    private final List<String> queue = Collections.synchronizedList(new ArrayList<>());

    public final int queueDelay;

    private final List<Listener> listeners = new ArrayList<>();

    public ChatPlugin (Bot bot) {
        this.bot = bot;
        this.CHAT_SPLIT_PATTERN = Pattern.compile("\\G\\s*([^\\r\\n]{1," + (bot.options.creayun ? 126 : 254) + "}(?=\\s|$)|[^\\r\\n]{254})"); // thanks HBot for the regex <3

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

        if (
                component instanceof TextComponent t_component &&
                        t_component.content().length() > 15_000
        ) return;

        if (component instanceof TranslatableComponent t_component) {
            final String key = t_component.key();

            if (
                    key.equals("advMode.setCommand.success") ||
                            key.equals("advMode.notAllowed") ||
                            key.equals("multiplayer.message_not_delivered")
            ) return;
        }

        PlayerMessage playerMessage = null;

        for (ChatParser parser : chatParsers) {
            playerMessage = parser.parse(component);
            if (playerMessage != null) break;
        }

        final String string = ComponentUtilities.stringify(component);
        final String ansi = ComponentUtilities.stringifyAnsi(component);

        for (Listener listener : listeners) {
            final boolean bool1 = listener.systemMessageReceived(component, string, ansi);

            if (!bool1) break;

            if (playerMessage != null) {
                final boolean bool2 = listener.playerMessageReceived(playerMessage);

                if (!bool2) break;
            }
        }
    }

    private Component getComponentByChatType (int chatType, Component name, Component message) {
        // maybe use the registry in the login packet? too lazy.,.,,.
        return switch (chatType) {
            case 0 -> Component.translatable("chat.type.text", name, message); // normal vanilla chat message
            case 1 -> Component.translatable("chat.type.emote", name, message); // /me
            case 2 -> Component.translatable("commands.message.display.incoming", name, message); // player that received /w message
            case 3 -> Component.translatable("commands.message.display.outgoing", name, message); // player that sent /w message
            case 4 -> Component.translatable("%s", name, message); // paper chat type thingy
            case 5 -> Component.translatable("chat.type.announcement", name, message); // /say
            case 6, 7 -> {
                final Team botTeam = bot.team.findTeamByMember(bot.profile.getName());

                if (botTeam == null) yield null;

                final Component botTeamDisplayName = Component
                        .translatable("chat.square_brackets")
                        .arguments(botTeam.displayName)
                        .style(botTeam.colorToStyle());

                yield chatType == 6 ?
                        Component.translatable("chat.type.team.text", botTeamDisplayName, name, message) : // player that received /teammsg message (type 6)
                        Component.translatable("chat.type.team.sent", botTeamDisplayName, name, message); // player that sent /teammsg message (type 7)
            }
            default -> null;
        };
    }

    public void packetReceived (ClientboundPlayerChatPacket packet) {
        final UUID senderUUID = packet.getSender();

        final PlayerEntry entry = bot.players.getEntry(senderUUID);

        if (entry == null) return;

        final PlayerMessage playerMessage = new PlayerMessage(
                entry,
                packet.getName(),
                Component.text(packet.getContent())
        );

        final Component unsignedContent = packet.getUnsignedContent();

        for (Listener listener : listeners) {
            final boolean bool = listener.playerMessageReceived(playerMessage);

            if (!bool) break;

            final Component chatTypeComponent = getComponentByChatType(packet.getChatType().id(), playerMessage.displayName, playerMessage.contents);

            if (chatTypeComponent != null && unsignedContent == null) {
                final String string = ComponentUtilities.stringify(chatTypeComponent);
                final String ansi = ComponentUtilities.stringifyAnsi(chatTypeComponent);

                final boolean _bool = listener.systemMessageReceived(chatTypeComponent, string, ansi);

                if (!_bool) break;
            } else {
                final String string = ComponentUtilities.stringify(unsignedContent);
                final String ansi = ComponentUtilities.stringifyAnsi(unsignedContent);

                final boolean _bool = listener.systemMessageReceived(unsignedContent, string, ansi);

                if (!_bool) break;
            }
        }
    }

    public void packetReceived (ClientboundDisguisedChatPacket packet) {
        try {
            final Component component = packet.getMessage();

            PlayerMessage parsedFromMessage = null;

            for (ChatParser parser : chatParsers) {
                parsedFromMessage = parser.parse(component);
                if (parsedFromMessage != null) break;
            }

            final Component chatTypeComponent = getComponentByChatType(packet.getChatType().id(), packet.getName(), packet.getMessage());

            if (chatTypeComponent != null && parsedFromMessage == null) {
                final String string = ComponentUtilities.stringify(chatTypeComponent);
                final String ansi = ComponentUtilities.stringifyAnsi(chatTypeComponent);

                for (Listener listener : listeners) {
                    final boolean bool = listener.systemMessageReceived(chatTypeComponent, string, ansi);

                    if (!bool) break;
                }

                for (ChatParser parser : chatParsers) {
                    final PlayerMessage parsed = parser.parse(chatTypeComponent);

                    if (parsed == null) continue;

                    final PlayerMessage playerMessage = new PlayerMessage(parsed.sender, packet.getName(), parsed.contents);

                    for (Listener listener : listeners) {
                        final boolean bool = listener.playerMessageReceived(playerMessage);

                        if (!bool) break;
                    }
                }
            } else {
                if (parsedFromMessage == null) return;

                final PlayerMessage playerMessage = new PlayerMessage(parsedFromMessage.sender, packet.getName(), parsedFromMessage.contents);

                final String string = ComponentUtilities.stringify(component);
                final String ansi = ComponentUtilities.stringifyAnsi(component);

                for (Listener listener : listeners) {
                    final boolean bool1 = listener.playerMessageReceived(playerMessage);

                    if (!bool1) break;

                    final boolean bool2 = listener.systemMessageReceived(component, string, ansi);

                    if (!bool2) break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendChatTick () {
        if (queue.size() > 100) queue.clear(); // detects spam, like spamming *echo for example

        if (queue.isEmpty()) return;

        final String message = queue.get(0);

        if (message.startsWith("/")) {
            String removedMessage = message.substring(1);

            if (bot.options.removeNamespaces) {
                final String[] splittedSpace = removedMessage.split("\\s+"); // [minecraft:test, arg1, arg2, ...]
                final String[] splittedColon = splittedSpace[0].split(":"); // [minecraft, test]
                if (splittedColon.length >= 2) {
                    removedMessage = String.join(":", Arrays.copyOfRange(splittedColon, 1, splittedColon.length));

                    if (splittedSpace.length > 1) {
                        removedMessage += " ";
                        removedMessage += String.join(" ", Arrays.copyOfRange(splittedSpace, 1, splittedSpace.length));
                    }
                }
            }

            sendCommandInstantly(removedMessage);
        } else {
            sendChatInstantly(message);
        }

        queue.remove(0);
    }

    public void sendCommandInstantly (String command) {
        if (!bot.loggedIn) return;

        bot.session.send(new ServerboundChatCommandPacket(command));
    }

    public void sendChatInstantly (String message) {
        if (!bot.loggedIn) return;

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
        if (message.startsWith("/")) {
            queue.add(message);
            return;
        }

        final Matcher splitMatcher = CHAT_SPLIT_PATTERN.matcher(message);

        String lastColor = "";

        boolean isFirst = true;

        // kinda broken but whatever
        while (splitMatcher.find()) {
            final String eachMessage = splitMatcher.group(1);

            final Matcher eachMessageMatcher = CHAT_SPLIT_PATTERN.matcher(eachMessage);

            while (eachMessageMatcher.find()) {
                String strippedMessage = IllegalCharactersUtilities.stripIllegalCharacters(eachMessageMatcher.group(1));

                if (strippedMessage.trim().isEmpty()) continue;

                final Matcher colorCodeEndMatcher = COLOR_CODE_END_PATTERN.matcher(strippedMessage);

                if (colorCodeEndMatcher.find()) strippedMessage = strippedMessage.substring(0, strippedMessage.length() - 2);

                if (!isFirst) {
                    final Matcher colorCodeEndMatcher2 = COLOR_CODE_END_PATTERN.matcher(message);

                    Matcher colorCodeMatcher;

                    if (!colorCodeEndMatcher2.find()) colorCodeMatcher = COLOR_CODE_PATTERN.matcher(message);
                    else colorCodeMatcher = COLOR_CODE_PATTERN.matcher(message.substring(0, message.length() - 2));

                    while (colorCodeMatcher.find()) lastColor = colorCodeMatcher.group();
                }

                queue.add(
                        lastColor + strippedMessage // the regex has 254 (comes from 256 - 2 (color code length)) so we can do this here
                );

                isFirst = false;
            }
        }
    }

    public void tellraw (Component component, String targets) {
        if (bot.options.useChat) {
            if (!targets.equals("@a")) return; // worst fix of all time!1!

            final String stringified = ComponentUtilities.stringifySectionSign(component).replace("§", "&");
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
        public boolean playerMessageReceived (PlayerMessage message) { return true; }
        public boolean systemMessageReceived (Component component, String string, String ansi) { return true; }
    }
}
