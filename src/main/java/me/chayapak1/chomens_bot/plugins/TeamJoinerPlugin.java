package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.team.Team;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;

import java.util.ArrayList;

// the name might sound confusing but it just adds the bot into its own team
public class TeamJoinerPlugin extends TickPlugin.Listener {
    public final String teamName;

    private final Bot bot;

    public TeamJoinerPlugin (Bot bot) {
        this.bot = bot;
        this.teamName = bot.config.teamName;

        bot.addListener(new Bot.Listener() {
            @Override
            public void connected(ConnectedEvent event) {
                TeamJoinerPlugin.this.connected();
            }
        });

        bot.tick.addListener(this);
    }

    private void connected () {
        addTeam();
    }

    @Override
    public void onTick () {
        try {
            final Team team = bot.team.findTeamByName(teamName);

            if (team == null) {
                addTeam();
                return;
            }

            if (!team.players.contains(bot.username)) joinTeam();

            for (String player : new ArrayList<>(team.players)) {
                if (!player.equals(bot.username)) {
                    excludeOthers();
                    break;
                }
            }
        } catch (Exception e) {
            bot.logger.error(e);
        }
    }

    private void addTeam () {
        bot.core.run("minecraft:team add " + teamName);
        joinTeam();
    }

    private void joinTeam () {
        bot.core.run("minecraft:team join " + teamName + " " + UUIDUtilities.selector(bot.profile.getId()));
    }

    private void excludeOthers () {
        bot.core.run(
                String.format(
                        "minecraft:team leave %s,team=%s]",
                        UUIDUtilities.exclusiveSelector(bot.profile.getId(), false),
                        teamName
                )
        );
    }
}
