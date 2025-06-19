package me.chayapak1.chomens_bot.selfCares.ayunboom;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;

public class AyunBoomSelfCare extends SelfCare {
    public AyunBoomSelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public boolean shouldRun () {
        return bot.options.isAyunBoom;
    }

    @Override
    public void onPacketReceived (final Packet packet) {
        if (packet instanceof final ClientboundLoginPacket t_packet) onPacketReceived(t_packet);
    }

    private void onPacketReceived (final ClientboundLoginPacket ignoredPacket) {
        this.needsRunning = true;
    }

    @Override
    public void onMessageReceived (final Component component, final String string) {
        if (string.equals("Unable to connect you to ayunboom. Please try again later.")) this.needsRunning = true;
        else if (string.equals("You are already connected to this server!")) this.needsRunning = false;
    }

    @Override
    public void run () {
        bot.chat.sendCommandInstantly("server ayunboom");
    }

    @Override
    public void cleanup () {
        this.needsRunning = true;
    }
}
