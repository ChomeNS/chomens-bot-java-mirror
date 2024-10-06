package me.chayapak1.chomens_bot.plugins;

import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.BossBar;
import me.chayapak1.chomens_bot.data.BotBossBar;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// yes this has been rewritten to be not spammy
public class BossbarManagerPlugin extends Bot.Listener {
    private final Bot bot;

    public final Map<UUID, BossBar> serverBossBars = new HashMap<>();
    private final Map<UUID, BotBossBar> bossBars = new HashMap<>();

    public boolean enabled = true;
    public boolean actionBar = false;

    public final String bossBarPrefix;

    public BossbarManagerPlugin (Bot bot) {
        this.bot = bot;
        this.bossBarPrefix = bot.config.bossBarNamespace + ":";

        bot.addListener(this);

        bot.players.addListener(new PlayersPlugin.Listener() {
            @Override
            public void playerJoined(PlayerEntry target) {
                BossbarManagerPlugin.this.playerJoined();
            }
        });

        bot.executor.scheduleAtFixedRate(this::check, 0, 600, TimeUnit.MILLISECONDS);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundBossEventPacket) packetReceived((ClientboundBossEventPacket) packet);
    }

    public void packetReceived(ClientboundBossEventPacket packet) {
        if (!enabled || actionBar || !bot.options.useCore) return;

        try {
            switch (packet.getAction()) {
                case ADD -> {
                    final Map<UUID, BotBossBar> mapCopy = new HashMap<>(bossBars);

                    for (Map.Entry<UUID, BotBossBar> _bossBar : mapCopy.entrySet()) {
                        final BotBossBar bossBar = _bossBar.getValue();

                        if (ComponentUtilities.isEqual(bossBar.secret, packet.getTitle())) {
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

                    if (botBossBar != null && ComponentUtilities.isEqual(botBossBar.secret, packet.getTitle())) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void check () {
        for (Map.Entry<UUID, BotBossBar> _bossBar : bossBars.entrySet()) {
            final UUID uuid = _bossBar.getKey();
            final BotBossBar bossBar = _bossBar.getValue();

            final BossBar serverBossBar = serverBossBars.get(uuid);

            if (serverBossBar == null) {
                bossBar.gotSecret = false;

                addBossBar(bossBar.id, bossBar, true);
            } else if (!ComponentUtilities.isEqual(bossBar.title(), serverBossBar.title)) {
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
    public void connected(ConnectedEvent event) {
        for (Map.Entry<UUID, BotBossBar> _bossBar : bossBars.entrySet()) {
            final BotBossBar bossBar = _bossBar.getValue();

            addBossBar(bossBar.id, bossBar);
        }
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        serverBossBars.clear();
    }

    private void playerJoined () {
        if (!enabled || actionBar || !bot.options.useCore) return;

        for (Map.Entry<UUID, BotBossBar> _bossBar : bossBars.entrySet()) {
            final BotBossBar bossBar = _bossBar.getValue();

            bossBar.setPlayers(bossBar.players());
        }
    }

    public void add (String name, BotBossBar bossBar) {
        if (!enabled || !bot.options.useCore) return;

        bossBar.onlyName = name;

        bossBar.id = bossBarPrefix + name;

        bossBars.put(bossBar.uuid, bossBar);

        addBossBar(bossBar.id, bossBar, true);
    }

    private void addBossBar (String name, BotBossBar bossBar) {
        addBossBar(name, bossBar, false);
    }
    private void addBossBar (String name, BotBossBar bossBar, boolean secret) {
        if (actionBar) return;

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

    public void remove (String name) {
        if (!enabled || actionBar || !bot.options.useCore) return;

        final Map<UUID, BotBossBar> mapCopy = new HashMap<>(bossBars);

        for (Map.Entry<UUID, BotBossBar> bossBar : mapCopy.entrySet()) {
            if (bossBar.getValue().id.equals(bossBarPrefix + name)) bossBars.remove(bossBar.getValue().uuid);
        }

        bot.core.run("minecraft:bossbar remove " + bossBarPrefix + name);
    }

    public BotBossBar get (String name) {
        for (Map.Entry<UUID, BotBossBar> _bossBar : bossBars.entrySet()) {
            final BotBossBar bossBar = _bossBar.getValue();

            if (bossBar.id != null && bossBar.id.equals(bossBarPrefix + name)) return bossBars.get(bossBar.uuid);
        }

        return null;
    }

    public BotBossBar get (UUID uuid) {
        for (Map.Entry<UUID, BotBossBar> bossBar : bossBars.entrySet()) {
            if (bossBar.getValue().uuid == uuid) return bossBar.getValue();
        }

        return null;
    }
}
