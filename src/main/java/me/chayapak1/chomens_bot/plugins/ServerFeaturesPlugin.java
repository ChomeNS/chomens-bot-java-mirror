package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundCommandsPacket;

import java.util.List;

public class ServerFeaturesPlugin implements Listener {
    private final Bot bot;

    private final List<String> commands = new ObjectArrayList<>();

    public boolean hasNamespaces = false;

    public boolean hasEssentials = false;
    public boolean hasExtras = false;
    public boolean hasIControlU = false;
    public boolean hasCommandSpy = false;

    public ServerFeaturesPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        if (packet instanceof final ClientboundCommandsPacket t_packet) packetReceived(t_packet);
    }

    private void packetReceived (final ClientboundCommandsPacket packet) {
        commands.clear();

        for (final CommandNode node : packet.getNodes()) {
            if (!node.isExecutable() || node.getType() != CommandType.LITERAL) continue;

            final String name = node.getName();

            if (name == null) continue;

            commands.add(name);

            if (name.contains(":")) hasNamespaces = true;

            if (!name.contains(":")) continue;

            // 4 or 2?
            if (bot.selfCare.data.permissionLevel < 4) {
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
                case "icontrolu" -> hasIControlU = true;
                case "commandspy" -> hasCommandSpy = true;
            }
        }
    }

    public boolean serverHasCommand (final String command) {
        return commands.contains(command);
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        hasNamespaces = false;
        hasEssentials = false;
        hasExtras = false;
        hasIControlU = false;
        hasCommandSpy = false;
    }
}
