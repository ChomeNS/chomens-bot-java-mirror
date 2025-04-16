package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.bossbar.BossBar;
import me.chayapak1.chomens_bot.data.bossbar.BotBossBar;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// yes this has been rewritten to be not spammy
public class BossbarManagerPlugin implements Listener {
    private final Bot bot;

    public final Map<UUID, BossBar> serverBossBars = new HashMap<>();
    private final Map<UUID, BotBossBar> bossBars = new HashMap<>();

    public boolean enabled = true;
    public boolean actionBar = false;

    public final String bossBarPrefix;

    public BossbarManagerPlugin (final Bot bot) {
        this.bot = bot;
        this.bossBarPrefix = bot.config.namespace + ":";

        bot.listener.addListener(this);
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        if (packet instanceof final ClientboundBossEventPacket t_packet) packetReceived(t_packet);
    }

    private void packetReceived (final ClientboundBossEventPacket packet) {
        if (!enabled || actionBar || !bot.options.useCore) return;

        try {
            switch (packet.getAction()) {
                case ADD -> {
                    final Map<UUID, BotBossBar> mapCopy = new HashMap<>(bossBars);

                    for (final Map.Entry<UUID, BotBossBar> _bossBar : mapCopy.entrySet()) {
                        final BotBossBar bossBar = _bossBar.getValue();

                        if (bossBar.secret.equals(packet.getTitle())) {
                            bossBars.remove(_bossBar.getKey());

                            final BotBossBar newBossBar = new BotBossBar(
                                    bossBar.title(),
                                    bossBar.players(),
                                    bossBar.color,
                                    bossBar.division,
                                    bossBar.visible(),
                                    bossBar.max(),
                                    bossBar.value(),
                                    bot
                            );

                            newBossBar.gotSecret = true;

                            bossBars.put(
                                    packet.getUuid(),
                                    newBossBar
                            );

                            bossBars.get(packet.getUuid()).id = bossBar.id;
                            bossBars.get(packet.getUuid()).onlyName = bossBar.onlyName;
                            bossBars.get(packet.getUuid()).uuid = packet.getUuid();

                            newBossBar.setTitle(bossBar.title);
                        }
                    }

                    serverBossBars.put(
                            packet.getUuid(),
                            new BossBar(
                                    packet.getUuid(),
                                    packet.getTitle(),
                                    packet.getColor(),
                                    packet.getDivision(),
                                    packet.getHealth()
                            )
                    );
                }
                case REMOVE -> serverBossBars.remove(packet.getUuid()); // self care is at the check function
                case UPDATE_STYLE -> {
                    final BossBar bossBar = serverBossBars.get(packet.getUuid());

                    bossBar.color = packet.getColor();
                    bossBar.division = packet.getDivision();
                }
                case UPDATE_TITLE -> {
                    final BossBar bossBar = serverBossBars.get(packet.getUuid());

                    final BotBossBar botBossBar = get(bossBar.uuid);

                    if (botBossBar != null && botBossBar.secret.equals(packet.getTitle())) {
                        botBossBar.uuid = packet.getUuid();

                        botBossBar.gotSecret = true;
                    }

                    bossBar.title = packet.getTitle();
                }
                case UPDATE_HEALTH -> {
                    final BossBar bossBar = serverBossBars.get(packet.getUuid());

                    bossBar.health = packet.getHealth();
                }
            }
        } catch (final Exception e) {
            bot.logger.error(e);
        }
    }

    @Override
    public void onSecondTick () {
        for (final Map.Entry<UUID, BotBossBar> _bossBar : bossBars.entrySet()) {
            final UUID uuid = _bossBar.getKey();
            final BotBossBar bossBar = _bossBar.getValue();

            final BossBar serverBossBar = serverBossBars.get(uuid);

            if (serverBossBar == null) {
                bossBar.gotSecret = false;

                addBossBar(bossBar.id, bossBar, true);
            } else if (!serverBossBar.title.equals(bossBar.title)) {
                bossBar.setTitle(bossBar.title, true);
            } else if (bossBar.value() != serverBossBar.health * bossBar.max()) {
                bossBar.setValue(bossBar.value(), true);
                bossBar.setMax(bossBar.max(), true);
            } else if (bossBar.color != serverBossBar.color) {
                bossBar.setColor(bossBar.color, true);
            } else if (bossBar.division != serverBossBar.division) {
                bossBar.setDivision(bossBar.division, true);
            }
        }
    }

