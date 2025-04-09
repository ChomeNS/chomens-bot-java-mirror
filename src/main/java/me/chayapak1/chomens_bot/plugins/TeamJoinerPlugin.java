package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.team.Team;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;

import java.util.concurrent.TimeUnit;

// the name might sound confusing but it just adds the bot into its own team
public class TeamJoinerPlugin {
    public final String teamName;

    private final Bot bot;

    public TeamJoinerPlugin (final Bot bot) {
        this.bot = bot;
        this.teamName = bot.config.teamName;

        bot.addListener(new Bot.Listener() {
            @Override
            public void connected (final ConnectedEvent event) {
                TeamJoinerPlugin.this.connected();
            }
        });

        bot.executor.scheduleAtFixedRate(this::check, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void connected () {
        addTeam();
    }

    public void check () {
        try {
            if (!bot.loggedIn) return;

            final Team team = bot.team.findTeamByName(teamName);

            if (team == null) {
                addTeam();
                return;
            }

            if (!team.players.contains(bot.username)) {
                joinTeam();
                return;
            }

            // checks if ONLY the bot is in the team, and not anyone else
            if (team.players.size() == 1 && team.players.getFirst().equals(bot.username)) return;

            excludeOthers();
        } catch (final Exception e) {
            bot.logger.error(e);
        }
    }

    private void addTeam () {
        bot.core.run("minecraft:team add " + teamName);
        joinTeam();
    }

    private void joinTeam () {
        bot.core.run("minecraft:team join " + teamName + " " + bot.profile.getIdAsString());
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
