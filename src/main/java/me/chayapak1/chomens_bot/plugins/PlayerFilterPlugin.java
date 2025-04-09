package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.filter.FilteredPlayer;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.LoggerUtilities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PlayerFilterPlugin implements PlayersPlugin.Listener {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS filters (name VARCHAR(255) PRIMARY KEY, reason VARCHAR(255), regex BOOLEAN, ignoreCase BOOLEAN);";
    private static final String LIST_FILTERS = "SELECT * FROM filters;";
    private static final String INSERT_FILTER = "INSERT INTO filters (name, reason, regex, ignoreCase) VALUES (?, ?, ?, ?);";
    private static final String REMOVE_FILTER = "DELETE FROM filters WHERE name = ?;";
    private static final String CLEAR_FILTER = "DELETE FROM filters;";

    public static List<FilteredPlayer> localList = new ArrayList<>();

    static {
        if (Main.database != null) {
            DatabasePlugin.EXECUTOR_SERVICE.submit(() -> {
                try {
                    Main.database.execute(CREATE_TABLE);
                } catch (final SQLException e) {
                    LoggerUtilities.error(e);
                }
            });

            Main.EXECUTOR.scheduleAtFixedRate(PlayerFilterPlugin::list, 5, 30, TimeUnit.SECONDS);
        }
    }

    private final Bot bot;

    public PlayerFilterPlugin (final Bot bot) {
        this.bot = bot;

        if (Main.database == null) return;

        bot.players.addListener(this);
    }

    public static List<FilteredPlayer> list () {
        final List<FilteredPlayer> output = new ArrayList<>();

        try (final ResultSet result = Main.database.query(LIST_FILTERS)) {
            if (result == null) return output;

            while (result.next()) {
                final FilteredPlayer filteredPlayer = new FilteredPlayer(
                        result.getString("name"),
                        result.getString("reason"),
                        result.getBoolean("regex"),
                        result.getBoolean("ignoreCase")
                );

                output.add(filteredPlayer);
            }
        } catch (final SQLException e) {
            LoggerUtilities.error(e);
        }

        localList = output;

        return output;
    }

    private FilteredPlayer getPlayer (final String name) {
        for (final FilteredPlayer filteredPlayer : localList) {
            if (matchesPlayer(name, filteredPlayer)) {
                return filteredPlayer;
            }
        }

        return null;
    }

    private List<FilteredPlayer> getPlayers (final String name) {
        final List<FilteredPlayer> matches = new ArrayList<>();

        for (final FilteredPlayer filteredPlayer : localList) {
            if (matchesPlayer(name, filteredPlayer)) {
                matches.add(filteredPlayer);
            }
        }

        return matches;
    }

    private boolean matchesPlayer (final String name, final FilteredPlayer player) {
        if (player.regex) {
            final Pattern pattern = compilePattern(player);

            return pattern != null && pattern.matcher(name).find();
        } else {
            return compareNames(name, player);
        }
    }

    private Pattern compilePattern (final FilteredPlayer player) {
        try {
            final int flags = player.ignoreCase ? Pattern.CASE_INSENSITIVE : 0;

            return Pattern.compile(player.playerName, flags);
        } catch (final Exception e) {
            bot.logger.error("Error compiling player filter regex " + player.playerName + " (this shouldn't happen):");
            bot.logger.error(e);
            return null;
        }
    }

    private boolean compareNames (final String name, final FilteredPlayer player) {
        final String playerName = player.ignoreCase ? player.playerName.toLowerCase() : player.playerName;
        final String targetName = player.ignoreCase ? name.toLowerCase() : name;

        return playerName.equals(targetName);
    }

    @Override
    public void playerJoined (final PlayerEntry target) {
        bot.executorService.submit(() -> {
            final FilteredPlayer player = getPlayer(target.profile.getName());

            if (player == null) return;

            bot.filterManager.add(target, player.reason);
        });
    }

    public void add (final String playerName, final String reason, final boolean regex, final boolean ignoreCase) {
        try {
            final PreparedStatement statement = Main.database.connection.prepareStatement(INSERT_FILTER);

            statement.setString(1, playerName);
            statement.setString(2, reason);
            statement.setBoolean(3, regex);
            statement.setBoolean(4, ignoreCase);

            statement.executeUpdate();

            list();
        } catch (final SQLException e) {
            bot.logger.error(e);
        }

        final List<FilteredPlayer> matches = getPlayers(playerName);

        // loop through all the servers too
        for (final Bot bot : bot.bots) {
            for (final FilteredPlayer match : matches) {
                final PlayerEntry entry = bot.players.getEntry(match.playerName);

                if (entry == null) continue;

                bot.filterManager.add(entry, match.reason);
            }
        }
    }

    public void remove (final String playerName) {
        bot.filterManager.remove(playerName);

        try {
            final PreparedStatement statement = Main.database.connection.prepareStatement(REMOVE_FILTER);

            statement.setString(1, playerName);

            statement.executeUpdate();

            list();
        } catch (final SQLException e) {
            bot.logger.error(e);
        }
    }

    public void clear () {
        for (final FilteredPlayer player : localList) bot.filterManager.remove(player.playerName);

        try {
            Main.database.update(CLEAR_FILTER);

            list();
        } catch (final SQLException e) {
            bot.logger.error(e);
        }
    }
}
