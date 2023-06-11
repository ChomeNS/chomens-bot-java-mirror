package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.Team;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class TeamPlugin extends Bot.Listener {
    @Getter private final List<Team> teams = new ArrayList<>();

    public TeamPlugin (Bot bot) {
        bot.addListener(this);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundSetPlayerTeamPacket) packetReceived((ClientboundSetPlayerTeamPacket) packet);
    }

    public void packetReceived(ClientboundSetPlayerTeamPacket packet) {
        switch (packet.getAction()) {
            case CREATE -> {
                final Team team = new Team(
                        packet.getTeamName(),
                        List.of(packet.getPlayers()),
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
            case REMOVE -> teams.removeIf(team -> team.teamName().equals(packet.getTeamName()));
            case UPDATE -> {
                final Team team = teams
                        .stream()
                        .filter(eachTeam -> eachTeam.teamName().equals(packet.getTeamName()))
                        .toArray(Team[]::new)[0];

                if (team == null) return;

                team.teamName(packet.getTeamName());
                team.displayName(packet.getDisplayName());
                team.friendlyFire(packet.isFriendlyFire());
                team.seeFriendlyInvisibles(packet.isSeeFriendlyInvisibles());
                team.nametagVisibility(packet.getNameTagVisibility());
                team.collisionRule(packet.getCollisionRule());
                team.color(packet.getColor());
                team.prefix(packet.getPrefix());
                team.suffix(packet.getSuffix());
            }
            case ADD_PLAYER -> {
                final Team team = teams
                        .stream()
                        .filter(eachTeam -> eachTeam.teamName().equals(packet.getTeamName()))
                        .toArray(Team[]::new)[0];

                if (team == null) return;

                team.players().addAll(List.of(packet.getPlayers()));
            }
            case REMOVE_PLAYER -> {
                final Team team = teams
                        .stream()
                        .filter(eachTeam -> eachTeam.teamName().equals(packet.getTeamName()))
                        .toArray(Team[]::new)[0];

                if (team == null) return;

                team.players().removeAll(List.of(packet.getPlayers()));
            }
        }
    }
}
