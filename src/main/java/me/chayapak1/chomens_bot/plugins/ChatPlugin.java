package me.chayapak1.chomens_bot.plugins;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chatParsers.KaboomChatParser;
import me.chayapak1.chomens_bot.chatParsers.MinecraftChatParser;
import me.chayapak1.chomens_bot.chatParsers.U203aChatParser;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.chat.ChatParser;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.codec.NbtComponentSerializer;
import org.geysermc.mcprotocollib.protocol.data.DefaultComponentSerializer;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDisguisedChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.BitSet;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatPlugin implements Listener {
    public static final Pattern COLOR_CODE_PATTERN = Pattern.compile("(&[a-f0-9rlonmk])", Pattern.MULTILINE);
    public static final Pattern COLOR_CODE_END_PATTERN = Pattern.compile("^.*&[a-f0-9rlonmk]$", Pattern.MULTILINE);
    public static final Pattern CHAT_SPLIT_PATTERN = Pattern.compile("\\G\\s*([^\\r\\n]{1,254}(?=\\s|$)|[^\\r\\n]{254})");

    private static final String CHAT_TYPE_REGISTRY_KEY = "minecraft:chat_type";
    private static final ChatTypeComponentRenderer CHAT_TYPE_COMPONENT_RENDERER = new ChatTypeComponentRenderer();

    private final Bot bot;

    private final List<ChatParser> chatParsers = new ObjectArrayList<>();

    public final List<Component> chatTypes = new ObjectArrayList<>();

    private final Queue<String> queue = new ConcurrentLinkedQueue<>();

    public final int queueDelay;

    public ChatPlugin (final Bot bot) {
        this.bot = bot;

        queueDelay = bot.options.chatQueueDelay;

        bot.listener.addListener(this);

        chatParsers.add(new MinecraftChatParser(bot));
        chatParsers.add(new KaboomChatParser(bot));
        chatParsers.add(new U203aChatParser(bot));

        bot.executor.scheduleAtFixedRate(this::sendChatTick, 0, queueDelay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        switch (packet) {
            case final ClientboundSystemChatPacket t_packet -> packetReceived(t_packet);
            case final ClientboundPlayerChatPacket t_packet -> packetReceived(t_packet);
            case final ClientboundDisguisedChatPacket t_packet -> packetReceived(t_packet);
            case final ClientboundRegistryDataPacket t_packet -> packetReceived(t_packet);
            default -> { }
        }
    }

    private void packetReceived (final ClientboundSystemChatPacket packet) {
        final Component component = packet.getContent();

        if (
                packet.isOverlay() ||
                        (component instanceof final TextComponent t_component
                                && t_component.content().length() > 15_000)
        ) return;

        if (component instanceof final TranslatableComponent t_component) {
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

        for (final ChatParser parser : chatParsers) {
            playerMessage = parser.parse(component);
            if (playerMessage != null) break;
        }

        final String string = ComponentUtilities.stringify(component);
        final String ansi = ComponentUtilities.stringifyAnsi(component);

        final PlayerMessage finalPlayerMessage = playerMessage; // ???
        bot.listener.dispatchWithCheck(listener -> {
            if (!listener.onSystemMessageReceived(component, string, ansi)) return false;

            return finalPlayerMessage == null
                    || listener.onPlayerMessageReceived(finalPlayerMessage, ChatPacketType.SYSTEM);
        });
    }

    private void packetReceived (final ClientboundRegistryDataPacket packet) {
        if (!packet.getRegistry().key().equals(Key.key(CHAT_TYPE_REGISTRY_KEY))) return;

        for (final RegistryEntry entry : packet.getEntries()) {
            final NbtMap data = entry.getData();

            if (data == null) continue;

            final NbtMap chat = data.getCompound("chat");

            if (chat == null) continue;

            final String translation = chat.getString("translation_key");
            final List<String> parameters = chat.getList("parameters", NbtType.STRING);

            final NbtMap styleMap = chat.getCompound("style", null);

            Component style = Component.empty();

            if (styleMap != null) {
                JsonElement json = NbtComponentSerializer.tagComponentToJson(styleMap);

                if (json.isJsonObject() && json.getAsJsonObject().get("text") == null) {
                    final JsonObject object = json.getAsJsonObject();
                    object.addProperty("text", "");
                    json = object; // is this necessary?
                }

                style = DefaultComponentSerializer.get().deserializeFromTree(json);
            }

            final Component component = Component
                    .translatable(
                            translation,
                            parameters
                                    .stream()
                                    .map(Component::text) // will be replaced later
                                    .toList()
                    )
                    .mergeStyle(style);

            chatTypes.add(component);
        }
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        chatTypes.clear();
    }

    private Component getComponentByChatType (final int chatType, final Component target, final Component sender, final Component content) {
        final Component type = chatTypes.get(chatType);

        if (type == null) return null;

        return CHAT_TYPE_COMPONENT_RENDERER.render(
                type,
                new ChatTypeContext(
                        target,
                        sender,
                        content
                )
        );
    }

    private void packetReceived (final ClientboundPlayerChatPacket packet) {
        final UUID senderUUID = packet.getSender();

        final PlayerEntry entry = bot.players.getEntry(senderUUID);

        if (entry == null) return;

        final PlayerMessage playerMessage = new PlayerMessage(
                entry,
                packet.getName(),
                Component.text(packet.getContent())
        );

        final Component unsignedContent = packet.getUnsignedContent();

        bot.listener.dispatchWithCheck(listener -> {
            if (!listener.onPlayerMessageReceived(playerMessage, ChatPacketType.PLAYER)) return false;

            final Component chatTypeComponent = getComponentByChatType(
                    packet.getChatType().id(),
                    packet.getTargetName(),
                    packet.getName(),
                    playerMessage.contents()
            );

            if (chatTypeComponent != null && unsignedContent == null) {
                final String string = ComponentUtilities.stringify(chatTypeComponent);
                final String ansi = ComponentUtilities.stringifyAnsi(chatTypeComponent);

                return listener.onSystemMessageReceived(chatTypeComponent, string, ansi);
            } else {
                final String string = ComponentUtilities.stringify(unsignedContent);
                final String ansi = ComponentUtilities.stringifyAnsi(unsignedContent);

                return listener.onSystemMessageReceived(unsignedContent, string, ansi);
            }
        });
    }

    private void packetReceived (final ClientboundDisguisedChatPacket packet) {
        final Component component = packet.getMessage();

        PlayerMessage parsedFromMessage = null;

        for (final ChatParser parser : chatParsers) {
            parsedFromMessage = parser.parse(component);
            if (parsedFromMessage != null) break;
        }

        final Component chatTypeComponent = getComponentByChatType(
                packet.getChatType().id(),
                packet.getTargetName(),
                packet.getName(),
                packet.getMessage()
        );

        if (chatTypeComponent != null && parsedFromMessage == null) {
            final String string = ComponentUtilities.stringify(chatTypeComponent);
            final String ansi = ComponentUtilities.stringifyAnsi(chatTypeComponent);

            bot.listener.dispatchWithCheck(listener ->
                                                   listener.onSystemMessageReceived(chatTypeComponent, string, ansi));

            for (final ChatParser parser : chatParsers) {
                final PlayerMessage parsed = parser.parse(chatTypeComponent);

                if (parsed == null) continue;

                final PlayerMessage playerMessage = new PlayerMessage(
                        parsed.sender(),
                        packet.getName(),
                        parsed.contents()
                );

                bot.listener.dispatchWithCheck(listener -> listener.onPlayerMessageReceived(
                        playerMessage, ChatPacketType.DISGUISED
                ));
            }
        } else {
            if (parsedFromMessage == null) return;

            final PlayerMessage playerMessage = new PlayerMessage(
                    parsedFromMessage.sender(),
                    packet.getName(),
                    parsedFromMessage.contents()
            );

            final String string = ComponentUtilities.stringify(component);
            final String ansi = ComponentUtilities.stringifyAnsi(component);

            bot.listener.dispatchWithCheck(listener -> {
                if (!listener.onPlayerMessageReceived(playerMessage, ChatPacketType.DISGUISED)) return false;
                return listener.onSystemMessageReceived(component, string, ansi);
            });
        }
    }

    private void sendChatTick () {
        if (queue.size() > 100) queue.clear(); // detects spam, like spamming *echo for example

        final String message = queue.poll();

        if (message == null) return;

        if (message.startsWith("/")) {
            final String slashRemoved = message.substring(1);
            sendCommandInstantly(slashRemoved);
        } else {
            sendChatInstantly(message);
        }
    }

    public void sendCommandInstantly (final String command) {
        if (!bot.loggedIn) return;

        final String namespaceSanitizedCommand =
                bot.serverFeatures.hasNamespaces ?
                        command :
                        StringUtilities.removeNamespace(command);

        bot.session.send(new ServerboundChatCommandPacket(namespaceSanitizedCommand));
    }

    public void sendChatInstantly (final String message) {
        if (!bot.loggedIn) return;

        bot.session.send(new ServerboundChatPacket(
                StringUtilities.truncateToFitUtf8ByteLength(message, 256),
                Instant.now().toEpochMilli(),
                0L,
                null,
                0,
                new BitSet(),
                0
        ));
    }

    public void clearQueue () { queue.clear(); }

    public void send (final String message) {
        if (message.startsWith("/")) {
            queue.add(message);
            return;
        }

        final Matcher colorCodeMatcher = COLOR_CODE_PATTERN.matcher(message);

        final List<Integer> colorCodePositions = new ObjectArrayList<>();
        final List<String> colorCodes = new ObjectArrayList<>();

        while (colorCodeMatcher.find()) {
            colorCodePositions.add(colorCodeMatcher.start());
            colorCodes.add(colorCodeMatcher.group());
        }

        String lastColor = "";
        int colorCodeIndex = 0;

        final Matcher splitMatcher = CHAT_SPLIT_PATTERN.matcher(message);
        boolean isFirst = true;

        while (splitMatcher.find()) {
            final String eachMessage = splitMatcher.group(1);
            String strippedMessage = IllegalCharactersUtilities.stripIllegalCharacters(eachMessage);

            if (strippedMessage.trim().isEmpty()) continue;

            if (COLOR_CODE_END_PATTERN.matcher(strippedMessage).find()) {
                strippedMessage = strippedMessage.substring(0, strippedMessage.length() - 2);
            }

            if (!isFirst) {
                final int currentPos = splitMatcher.start(1);

                while (colorCodeIndex < colorCodePositions.size() && colorCodePositions.get(colorCodeIndex) < currentPos) {
                    lastColor = colorCodes.get(colorCodeIndex);
                    colorCodeIndex++;
                }
            }

            queue.add(lastColor + strippedMessage);
            isFirst = false;
        }
    }

    public void tellraw (final Component component, final String targets) {
        if (bot.options.useChat) {
            if (!targets.equals("@a")) return; // worst fix of all time!1!

            final String stringified = ComponentUtilities.stringifySectionSign(component).replace("§", "&");
            send(stringified);
        } else {
            bot.core.run("minecraft:tellraw " + targets + " " + SNBTUtilities.fromComponent(bot, component));
        }
    }

    public void tellraw (final Component component, final UUID uuid) { tellraw(component, UUIDUtilities.selector(uuid)); }

    public void tellraw (final Component component) { tellraw(component, "@a"); }

    public void actionBar (final Component component, final String targets) {
        if (bot.options.useChat) return;
        bot.core.run("minecraft:title " + targets + " actionbar " + SNBTUtilities.fromComponent(bot, component));
    }

    public void actionBar (final Component component, final UUID uuid) { actionBar(component, UUIDUtilities.selector(uuid)); }

    public void actionBar (final Component component) { actionBar(component, "@a"); }

    private record ChatTypeContext(Component target, Component sender, Component content) { }

    private static class ChatTypeComponentRenderer extends TranslatableComponentRenderer<ChatTypeContext> {
        @Override
        protected @NotNull Component renderText (@NotNull final TextComponent component, @NotNull final ChatTypeContext context) {
            return switch (component.content()) {
                case "target" -> context.target();
                case "sender" -> context.sender();
                case "content" -> context.content();
                default -> component;
            };
        }
    }
}
