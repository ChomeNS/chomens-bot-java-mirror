package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chatParsers.CreayunChatParser;
import me.chayapak1.chomens_bot.chatParsers.KaboomChatParser;
import me.chayapak1.chomens_bot.chatParsers.MinecraftChatParser;
import me.chayapak1.chomens_bot.chatParsers.U203aChatParser;
import me.chayapak1.chomens_bot.data.chat.ChatParser;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.data.team.Team;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.IllegalCharactersUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatPlugin extends Bot.Listener {
    public static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(&[a-f0-9rlonmk])", Pattern.MULTILINE);
    public static final Pattern COLOR_CODE_END_PATTERN = Pattern.compile("^.*&[a-f0-9rlonmk]$", Pattern.MULTILINE);

    private static final String CHAT_TYPE_REGISTRY_KEY = "minecraft:chat_type";
    private static final ChatTypeComponentRenderer CHAT_TYPE_COMPONENT_RENDERER = new ChatTypeComponentRenderer();

    private final Bot bot;

    public final Pattern CHAT_SPLIT_PATTERN;

    private final List<ChatParser> chatParsers;

    public final List<Component> chatTypes = new ArrayList<>();

    private final Queue<String> queue = new ConcurrentLinkedQueue<>();

    public final int queueDelay;

    private final List<Listener> listeners = new ArrayList<>();

    public ChatPlugin (Bot bot) {
        this.bot = bot;
        this.CHAT_SPLIT_PATTERN = Pattern.compile("\\G\\s*([^\\r\\n]{1," + (bot.options.creayun ? 126 : 254) + "}(?=\\s|$)|[^\\r\\n]{254})"); // thanks HBot for the regex <3

        queueDelay = bot.options.chatQueueDelay;

        bot.addListener(this);

        chatParsers = Collections.synchronizedList(new ArrayList<>());
        chatParsers.add(new MinecraftChatParser(bot));
        chatParsers.add(new KaboomChatParser(bot));
        chatParsers.add(new U203aChatParser(bot));
        chatParsers.add(new CreayunChatParser(bot));

        bot.executor.scheduleAtFixedRate(this::sendChatTick, 0, queueDelay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundSystemChatPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundPlayerChatPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundDisguisedChatPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundRegistryDataPacket t_packet) packetReceived(t_packet);
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
                            key.equals("multiplayer.message_not_delivered") ||

                            // arabic kaboom clone be like

                            // command set
                            key.equals("قيادة المجموعة: %s") // i'm pretty sure the text direction will depend on what you are using to view this file right now

                            // no other stuff because laziness
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
            if (!listener.systemMessageReceived(component, string, ansi)) break;

            if (playerMessage != null && !listener.playerMessageReceived(playerMessage)) break;
        }
    }

    private void packetReceived (ClientboundRegistryDataPacket packet) {
        if (!packet.getRegistry().key().equals(Key.key(CHAT_TYPE_REGISTRY_KEY))) return;

        for (RegistryEntry entry : packet.getEntries()) {
            final NbtMap data = entry.getData();

            if (data == null) continue;

            final NbtMap chat = data.getCompound("chat");

            if (chat == null) continue;

            final String translation = chat.getString("translation_key");
            final List<String> parameters = chat.getList("parameters", NbtType.STRING);
            // styles?

            final Component component = Component.translatable(
                    translation,
                    parameters
                            .stream()
                            .map(Component::text) // will be replaced later
                            .toList()
            );

            chatTypes.add(component);
        }
    }

    private Component getComponentByChatType (int chatType, Component name, Component message) {
        final Component type = chatTypes.get(chatType);
        if (type == null) return null;

        final Team botTeam = bot.team.findTeamByMember(bot.profile.getName());
        final Component botTeamDisplayName = botTeam == null ?
                Component.empty() :
                Component
                        .translatable("chat.square_brackets")
                        .arguments(botTeam.displayName)
                        .style(botTeam.colorToStyle());

        return CHAT_TYPE_COMPONENT_RENDERER.render(
                type,
                new ChatTypeContext(
                        botTeamDisplayName,
                        name,
                        message
                )
        );
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
            if (!listener.playerMessageReceived(playerMessage)) break;

            final Component chatTypeComponent = getComponentByChatType(packet.getChatType().id(), playerMessage.displayName, playerMessage.contents);

            if (chatTypeComponent != null && unsignedContent == null) {
                final String string = ComponentUtilities.stringify(chatTypeComponent);
                final String ansi = ComponentUtilities.stringifyAnsi(chatTypeComponent);

                if (!listener.systemMessageReceived(chatTypeComponent, string, ansi)) break;
            } else {
                final String string = ComponentUtilities.stringify(unsignedContent);
                final String ansi = ComponentUtilities.stringifyAnsi(unsignedContent);

                if (!listener.systemMessageReceived(unsignedContent, string, ansi)) break;
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
                    if (!listener.systemMessageReceived(chatTypeComponent, string, ansi)) break;
                }

                for (ChatParser parser : chatParsers) {
                    final PlayerMessage parsed = parser.parse(chatTypeComponent);

                    if (parsed == null) continue;

                    final PlayerMessage playerMessage = new PlayerMessage(parsed.sender, packet.getName(), parsed.contents);

                    for (Listener listener : listeners) {
                        if (!listener.playerMessageReceived(playerMessage)) break;
                    }
                }
            } else {
                if (parsedFromMessage == null) return;

                final PlayerMessage playerMessage = new PlayerMessage(parsedFromMessage.sender, packet.getName(), parsedFromMessage.contents);

                final String string = ComponentUtilities.stringify(component);
                final String ansi = ComponentUtilities.stringifyAnsi(component);

                for (Listener listener : listeners) {
                    if (!listener.playerMessageReceived(playerMessage)) break;
                    if (!listener.systemMessageReceived(component, string, ansi)) break;
                }
            }
        } catch (Exception e) {
            bot.logger.error(e);
        }
    }

    private void sendChatTick () {
        if (queue.size() > 100) queue.clear(); // detects spam, like spamming *echo for example

        final String message = queue.poll();

        if (message == null) return;

        if (message.startsWith("/")) {
            String removedMessage = message.substring(1);

            if (!bot.serverFeatures.hasNamespaces) {
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

    public interface Listener {
        default boolean playerMessageReceived (PlayerMessage message) { return true; }
        default boolean systemMessageReceived (Component component, String string, String ansi) { return true; }
    }

    private record ChatTypeContext (Component target, Component sender, Component content) {}

    private static class ChatTypeComponentRenderer extends TranslatableComponentRenderer<ChatTypeContext> {
        @Override
        protected @NotNull Component renderText (@NotNull TextComponent component, @NotNull ChatTypeContext context) {
            return switch (component.content()) {
                case "target" -> context.target();
                case "sender" -> context.sender();
                case "content" -> context.content();
                default -> component;
            };
        }
    }
}
