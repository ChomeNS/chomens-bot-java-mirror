package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.filter.FilteredPlayer;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
            DatabasePlugin.executorService.submit(() -> {
                try {
                    Main.database.execute(CREATE_TABLE);
                } catch (SQLException e) {
                    LoggerUtilities.error(e);
                }
            });

            Main.executor.scheduleAtFixedRate(PlayerFilterPlugin::list, 5, 30, TimeUnit.SECONDS);
        }
    }

    private final Bot bot;

    public PlayerFilterPlugin(Bot bot) {
        this.bot = bot;

        if (Main.database == null) return;

        bot.players.addListener(this);
    }

    public static List<FilteredPlayer> list () {
        final List<FilteredPlayer> output = new ArrayList<>();

        try (ResultSet result = Main.database.query(LIST_FILTERS)) {
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
        } catch (SQLException e) {
            LoggerUtilities.error(e);
        }

        localList = output;

        return output;
    }

    private FilteredPlayer getPlayer (String name) {
        for (FilteredPlayer filteredPlayer : localList) {
            if (matchesPlayer(name, filteredPlayer)) {
                return filteredPlayer;
            }
        }

        return null;
    }

    private List<FilteredPlayer> getPlayers (String name) {
        final List<FilteredPlayer> matches = new ArrayList<>();

        for (FilteredPlayer filteredPlayer : localList) {
            if (matchesPlayer(name, filteredPlayer)) {
                matches.add(filteredPlayer);
            }
        }

        return matches;
    }

    private boolean matchesPlayer (String name, FilteredPlayer player) {
        if (player.regex) {
            final Pattern pattern = compilePattern(player);

            return pattern != null && pattern.matcher(name).find();
        } else {
            return compareNames(name, player);
        }
    }

    private Pattern compilePattern (FilteredPlayer player) {
        try {
            final int flags = player.ignoreCase ? Pattern.CASE_INSENSITIVE : 0;

            return Pattern.compile(player.playerName, flags);
        } catch (Exception e) {
            bot.chat.tellraw(Component.text(e.toString()).color(NamedTextColor.RED));
            return null;
        }
    }

    private boolean compareNames (String name, FilteredPlayer player) {
        final String playerName = player.ignoreCase ? player.playerName.toLowerCase() : player.playerName;
        final String targetName = player.ignoreCase ? name.toLowerCase() : name;

        return playerName.equals(targetName);
    }

    @Override
    public void playerJoined (PlayerEntry target) {
        bot.executorService.submit(() -> {
            final FilteredPlayer player = getPlayer(target.profile.getName());

            if (player == null) return;

            bot.filterManager.add(target, player.reason);
        });
    }

    public void add (String playerName, String reason, boolean regex, boolean ignoreCase) {
        try {
            final PreparedStatement statement = bot.database.connection.prepareStatement(INSERT_FILTER);

            statement.setString(1, playerName);
            statement.setString(2, reason);
            statement.setBoolean(3, regex);
            statement.setBoolean(4, ignoreCase);

            statement.executeUpdate();

            list();
        } catch (SQLException e) {
            bot.logger.error(e);
        }

        final List<FilteredPlayer> matches = getPlayers(playerName);

        for (FilteredPlayer match : matches) {
            final PlayerEntry entry = bot.players.getEntry(match.playerName);

            if (entry == null) continue;

            bot.filterManager.add(entry, match.reason);
        }
    }

    public void remove (String playerName) {
        bot.filterManager.remove(playerName);

        try {
            final PreparedStatement statement = bot.database.connection.prepareStatement(REMOVE_FILTER);

            statement.setString(1, playerName);

            statement.executeUpdate();

            list();
        } catch (SQLException e) {
            bot.logger.error(e);
        }
    }

    public void clear () {
        for (FilteredPlayer player : localList) bot.filterManager.remove(player.playerName);

        try {
            bot.database.update(CLEAR_FILTER);

            list();
        } catch (SQLException e) {
            bot.logger.error(e);
        }
    }
}
