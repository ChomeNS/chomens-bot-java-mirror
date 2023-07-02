package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCommandSuggestionsPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCommandSuggestionPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;

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
        transactions.get(packet.getTransactionId()).complete(packet);
    }
}
