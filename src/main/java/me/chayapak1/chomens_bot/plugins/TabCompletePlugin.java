package me.chayapak1.chomens_bot.plugins;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandSuggestionsPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;
import me.chayapak1.chomens_bot.Bot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TabCompletePlugin extends Bot.Listener {
    private final Bot bot;
    private int nextTransactionId = 0;
    private final Map<Integer, CompletableFuture<ClientboundCommandSuggestionsPacket>> transactions = new HashMap<>();

    public TabCompletePlugin (Bot bot) {
        this.bot = bot;
        bot.addListener(this);
    }

    public CompletableFuture<ClientboundCommandSuggestionsPacket> tabComplete (String command) {
        if (!bot.loggedIn) return null;

        final int transactionId = nextTransactionId++;

        bot.session.send(new ServerboundCommandSuggestionPacket(transactionId, command));

        final CompletableFuture<ClientboundCommandSuggestionsPacket> future = new CompletableFuture<>();
        transactions.put(transactionId, future);

        return future;
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundCommandSuggestionsPacket) packetReceived((ClientboundCommandSuggestionsPacket) packet);
    }

    public void packetReceived (ClientboundCommandSuggestionsPacket packet) {
        final int id = packet.getTransactionId();

        transactions.get(id).complete(packet);

        transactions.remove(id);
    }
}
