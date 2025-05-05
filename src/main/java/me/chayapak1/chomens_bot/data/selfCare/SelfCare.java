package me.chayapak1.chomens_bot.data.selfCare;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.packet.Packet;

public abstract class SelfCare {
    public final Bot bot;

    public boolean needsRunning = false;

    public SelfCare (final Bot bot) {
        this.bot = bot;
    }

    public abstract boolean shouldRun ();

    public void onPacketReceived (final Packet packet) { }

    public void onMessageReceived (final Component component, final String string) { }

    public void onCommandSpyMessageReceived (final PlayerEntry sender, final String command) { }

    public void run () { }

    public void cleanup () { }
}
