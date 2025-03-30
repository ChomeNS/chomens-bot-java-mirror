package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.BlockNBTComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.apache.commons.lang3.tuple.Triple;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class QueryPlugin extends Bot.Listener implements ChatPlugin.Listener {
    private final Bot bot;

    public long nextTransactionId = 0;
    public final Map<Long, CompletableFuture<String>> transactions = new HashMap<>();
    public final List<UUID> ids = new ArrayList<>();

    public QueryPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);
        bot.chat.addListener(this);
    }

    @Override
    public boolean systemMessageReceived (Component component, String string, String ansi) {
        if (!(component instanceof TextComponent textComponent)) return true;

        final UUID inputId = UUIDUtilities.tryParse(textComponent.content());

        if (inputId == null || !ids.contains(inputId)) return true;

        ids.remove(inputId);

        final List<Component> children = component.children();

        if (
                children.size() > 2 ||
                        !(children.getFirst() instanceof TextComponent firstChild)
        ) return false;

        try {
            final long transactionId = Long.parseLong(firstChild.content());

            if (!transactions.containsKey(transactionId)) return false;

            final CompletableFuture<String> future = transactions.get(transactionId);

            transactions.remove(transactionId);

            if (children.size() == 1 || !(children.get(1) instanceof TextComponent)) {
                future.complete(null);
            } else {
                final String stringOutput = ((TextComponent) children.get(1)).content();

                future.complete(stringOutput);
            }

            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private Triple<CompletableFuture<String>, Long, UUID> getFutureAndId () {
        final long transactionId = nextTransactionId++;

        final CompletableFuture<String> future = new CompletableFuture<>();
        transactions.put(transactionId, future);

        final UUID id = UUID.randomUUID();
        ids.add(id);

        return Triple.of(future, transactionId, id);
    }

    public CompletableFuture<String> block (Vector3i location, String path) {
        final Triple<CompletableFuture<String>, Long, UUID> triple = getFutureAndId();

        final UUID id = triple.getRight();
        final long transactionId = triple.getMiddle();

        bot.chat.tellraw(
                Component
                        .text(id.toString())
                        .append(Component.text(transactionId))
                        .append(
                                Component.blockNBT(
                                        path,

                                        BlockNBTComponent.WorldPos.worldPos(
                                                BlockNBTComponent.WorldPos.Coordinate.absolute(location.getX()),
                                                BlockNBTComponent.WorldPos.Coordinate.absolute(location.getY()),
                                                BlockNBTComponent.WorldPos.Coordinate.absolute(location.getZ())
                                        )
                                )
                        ),
                bot.profile.getId()
        );

        return triple.getLeft();
    }

    public CompletableFuture<String> entity (String selector, String path) {
        final Triple<CompletableFuture<String>, Long, UUID> triple = getFutureAndId();

        final UUID id = triple.getRight();
        final long transactionId = triple.getMiddle();

        bot.chat.tellraw(
                Component
                        .text(id.toString())
                        .append(Component.text(transactionId))
                        .append(
                                Component.entityNBT(
                                        path,
                                        selector
                                )
                        ),
                bot.profile.getId()
        );

        return triple.getLeft();
    }

    // should there be storage query?

    @Override
    public void disconnected (DisconnectedEvent event) {
        nextTransactionId = 0;
        transactions.clear();
        ids.clear();
    }
}
