package me.chayapak1.chomens_bot.selfCares.vanilla;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import me.chayapak1.chomens_bot.data.selfCare.SelfData;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEventValue;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;

public class GameModeSelfCare extends SelfCare {
    public GameModeSelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public boolean shouldRun () {
        return bot.config.selfCare.gamemode;
    }

    @Override
    public void onPacketReceived (final Packet packet) {
        if (packet instanceof final ClientboundLoginPacket t_packet) onPacketReceived(t_packet);
        else if (packet instanceof final ClientboundGameEventPacket t_packet) onPacketReceived(t_packet);
    }

    private void onPacketReceived (final ClientboundLoginPacket packet) {
        // data is already initialized, since self-care events get dispatched after the base handling in the plugin
        final SelfData data = bot.selfCare.data;

        data.gameMode = packet.getCommonPlayerSpawnInfo().getGameMode();

        this.needsRunning = data.gameMode != GameMode.CREATIVE;
    }

    private void onPacketReceived (final ClientboundGameEventPacket packet) {
        final GameEvent notification = packet.getNotification();
        final GameEventValue value = packet.getValue();

        final SelfData data = bot.selfCare.data;

        if (notification == GameEvent.CHANGE_GAME_MODE) data.gameMode = (GameMode) value;

        this.needsRunning = data.gameMode != GameMode.CREATIVE;
    }

    @Override
    public void run () {
        bot.chat.sendCommandInstantly("minecraft:gamemode creative @s[type=player]");
    }
}
