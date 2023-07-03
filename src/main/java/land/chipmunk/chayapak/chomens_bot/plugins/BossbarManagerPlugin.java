package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.data.game.BossBarColor;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.BossBar;
import land.chipmunk.chayapak.chomens_bot.data.BotBossBar;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// yes this has been rewritten to be not spammy
public class BossbarManagerPlugin extends Bot.Listener {
    private final Bot bot;

    public final Map<UUID, BossBar> serverBossBars = new HashMap<>();
    private final Map<UUID, BotBossBar> bossBars = new HashMap<>();

    public final boolean enabled = true;

    public final String bossBarPrefix = "chomens_bot:";

    public BossbarManagerPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);

        bot.players.addListener(new PlayersPlugin.Listener() {
            @Override
            public void playerJoined(MutablePlayerListEntry target) {
                BossbarManagerPlugin.this.playerJoined();
            }
        });
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundBossEventPacket) packetReceived((ClientboundBossEventPacket) packet);
    }

    public void packetReceived(ClientboundBossEventPacket packet) {
        if (!enabled || !bot.options.useCore) return;

        try {
            switch (packet.getAction()) {
                case ADD -> {
                    final Map<UUID, BotBossBar> mapCopy = new HashMap<>(bossBars);

                    for (Map.Entry<UUID, BotBossBar> _bossBar : mapCopy.entrySet()) {
                        final BotBossBar bossBar = _bossBar.getValue();

                        if (ComponentUtilities.isEqual(bossBar.secret, packet.getTitle())) {
                            bossBars.remove(_bossBar.getKey());

                            bossBars.put(
                                    packet.getUuid(),
                                    new BotBossBar(
                                            bossBar.title,
                                            bossBar.players,
                                            bossBar.color,
                                            bossBar.division,
                                            bossBar.visible,
                                            bossBar.max,
                                            bossBar.value,
                                            bot
                                    )
                            );

                            bossBars.get(packet.getUuid()).id = bossBar.id;
                            bossBars.get(packet.getUuid()).uuid = packet.getUuid();
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
                case REMOVE -> {
                    for (Map.Entry<UUID, BotBossBar> _bossBar : bossBars.entrySet()) {
                        final BotBossBar bossBar = _bossBar.getValue();

                        if (bossBar.uuid.equals(packet.getUuid())) {
                            addBossBar(bossBar.id, bossBar);
                            break;
                        }
                    }

                    serverBossBars.remove(packet.getUuid());
                }
                case UPDATE_STYLE -> {
                    final BossBar bossBar = serverBossBars.get(packet.getUuid());

                    final BotBossBar botBossBar = get(bossBar.uuid);

                    if (botBossBar != null && botBossBar.color != packet.getColor()) {
                        botBossBar.setColor(botBossBar.color, true);
                    } else if (botBossBar != null && botBossBar.division != packet.getDivision()) {
                        botBossBar.setDivision(botBossBar.division, true);
                    }

                    bossBar.color = packet.getColor();
                    bossBar.division = packet.getDivision();
                }
                case UPDATE_TITLE -> {
                    final BossBar bossBar = serverBossBars.get(packet.getUuid());

                    final BotBossBar botBossBar = get(bossBar.uuid);

                    if (botBossBar != null && !ComponentUtilities.isEqual(botBossBar.title, packet.getTitle())) {
                        botBossBar.setTitle(botBossBar.title, true);
                    }

                    bossBar.title = packet.getTitle();
                }
                case UPDATE_HEALTH -> {
                    final BossBar bossBar = serverBossBars.get(packet.getUuid());

                    final BotBossBar botBossBar = get(bossBar.uuid);

                    if (
                            botBossBar != null &&
                                    botBossBar.value != packet.getHealth() * botBossBar.max
                    ) {
                        botBossBar.setValue(botBossBar.value, true);
                        botBossBar.setMax(botBossBar.max, true);
                    }

                    bossBar.health = packet.getHealth();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playerJoined () {
        for (Map.Entry<UUID, BotBossBar> _bossBar : bossBars.entrySet()) {
            final BotBossBar bossBar = _bossBar.getValue();

            bossBar.setPlayers(bossBar.players);
        }
    }

    public void add (String name, BotBossBar bossBar) {
        if (!enabled || !bot.options.useCore) return;

        bossBar.id = bossBarPrefix + name;

        bossBars.put(bossBar.uuid, bossBar);

        addBossBar(bossBar.id, bossBar, true);
    }

    private void addBossBar (String name, BotBossBar bossBar) {
        addBossBar(name, bossBar, false);
    }
    private void addBossBar (String name, BotBossBar bossBar, boolean secret) {
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
        bot.core.run(prefix + "players " + bossBar.players);
        bot.core.run(prefix + "color " + (bossBar.color == BossBarColor.LIME ? "green" : bossBar.color.name().toLowerCase()));
        bot.core.run(prefix + "visible " + bossBar.visible);
        bot.core.run(prefix + "style " + division);
        bot.core.run(prefix + "max " + bossBar.max);
        bot.core.run(prefix + "value " + bossBar.value);
    }

    public void remove (String name) {
        if (!enabled || !bot.options.useCore) return;

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
