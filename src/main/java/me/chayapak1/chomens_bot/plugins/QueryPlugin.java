package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
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
import java.util.stream.Collectors;

public class QueryPlugin implements Listener {
    private static final String ID = "chomens_bot_query";

    private final Bot bot;

    public AtomicLong nextTransactionId = new AtomicLong(0);
    public final Map<Long, CompletableFuture<String>> transactions = new ConcurrentHashMap<>();
    public final List<UUID> ids = Collections.synchronizedList(new ArrayList<>());

    public final Queue<Component> cargosQueue = new ConcurrentLinkedQueue<>();

    public QueryPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    @Override
    public void onSecondTick () {
        if (cargosQueue.isEmpty()) return;

        if (cargosQueue.size() > 1000) {
            cargosQueue.clear();
            return;
        }

        final Set<Component> set = cargosQueue.stream()
                .limit(150) // due to how bloated the components are, we can only go up to around 150
                .collect(Collectors.toUnmodifiableSet());

        sendQueueComponent(set);

        for (int i = 0; i < set.size(); i++) cargosQueue.poll();
    }

    @Override
    public boolean onSystemMessageReceived (final Component component, final String string, final String ansi) {
        if (
                !(component instanceof final TranslatableComponent rootTranslatable) ||
                        !rootTranslatable.key().equals(ID)
        ) return true;

        final List<TranslationArgument> arguments = rootTranslatable.arguments();

        if (arguments.size() != 1) return false;

        final Component cargosComponent = arguments.getFirst().asComponent();

        if (
                !(cargosComponent instanceof final TextComponent cargosTextComponent) ||
                        !cargosTextComponent.content().isEmpty()
        ) return false;

        final List<Component> cargos = cargosComponent.children();

        for (final Component cargo : cargos) processCargo(cargo);

        return false;
    }

    private void sendQueueComponent (final Component component) {
        final Set<Component> queue = new HashSet<>();
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

    private void sendQueueComponent (final Set<Component> queue) {
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

    private void processCargo (final Component cargo) {
        if (!(cargo instanceof final TextComponent textComponent)) return;

        final UUID inputId = UUIDUtilities.tryParse(textComponent.content());

        if (inputId == null || !ids.contains(inputId)) return;

        ids.remove(inputId);

        final List<Component> children = cargo.children();

        if (
                children.size() > 2 ||
                        !(children.getFirst() instanceof final TextComponent firstChild)
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
        } catch (final NumberFormatException ignored) { }
    }

    private Triple<CompletableFuture<String>, Long, UUID> getFutureAndId () {
        final long transactionId = nextTransactionId.getAndIncrement();

        final CompletableFuture<String> future = new CompletableFuture<>();
        transactions.put(transactionId, future);

        final UUID id = UUID.randomUUID();
        ids.add(id);

        return Triple.of(future, transactionId, id);
    }

    public CompletableFuture<String> block (final Vector3i location, final String path) { return block(false, location, path); }

    public CompletableFuture<String> block (final boolean useCargo, final Vector3i location, final String path) {
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

    public CompletableFuture<String> entity (final String selector, final String path) { return entity(false, selector, path); }

    public CompletableFuture<String> entity (final boolean useCargo, final String selector, final String path) {
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
    public void disconnected (final DisconnectedEvent event) {
        nextTransactionId.set(0);
        transactions.clear();
        ids.clear();
    }
}
