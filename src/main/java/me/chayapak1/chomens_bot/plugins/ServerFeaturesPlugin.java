package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;

public class ServerFeaturesPlugin extends Bot.Listener {
    private final Bot bot;

    public boolean hasNamespaces = false;

    public boolean hasEssentials = false;
    public boolean hasExtras = false;

    public ServerFeaturesPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundCommandsPacket t_packet) packetReceived(t_packet);
    }

    public void packetReceived (ClientboundCommandsPacket packet) {
        for (CommandNode node : packet.getNodes()) {
            if (!node.isExecutable() || node.getType() != CommandType.LITERAL) continue;

            final String name = node.getName();

            if (name == null) continue;

            if (name.contains(":")) hasNamespaces = true;

            if (!name.contains(":")) continue;

            // 4 or 2?
            if (bot.selfCare.permissionLevel < 4) {
                // it'd be weird for servers that allow OP while not having OP
                if (name.equals("minecraft:op")) hasExtras = true;

                continue;
            }

            final String[] split = name.split(":");

            if (split.length < 2) continue;

            final String key = split[0].toLowerCase();

            switch (key) {
                case "extras" -> hasExtras = true;
                case "essentials" -> hasEssentials = true;
            }
        }
    }

    @Override
    public void disconnected (DisconnectedEvent event) {
        hasNamespaces = false;
        hasEssentials = false;
        hasExtras = false;
    }
}
