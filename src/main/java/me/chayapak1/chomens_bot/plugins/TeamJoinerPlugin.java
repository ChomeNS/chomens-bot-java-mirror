package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.Team;
import me.chayapak1.chomens_bot.util.UUIDUtilities;

// the name might sound confusing but it just adds the bot into its own team
public class TeamJoinerPlugin extends TickPlugin.Listener {
    public final String teamName;

    private final Bot bot;

    public TeamJoinerPlugin (Bot bot) {
        this.bot = bot;
        this.teamName = bot.config.teamName;

        bot.tick.addListener(this);
    }

    @Override
    public void onTick () {
        final Team team = bot.team.findTeamByName(teamName);

        if (team == null) {
            addTeam();
            return;
        }

        if (!team.players.contains(bot.username)) joinTeam();

        for (String player : team.players) {
            if (!player.equals(bot.username)) {
                excludeOthers();
                break;
            }
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
