package me.chayapak1.chomens_bot.selfCares.vanilla;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import me.chayapak1.chomens_bot.data.selfCare.SelfData;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;

public class OperatorSelfCare extends SelfCare {
    public OperatorSelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public boolean shouldRun () {
        return bot.config.selfCare.op;
    }

    @Override
    public void onPacketReceived (final Packet packet) {
        if (packet instanceof final ClientboundEntityEventPacket t_packet) onPacketReceived(t_packet);
    }

    private void onPacketReceived (final ClientboundEntityEventPacket packet) {
        final EntityEvent event = packet.getEvent();
        final int id = packet.getEntityId();

        final SelfData data = bot.selfCare.data;

        if (id != data.entityId) return;

        final int permissionLevel = switch (event) {
            case PLAYER_OP_PERMISSION_LEVEL_0 -> 0;
            case PLAYER_OP_PERMISSION_LEVEL_1 -> 1;
            case PLAYER_OP_PERMISSION_LEVEL_2 -> 2;
            case PLAYER_OP_PERMISSION_LEVEL_3 -> 3;
            case PLAYER_OP_PERMISSION_LEVEL_4 -> 4;
            default -> -1;
        };

        if (permissionLevel != -1) data.permissionLevel = permissionLevel;

        this.needsRunning = data.permissionLevel < 2;
    }

    @Override
    public void run () {
        bot.chat.sendCommandInstantly("minecraft:op @s[type=player]");
    }
}
