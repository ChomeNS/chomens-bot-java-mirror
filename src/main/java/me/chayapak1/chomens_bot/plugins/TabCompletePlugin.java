package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandSuggestionsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class TabCompletePlugin implements Listener {
    private final Bot bot;
    private final AtomicInteger nextTransactionId = new AtomicInteger();
    private final Map<Integer, CompletableFuture<ClientboundCommandSuggestionsPacket>> transactions = new Object2ObjectOpenHashMap<>();

    public TabCompletePlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    public CompletableFuture<ClientboundCommandSuggestionsPacket> complete (final String command) {
        if (!bot.loggedIn) return null;

        final int transactionId = nextTransactionId.getAndIncrement();

        bot.session.send(new ServerboundCommandSuggestionPacket(transactionId, command));

        final CompletableFuture<ClientboundCommandSuggestionsPacket> future = new CompletableFuture<>();
        transactions.put(transactionId, future);

        return future;
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        if (packet instanceof final ClientboundCommandSuggestionsPacket t_packet) packetReceived(t_packet);
    }

    private void packetReceived (final ClientboundCommandSuggestionsPacket packet) {
        final int id = packet.getTransactionId();

        final CompletableFuture<ClientboundCommandSuggestionsPacket> future = transactions.remove(id);
        if (future != null) future.complete(packet);
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        nextTransactionId.set(0);
        transactions.clear();
    }
}