    @Override
    public void onCoreReady () {
        for (final Map.Entry<UUID, BotBossBar> _bossBar : bossBars.entrySet()) {
            final BotBossBar bossBar = _bossBar.getValue();

            addBossBar(bossBar.id, bossBar, true);
        }
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        serverBossBars.clear();
    }

    @Override
    public void onPlayerJoined (final PlayerEntry target) {
        if (!enabled || actionBar || !bot.options.useCore) return;

        for (final Map.Entry<UUID, BotBossBar> _bossBar : bossBars.entrySet()) {
            final BotBossBar bossBar = _bossBar.getValue();

            bossBar.setPlayers(bossBar.players(), true);
        }
    }

    public void add (final String name, final BotBossBar bossBar) {
        if (!enabled || !bot.options.useCore) return;

        bossBar.onlyName = name;

        bossBar.id = bossBarPrefix + name;

        bossBars.put(bossBar.uuid, bossBar);

        addBossBar(bossBar.id, bossBar, true);
    }

    private void addBossBar (final String name, final BotBossBar bossBar) {
        addBossBar(name, bossBar, false);
    }

    private void addBossBar (final String name, final BotBossBar bossBar, final boolean secret) {
        if (!enabled || actionBar) return;

        final String prefix = "minecraft:bossbar set " + name + " ";

        final String stringifiedName = GsonComponentSerializer.gson().serialize(secret ? bossBar.secret : bossBar.title);

        String division = null;

        switch (bossBar.division) {
            case NONE -> division = "progress";
            case NOTCHES_20 -> division = "notched_20";
            case NOTCHES_6 -> division = "notched_6";
            case NOTCHES_12 -> division = "notched_12";
            case NOTCHES_10 -> division = "notched_10";
        }

        bot.core.run("minecraft:bossbar add " + name + " " + stringifiedName);
        bot.core.run(prefix + "players " + bossBar.players());
        bot.core.run(prefix + "color " + (bossBar.color == BossBarColor.LIME ? "green" : (bossBar.color == BossBarColor.CYAN ? "blue" : bossBar.color.name().toLowerCase())));
        bot.core.run(prefix + "visible " + bossBar.visible());
        bot.core.run(prefix + "style " + division);
        bot.core.run(prefix + "max " + bossBar.max());
        bot.core.run(prefix + "value " + bossBar.value());
    }

    public void remove (final String name) {
        if (!enabled || actionBar || !bot.options.useCore) return;

        final Map<UUID, BotBossBar> mapCopy = new HashMap<>(bossBars);

        for (final Map.Entry<UUID, BotBossBar> bossBar : mapCopy.entrySet()) {
            if (bossBar.getValue().id.equals(bossBarPrefix + name)) bossBars.remove(bossBar.getValue().uuid);
        }

        bot.core.run("minecraft:bossbar remove " + bossBarPrefix + name);
    }

    public BotBossBar get (final String name) {
        if (!enabled) return null;

        for (final Map.Entry<UUID, BotBossBar> _bossBar : bossBars.entrySet()) {
            final BotBossBar bossBar = _bossBar.getValue();

            if (bossBar.id != null && bossBar.id.equals(bossBarPrefix + name)) return bossBars.get(bossBar.uuid);
        }

        return null;
    }

    public BotBossBar get (final UUID uuid) {
        if (!enabled) return null;

        for (final Map.Entry<UUID, BotBossBar> bossBar : bossBars.entrySet()) {
            if (bossBar.getValue().uuid == uuid) return bossBar.getValue();
        }

        return null;
    }
}
