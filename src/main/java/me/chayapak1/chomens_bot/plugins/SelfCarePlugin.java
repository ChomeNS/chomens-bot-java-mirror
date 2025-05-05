package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import me.chayapak1.chomens_bot.data.selfCare.SelfData;
import me.chayapak1.chomens_bot.selfCares.essentials.MuteSelfCare;
import me.chayapak1.chomens_bot.selfCares.essentials.NicknameSelfCare;
import me.chayapak1.chomens_bot.selfCares.essentials.SocialSpySelfCare;
import me.chayapak1.chomens_bot.selfCares.essentials.VanishSelfCare;
import me.chayapak1.chomens_bot.selfCares.kaboom.commandSpy.CommandSpySelfCare;
import me.chayapak1.chomens_bot.selfCares.kaboom.extras.PrefixSelfCare;
import me.chayapak1.chomens_bot.selfCares.kaboom.extras.UsernameSelfCare;
import me.chayapak1.chomens_bot.selfCares.kaboom.icu.IControlUSelfCare;
import me.chayapak1.chomens_bot.selfCares.vanilla.GameModeSelfCare;
import me.chayapak1.chomens_bot.selfCares.vanilla.OperatorSelfCare;
import me.chayapak1.chomens_bot.selfCares.vanilla.RespawnSelfCare;
import me.chayapak1.chomens_bot.selfCares.vanilla.misc.OpenScreenSelfCare;
import me.chayapak1.chomens_bot.selfCares.vanilla.misc.RideSelfCare;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;

import java.util.ArrayList;
import java.util.List;

public class SelfCarePlugin implements Listener {
    private final Bot bot;

    public SelfData data;

    private final List<SelfCare> selfCares = new ArrayList<>();

    public SelfCarePlugin (final Bot bot) {
        this.bot = bot;

        // vanilla
        selfCares.add(new OperatorSelfCare(bot));
        selfCares.add(new GameModeSelfCare(bot));
        selfCares.add(new RespawnSelfCare(bot));

        // vanilla - misc
        selfCares.add(new OpenScreenSelfCare(bot));
        selfCares.add(new RideSelfCare(bot));

        // kaboom - CommandSpy
        selfCares.add(new CommandSpySelfCare(bot));

        // kaboom - extras
        selfCares.add(new PrefixSelfCare(bot));
        selfCares.add(new UsernameSelfCare(bot));

        // kaboom - iControlUwU
        selfCares.add(new IControlUSelfCare(bot));

        // essentials
        selfCares.add(new VanishSelfCare(bot));
        selfCares.add(new NicknameSelfCare(bot));
        selfCares.add(new SocialSpySelfCare(bot));
        selfCares.add(new MuteSelfCare(bot));

        bot.listener.addListener(this);
    }

    @Override
    public boolean onSystemMessageReceived (final Component component, final String string, final String ansi) {
        for (final SelfCare selfCare : selfCares) selfCare.onMessageReceived(component, string);

        return true;
    }

    @Override
    public void onSecondTick () {
        if (!bot.loggedIn) return;

        for (final SelfCare selfCare : selfCares) {
            if (!selfCare.needsRunning || !selfCare.shouldRun()) continue;

            selfCare.run();

            break; // we do not want to run all of them in parallel
        }
    }

    @Override
    public void onCommandSpyMessageReceived (final PlayerEntry sender, final String command) {
        for (final SelfCare selfCare : selfCares) selfCare.onCommandSpyMessageReceived(sender, command);
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        if (packet instanceof final ClientboundLoginPacket t_packet) packetReceived(t_packet);

        for (final SelfCare selfCare : selfCares) selfCare.onPacketReceived(packet);
    }

    private void packetReceived (final ClientboundLoginPacket packet) {
        this.data = new SelfData(packet.getEntityId());
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        this.data = null;

        for (final SelfCare selfCare : selfCares) selfCare.cleanup();
    }

    public <T extends SelfCare> T find (final Class<T> clazz) {
        for (final SelfCare selfCare : selfCares) {
            if (clazz.isInstance(selfCare)) return clazz.cast(selfCare);
        }

        return null;
    }
}
