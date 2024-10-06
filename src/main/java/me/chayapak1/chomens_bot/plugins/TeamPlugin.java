package me.chayapak1.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.Team;

import java.util.HashMap;
import java.util.Map;

public class TeamPlugin extends Bot.Listener {
    public final Map<String, Team> teams = new HashMap<>();
    public final Map<String, Team> teamsByPlayer = new HashMap<>();

    public TeamPlugin (Bot bot) {
        bot.addListener(this);
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
                            packet.getDisplayName(),
                            packet.isFriendlyFire(),
                            packet.isSeeFriendlyInvisibles(),
                            packet.getNameTagVisibility(),
                            packet.getCollisionRule(),
                            packet.getColor(),
                            packet.getPrefix(),
                            packet.getSuffix()
                    );

                    teams.put(packet.getTeamName(), team);
                }
                case REMOVE -> teams.remove(packet.getTeamName());
                case UPDATE -> {
                    final Team team = teams.get(packet.getTeamName());

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
                    final Team team = teams.get(packet.getTeamName());

                    if (team == null) return;

                    for (String player : packet.getPlayers()) teamsByPlayer.put(player, team);
                }
                case REMOVE_PLAYER -> {
                    final Team team = teams.get(packet.getTeamName());

                    if (team == null) return;

                    for (String player : packet.getPlayers()) teamsByPlayer.remove(player);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
