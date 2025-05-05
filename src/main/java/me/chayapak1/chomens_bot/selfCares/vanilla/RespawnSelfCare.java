package me.chayapak1.chomens_bot.selfCares.vanilla;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;

public class RespawnSelfCare extends SelfCare {
    public RespawnSelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public boolean shouldRun () {
        return bot.config.selfCare.respawn;
    }

    @Override
    public void onPacketReceived (final Packet packet) {
        if (packet instanceof final ClientboundGameEventPacket t_packet) onPacketReceived(t_packet);
        else if (packet instanceof final ClientboundPlayerCombatKillPacket t_packet) onPacketReceived(t_packet);
    }

    private void onPacketReceived (final ClientboundGameEventPacket packet) {
        if (packet.getNotification() == GameEvent.WIN_GAME) this.needsRunning = true;
    }

    // You died!
    private void onPacketReceived (final ClientboundPlayerCombatKillPacket packet) {
        if (packet.getPlayerId() == bot.selfCare.data.entityId) this.needsRunning = true;
    }

    @Override
    public void run () {
        bot.session.send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
    }
}
