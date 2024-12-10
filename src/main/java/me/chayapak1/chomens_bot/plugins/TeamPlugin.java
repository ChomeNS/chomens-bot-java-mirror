package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.Team;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;

import java.util.*;

public class TeamPlugin extends Bot.Listener {
    public final List<Team> teams = Collections.synchronizedList(new ArrayList<>());

    public TeamPlugin (Bot bot) {
        bot.addListener(this);
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        teams.clear();
    }

    public synchronized Team findTeamByName (String name) {
        for (Team team : new ArrayList<>(teams)) {
            if (team.teamName.equals(name)) return team;
        }

        return null;
    }

    public synchronized Team findTeamByMember (String member) {
        for (Team team : new ArrayList<>(teams)) {
            if (team.players.contains(member)) return team;
        }

        return null;
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundSetPlayerTeamPacket) packetReceived((ClientboundSetPlayerTeamPacket) packet);
    }

    public void packetReceived(ClientboundSetPlayerTeamPacket packet) {
        try {
            switch (packet.getAction()) {
                case CREATE -> {
                    final Team team = new Team(
                            packet.getTeamName(),
                            new ArrayList<>(),
                            packet.getDisplayName(),
                            packet.isFriendlyFire(),
                            packet.isSeeFriendlyInvisibles(),
                            packet.getNameTagVisibility(),
                            packet.getCollisionRule(),
                            packet.getColor(),
                            packet.getPrefix(),
                            packet.getSuffix()
                    );

                    teams.add(team);
                }
                case REMOVE -> {
                    final Team team = findTeamByName(packet.getTeamName());

                    if (team == null) return;

                    teams.remove(team);
                }
                case UPDATE -> {
                    final Team team = findTeamByName(packet.getTeamName());

                    if (team == null) return;

                    team.teamName = packet.getTeamName();
                    team.displayName = packet.getDisplayName();
                    team.friendlyFire = packet.isFriendlyFire();
                    team.seeFriendlyInvisibles = packet.isSeeFriendlyInvisibles();
                    team.nametagVisibility = packet.getNameTagVisibility();
                    team.collisionRule = packet.getCollisionRule();
                    team.color = packet.getColor();
                    team.prefix = packet.getPrefix();
                    team.suffix = packet.getSuffix();
                }
                case ADD_PLAYER -> {
                    final Team team = findTeamByName(packet.getTeamName());

                    if (team == null) return;

                    team.players.addAll(Arrays.asList(packet.getPlayers()));
                }
                case REMOVE_PLAYER -> {
                    final Team team = findTeamByName(packet.getTeamName());

                    if (team == null) return;

                    team.players.removeAll(Arrays.asList(packet.getPlayers()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
