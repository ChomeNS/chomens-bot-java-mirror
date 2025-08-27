package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.filter.PlayerFilter;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.LoggerUtilities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PlayerFilterPlugin implements Listener {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS filters (name VARCHAR(255) PRIMARY KEY, reason VARCHAR(2048), regex BOOLEAN, ignoreCase BOOLEAN);";
    private static final String LIST_FILTERS = "SELECT * FROM filters;";
    private static final String INSERT_FILTER = "INSERT INTO filters (name, reason, regex, ignoreCase) VALUES (?, ?, ?, ?);";
    private static final String REMOVE_FILTER = "DELETE FROM filters WHERE name = ?;";
    private static final String CLEAR_FILTER = "DELETE FROM filters;";

    public static List<PlayerFilter> localList = new ObjectArrayList<>();

    static {
        if (Main.database != null) {
            DatabasePlugin.EXECUTOR_SERVICE.execute(() -> {
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

        bot.listener.addListener(this);
    }

    public static List<PlayerFilter> list () {
        final List<PlayerFilter> output = new ArrayList<>();

        try (final ResultSet result = Main.database.query(LIST_FILTERS)) {
            if (result == null) return output;

            while (result.next()) {
                final PlayerFilter playerFilter = new PlayerFilter(
                        result.getString("name"),
                        result.getString("reason"),
                        result.getBoolean("regex"),
                        result.getBoolean("ignoreCase")
                );

                output.add(playerFilter);
            }
        } catch (final SQLException e) {
            LoggerUtilities.error(e);
        }

        localList = output;

        return output;
    }

    private PlayerFilter getPlayer (final String name) {
        for (final PlayerFilter playerFilter : localList) {
            if (matchesPlayer(name, playerFilter)) {
                return playerFilter;
            }
        }

        return null;
    }

    private boolean matchesPlayer (final String name, final PlayerFilter player) {
        if (player.regex()) {
            final Pattern pattern = compilePattern(player);

            return pattern != null && pattern.matcher(name).find();
        } else {
            return compareNames(name, player);
        }
    }

    private Pattern compilePattern (final PlayerFilter player) {
        try {
            final int flags = player.ignoreCase() ? Pattern.CASE_INSENSITIVE : 0;

            return Pattern.compile(player.playerName(), flags);
        } catch (final Exception e) {
            bot.logger.error("Error compiling player filter regex " + player.playerName() + " (this shouldn't happen):");
            bot.logger.error(e);
            return null;
        }
    }

    private boolean compareNames (final String name, final PlayerFilter player) {
        final String playerName = player.ignoreCase() ? player.playerName().toLowerCase() : player.playerName();
        final String targetName = player.ignoreCase() ? name.toLowerCase() : name;

        return playerName.equals(targetName);
    }

    @Override
    public void onPlayerJoined (final PlayerEntry target) {
        bot.executorService.execute(() -> {
            final PlayerFilter player = getPlayer(target.profile.getName());
            if (player == null) return;
            bot.filterManager.add(target, player.reason());
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

        // loop through all the servers and check too
        for (final Bot bot : bot.bots) {
            synchronized (bot.players.list) {
                for (final PlayerEntry entry : bot.players.list) {
                    final PlayerFilter player = getPlayer(entry.profile.getName());
                    if (player == null) continue;
                    bot.filterManager.add(entry, player.reason());
                }
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
        for (final PlayerFilter player : localList) bot.filterManager.remove(player.playerName());

        try {
            Main.database.update(CLEAR_FILTER);
            list();
        } catch (final SQLException e) {
            bot.logger.error(e);
        }
    }
}
