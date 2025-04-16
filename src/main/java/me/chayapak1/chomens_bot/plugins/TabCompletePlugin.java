package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandSuggestionsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TabCompletePlugin implements Listener {
    private final Bot bot;
    private int nextTransactionId = 0;
    private final Map<Integer, CompletableFuture<ClientboundCommandSuggestionsPacket>> transactions = new HashMap<>();

    public TabCompletePlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    public CompletableFuture<ClientboundCommandSuggestionsPacket> tabComplete (final String command) {
        if (!bot.loggedIn) return null;

        final int transactionId = nextTransactionId++;

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

        if (!transactions.containsKey(id)) return;

        transactions.remove(id).complete(packet);
    }
}
