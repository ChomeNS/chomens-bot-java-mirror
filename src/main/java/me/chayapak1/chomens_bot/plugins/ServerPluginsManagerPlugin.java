package me.chayapak1.chomens_bot.plugins;

import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandSuggestionsPacket;
import me.chayapak1.chomens_bot.Bot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ServerPluginsManagerPlugin extends Bot.Listener {
    public static final String EXTRAS = "Extras";
    public static final String ESSENTIALS = "Essentials";

    private final Bot bot;

    public List<String> plugins = new ArrayList<>();

    public ServerPluginsManagerPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);
    }

    @Override
    public void connected(ConnectedEvent event) {
        final CompletableFuture<ClientboundCommandSuggestionsPacket> future = bot.tabComplete.tabComplete("/ver ");

        future.thenApplyAsync((packet) -> {
            final String[] matches = packet.getMatches();

            // should i just use the plugins as the String array instead of a list?
            plugins = new ArrayList<>(Arrays.asList(matches));

            return packet;
        });
    }

    public boolean hasPlugin (String plugin) { return plugins.contains(plugin); }
}
