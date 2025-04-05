package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.*;
import org.apache.commons.lang3.tuple.Triple;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class QueryPlugin extends Bot.Listener implements ChatPlugin.Listener, TickPlugin.Listener {
    private static final String ID = "chomens_bot_query";

    private final Bot bot;

    public AtomicLong nextTransactionId = new AtomicLong(0);
    public final Map<Long, CompletableFuture<String>> transactions = new ConcurrentHashMap<>();
    public final List<UUID> ids = Collections.synchronizedList(new ArrayList<>());

    public final Queue<Component> cargosQueue = new ConcurrentLinkedQueue<>();

    public QueryPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);
        bot.chat.addListener(this);
        bot.tick.addListener(this);
    }

    @Override
    public void onSecondTick () {
        if (cargosQueue.isEmpty()) return;

        sendQueueComponent(cargosQueue);

        cargosQueue.clear();
    }

    @Override
    public boolean systemMessageReceived (Component component, String string, String ansi) {
        if (
                !(component instanceof TranslatableComponent rootTranslatable) ||
                        !rootTranslatable.key().equals(ID)
        ) return true;

        final List<TranslationArgument> arguments = rootTranslatable.arguments();

        if (arguments.size() != 1) return false;

        final Component cargosComponent = arguments.getFirst().asComponent();

        if (
                !(cargosComponent instanceof TextComponent cargosTextComponent) ||
                        !cargosTextComponent.content().isEmpty()
        ) return false;

        final List<Component> cargos = cargosComponent.children();

        for (Component cargo : cargos) processCargo(cargo);

        return false;
    }

    private void sendQueueComponent (Component component) {
        final Queue<Component> queue = new LinkedList<>();
        queue.add(component);

        // this is required so the joined component will be
        // {"extra":["abc query here"],"text":""}
        // instead of
        // {"text":"abc query here"}
        // even though that increases size,
        // i think it will be messy to implement that
        queue.add(Component.empty());

        sendQueueComponent(queue);
    }

    private void sendQueueComponent (Queue<Component> queue) {
        bot.chat.tellraw(
                Component
                        .translatable(
                                ID,
                                Component.join(
                                        JoinConfiguration.noSeparators(),
                                        queue
                                )
                        ),
                bot.profile.getId()
        );
    }

    private void processCargo (Component cargo) {
        if (!(cargo instanceof TextComponent textComponent)) return;

        final UUID inputId = UUIDUtilities.tryParse(textComponent.content());

        if (inputId == null || !ids.contains(inputId)) return;

        ids.remove(inputId);

        final List<Component> children = cargo.children();

        if (
                children.size() > 2 ||
                        !(children.getFirst() instanceof TextComponent firstChild)
        ) return;

        try {
            final long transactionId = Long.parseLong(firstChild.content());

            if (!transactions.containsKey(transactionId)) return;

            final CompletableFuture<String> future = transactions.get(transactionId);

            transactions.remove(transactionId);

            if (children.size() == 1) {
                future.complete("");
            } else if (!(children.get(1) instanceof TextComponent)) {
                future.complete(null);
            } else {
                final String stringOutput = ((TextComponent) children.get(1)).content();

                future.complete(stringOutput);
            }
        } catch (NumberFormatException ignored) { }
    }

    private Triple<CompletableFuture<String>, Long, UUID> getFutureAndId () {
        final long transactionId = nextTransactionId.getAndIncrement();

        final CompletableFuture<String> future = new CompletableFuture<>();
        transactions.put(transactionId, future);

        final UUID id = UUID.randomUUID();
        ids.add(id);

        return Triple.of(future, transactionId, id);
    }

    public CompletableFuture<String> block (Vector3i location, String path) { return block(false, location, path); }

    public CompletableFuture<String> block (boolean useCargo, Vector3i location, String path) {
        final Triple<CompletableFuture<String>, Long, UUID> triple = getFutureAndId();

        final UUID id = triple.getRight();
        final long transactionId = triple.getMiddle();

        final Component component = Component
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
                );

        if (useCargo) cargosQueue.add(component);
        else sendQueueComponent(component);

        return triple.getLeft();
    }

    public CompletableFuture<String> entity (String selector, String path) { return entity(false, selector, path); }

    public CompletableFuture<String> entity (boolean useCargo, String selector, String path) {
        final Triple<CompletableFuture<String>, Long, UUID> triple = getFutureAndId();

        final UUID id = triple.getRight();
        final long transactionId = triple.getMiddle();

        final Component component = Component
                .text(id.toString())
                .append(Component.text(transactionId))
                .append(
                        Component.entityNBT(
                                path,
                                selector
                        )
                );

        if (useCargo) cargosQueue.add(component);
        else sendQueueComponent(component);

        return triple.getLeft();
    }

    // should there be storage query?

    @Override
    public void disconnected (DisconnectedEvent event) {
        nextTransactionId.set(0);
        transactions.clear();
        ids.clear();
    }
}
