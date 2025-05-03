package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEventValue;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SelfCarePlugin implements Listener {
    private final Bot bot;

    public boolean visible = false;

    private int entityId;
    public GameMode gamemode;
    public int permissionLevel;

    private long usernameStartTime = System.currentTimeMillis();

    private boolean cspy = false;
    private boolean vanish = false;
    private boolean nickname = false;
    private boolean socialspy = false;
    private boolean muted = false;
    private boolean prefix = false;
    private boolean username = true;

    public SelfCarePlugin (final Bot bot) {
        this.bot = bot;

        bot.executor.scheduleAtFixedRate(this::check, 0, bot.config.selfCare.delay, TimeUnit.MILLISECONDS);

        bot.listener.addListener(this);
    }

    @Override
    public boolean onSystemMessageReceived (final Component component, final String string, final String ansi) {
        final Configuration.BotOption.EssentialsMessages essentialsMessages = bot.options.essentialsMessages;

        if (string.equals("Successfully enabled CommandSpy")) cspy = true;
        else if (string.equals("Successfully disabled CommandSpy")) cspy = false;

        else if (string.equals(String.format(essentialsMessages.vanishEnable1, bot.username))) vanish = true;
        else if (string.equals(essentialsMessages.vanishEnable2)) vanish = true;
        else if (string.equals(String.format(essentialsMessages.vanishDisable, bot.username))) vanish = false;

        else if (string.equals(essentialsMessages.nickNameRemove)) nickname = true;
        else if (string.startsWith(essentialsMessages.nickNameSet)) nickname = false;

        else if (string.equals(String.format(essentialsMessages.socialSpyEnable, bot.username))) socialspy = true;
        else if (string.equals(String.format(essentialsMessages.socialSpyDisable, bot.username))) socialspy = false;

        else if (string.startsWith(essentialsMessages.muted)) muted = true;
        else if (string.equals(essentialsMessages.unmuted)) muted = false;

        else if (string.equals("You now have the tag: " + bot.config.selfCare.prefix.prefix)) prefix = true;
        else if (string.startsWith("You no longer have a tag")) prefix = false;
        else if (string.startsWith("You now have the tag: ")) prefix = false;
            // prefix = true as a workaround to prevent spamming
        else if (string.equals("Something went wrong while saving the prefix. Please check console."))
            prefix = true; // Parker2991

        else if (string.equals("Successfully set your username to \"" + bot.username + "\"")) username = true;
        else if (string.startsWith("Successfully set your username to \"")) {
            username = false;
            usernameStartTime = System.currentTimeMillis();
        } else if (string.startsWith("You already have the username \"" + bot.username + "\"")) username = true;
        else if (string.startsWith("You already have the username \"")) username = false;

        return true;
    }

    public void check () {
        if (!bot.loggedIn) return;

        final Configuration.SelfCare selfCares = bot.config.selfCare;

        final boolean kaboom = bot.serverFeatures.hasExtras;
        final boolean hasEssentials = bot.serverFeatures.hasEssentials;

        final String uuid = bot.profile.getIdAsString();

        if (selfCares.op && permissionLevel < 2) {
            bot.chat.send("/minecraft:op @s[type=player]");
        } else if (selfCares.gamemode && gamemode != GameMode.CREATIVE) {
            bot.chat.send("/minecraft:gamemode creative @s[type=player]");
        } else if (selfCares.cspy && !cspy && kaboom) {
            if (bot.options.useChat || !bot.options.coreCommandSpy) {
                bot.chat.sendCommandInstantly("commandspy:commandspy on");
            } else {
                bot.core.run("commandspy:commandspy " + uuid + " on");
            }
        } else if (selfCares.prefix.enabled && !prefix && kaboom) {
            bot.chat.send("/extras:prefix " + bot.config.selfCare.prefix.prefix);
        } else if (
                selfCares.username
                        && (System.currentTimeMillis() - usernameStartTime) >= 2 * 1000
                        && !username
                        && kaboom
        ) {
            bot.chat.send("/extras:username " + bot.username);
        } else if (hasEssentials) {
            final String usernameOrBlank = !bot.options.useChat ?
                    bot.username + " " :
                    "";

            if (selfCares.vanish && visible == vanish) {
                runEssentialsCommand("essentials:vanish " + usernameOrBlank + (visible ? "disable" : "enable"));
            } else if (selfCares.nickname && !nickname) {
                runEssentialsCommand("essentials:nickname " + uuid + " off");
            } else if (selfCares.socialspy && !socialspy) {
                runEssentialsCommand("essentials:socialspy " + usernameOrBlank + "enable");
            } else if (selfCares.mute && muted) {
                runEssentialsCommand("essentials:mute " + uuid);
                muted = false;
            }
        }
    }

    @Override
    public void onCommandSpyMessageReceived (final PlayerEntry sender, final String command) {
        if (!bot.config.selfCare.icu || !bot.serverFeatures.hasIControlU) return;

        final String[] args = command.split("\\s+");

        if (args.length < 3) return;

        if (
                (!args[0].equals("/icontrolu:icu") && !args[0].equals("/icu"))
                        || !args[1].equalsIgnoreCase("control")
        ) return;

        // interestingly, icu only uses the third argument for the player, and not a greedy string
        final String player = args[2];

        PlayerEntry target = bot.players.getEntryTheBukkitWay(player);

        if (target == null && args[2].matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})")) {
            target = bot.players.getEntry(UUID.fromString(args[2]));
        }

        if (target == null || !target.profile.getId().equals(bot.profile.getId())) return;

        bot.core.run("essentials:sudo " + sender.profile.getIdAsString() + " icu stop");
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        switch (packet) {
            case final ClientboundLoginPacket t_packet -> packetReceived(t_packet);
            case final ClientboundGameEventPacket t_packet -> packetReceived(t_packet);
            case final ClientboundEntityEventPacket t_packet -> packetReceived(t_packet);
            case final ClientboundOpenScreenPacket t_packet -> packetReceived(t_packet);
            case final ClientboundSetPassengersPacket t_packet -> packetReceived(t_packet);
            default -> { }
        }
    }

    private void packetReceived (final ClientboundLoginPacket packet) {
        this.entityId = packet.getEntityId();
        this.gamemode = packet.getCommonPlayerSpawnInfo().getGameMode();

        cspy = false;
        vanish = false;
        nickname = false;
        socialspy = false;
        muted = false;
        prefix = false;
    }

    private void packetReceived (final ClientboundGameEventPacket packet) {
        final GameEvent notification = packet.getNotification();
        final GameEventValue value = packet.getValue();

        if (notification == GameEvent.WIN_GAME && bot.config.selfCare.endCredits) {
            bot.session.send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
            return;
        }

        if (notification == GameEvent.CHANGE_GAME_MODE) gamemode = (GameMode) value;
    }

    private void packetReceived (final ClientboundEntityEventPacket packet) {
        final EntityEvent event = packet.getEvent();
        final int id = packet.getEntityId();

        if (id != entityId) return;

        if (event == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_0) permissionLevel = 0;
        else if (event == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_1) permissionLevel = 1;
        else if (event == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_2) permissionLevel = 2;
        else if (event == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_3) permissionLevel = 3;
        else if (event == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_4) permissionLevel = 4;
    }

    private void packetReceived (final ClientboundOpenScreenPacket packet) {
        // instantly closes the window when received the packet
        // also should this be in self care?
        bot.session.send(
                new ServerboundContainerClosePacket(
                        packet.getContainerId()
                )
        );
    }

    private void packetReceived (final ClientboundSetPassengersPacket packet) {
        if (
                Arrays.stream(packet.getPassengerIds())
                        .noneMatch(id -> id == entityId)
        ) return;

        // automatically unmount when mounting
        // e.g. player `/ride`s you
        // the server will automatically dismount for you,
        // so that's why we don't have to send PlayerState.STOP_SNEAKING
        bot.session.send(
                new ServerboundPlayerCommandPacket(
                        entityId,
                        PlayerState.START_SNEAKING
                )
        );
    }

    private void runEssentialsCommand (final String command) {
        if (bot.options.useChat) bot.chat.sendCommandInstantly(command);
        else bot.core.run(command);
    }
}
