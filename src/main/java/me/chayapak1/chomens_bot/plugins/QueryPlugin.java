package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.util.RandomStringUtilities;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class QueryPlugin implements Listener {
    private static final String ID = "chomens_bot_query";

    private final Bot bot;

    public final Map<String, CompletableFuture<String>> requests = new ConcurrentHashMap<>();

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
    public boolean onSystemMessageReceived (
            final Component component,
            final ChatPacketType packetType,
            final String string,
            final String ansi
    ) {
        if (
                packetType != ChatPacketType.SYSTEM
                        || !(component instanceof final TranslatableComponent rootTranslatable)
                        || !rootTranslatable.key().equals(ID)
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
        if (!(cargo instanceof final TextComponent idTextComponent)) return;

        final String id = idTextComponent.content();
        if (id.length() != 16) return;

        final CompletableFuture<String> future = requests.get(id);
        if (future == null) return;
        requests.remove(id);

        final boolean interpret = id.endsWith("1");

        final List<Component> children = cargo.children();
        if (children.size() > 1) return;

        if (children.isEmpty()) {
            future.complete("");
        } else if (!interpret && !(children.getFirst() instanceof TextComponent)) {
            future.complete(null);
        } else {
            final String stringOutput = interpret
                    ? GsonComponentSerializer.gson().serialize(children.getFirst()) // seems very
                    : ((TextComponent) children.getFirst()).content();

            future.complete(stringOutput);
        }
    }

    private ObjectObjectImmutablePair<String, CompletableFuture<String>> getFutureAndId (final boolean interpret) {
        final String id = RandomStringUtilities.generate(15) + (interpret ? "1" : "0");

        final CompletableFuture<String> future = new CompletableFuture<>();
        requests.put(id, future);

        return ObjectObjectImmutablePair.of(id, future);
    }

    private void addComponent (final Component component, final boolean useCargo) {
        if (useCargo || !bot.core.hasEnoughPermissions()) cargosQueue.add(component);
        else sendQueueComponent(component);
    }

    public CompletableFuture<String> block (final Vector3i location, final String path) { return block(false, location, path, false); }

    public CompletableFuture<String> block (final boolean useCargo, final Vector3i location, final String path, final boolean interpret) {
        final Pair<String, CompletableFuture<String>> pair = getFutureAndId(interpret);

        final String id = pair.left();

        final Component component = Component
                .text(id)
                .append(
                        Component.blockNBT(
                                path,

                                interpret,

                                BlockNBTComponent.WorldPos.worldPos(
                                        BlockNBTComponent.WorldPos.Coordinate.absolute(location.getX()),
                                        BlockNBTComponent.WorldPos.Coordinate.absolute(location.getY()),
                                        BlockNBTComponent.WorldPos.Coordinate.absolute(location.getZ())
                                )
                        )
                );

        addComponent(component, useCargo);

        return pair.right();
    }

    public CompletableFuture<String> entity (final String selector, final String path) { return entity(false, selector, path); }

    public CompletableFuture<String> entity (final boolean useCargo, final String selector, final String path) {
        final Pair<String, CompletableFuture<String>> pair = getFutureAndId(false);

        final String id = pair.left();

        final Component component = Component
                .text(id)
                .append(
                        Component.entityNBT(
                                path,
                                selector
                        )
                );

        addComponent(component, useCargo);

        return pair.right();
    }

    // should there be storage query?

    @Override
    public void disconnected (final DisconnectedEvent event) {
        requests.clear();
        cargosQueue.clear();
    }
}
