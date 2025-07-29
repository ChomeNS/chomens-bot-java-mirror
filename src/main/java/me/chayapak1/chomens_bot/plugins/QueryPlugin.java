package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
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

        if (!(cargosComponent instanceof TextComponent)) return false;

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
        final String id = cargo.insertion();

        if (
                !(cargo instanceof TextComponent)
                        || id == null
                        || id.length() != (4 + 1)
        ) return;

        final CompletableFuture<String> future = requests.get(id);
        if (future == null) return;
        requests.remove(id);

        final boolean interpret = id.endsWith("1");

        final Component result = cargo.insertion(null);

        if (result.equals(Component.empty())) {
            future.complete(null);
        } else {
            final String stringOutput = interpret
                    ? GsonComponentSerializer.gson().serialize(result) // seems very
                    : ComponentUtilities.stringify(result);

            future.complete(stringOutput);
        }
    }

    private ObjectObjectImmutablePair<String, CompletableFuture<String>> getFutureAndId (final boolean interpret) {
        final String id = String.format(
                "%s%s",
                // 4 characters are enough, (26 * 2) ** 4 = 7,311,616 (we need at least 150 * 1000)
                // (26 * 2 = a-zA-Z, ** 4 = 4 characters)
                RandomStringUtilities.generate(4, RandomStringUtilities.ALPHABETS_ONLY),
                interpret ? "1" : "0"
        );

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

        final Component component =
                Component
                        .blockNBT(
                                path,
                                interpret,
                                BlockNBTComponent.WorldPos.worldPos(
                                        BlockNBTComponent.WorldPos.Coordinate.absolute(location.getX()),
                                        BlockNBTComponent.WorldPos.Coordinate.absolute(location.getY()),
                                        BlockNBTComponent.WorldPos.Coordinate.absolute(location.getZ())
                                )
                        )
                        .insertion(id);

        addComponent(component, useCargo);

        return pair.right();
    }

    public CompletableFuture<String> entity (final String selector, final String path) { return entity(false, selector, path); }

    public CompletableFuture<String> entity (final boolean useCargo, final String selector, final String path) {
        final Pair<String, CompletableFuture<String>> pair = getFutureAndId(false);

        final String id = pair.left();

        final Component component =
                Component
                        .entityNBT(path, selector)
                        .insertion(id);

        addComponent(component, useCargo);

        return pair.right();
    }

    // should there be a storage query too? right now it won't be used though...

    @Override
    public void disconnected (final DisconnectedEvent event) {
        requests.clear();
        cargosQueue.clear();
    }
}
