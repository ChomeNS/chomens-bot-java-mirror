package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.LoggerUtilities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class IPFilterPlugin implements PlayersPlugin.Listener, CorePlugin.Listener {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS ipFilters (ip VARCHAR(255) PRIMARY KEY, reason VARCHAR(255));";
    private static final String LIST_FILTERS = "SELECT * FROM ipFilters;";
    private static final String INSERT_FILTER = "INSERT INTO ipFilters (ip, reason) VALUES (?, ?);";
    private static final String REMOVE_FILTER = "DELETE FROM ipFilters WHERE ip = ?;";
    private static final String CLEAR_FILTER = "DELETE FROM ipFilters;";

    public static Map<String, String> localList = new LinkedHashMap<>();

    static {
        if (Main.database != null) {
            DatabasePlugin.EXECUTOR_SERVICE.submit(() -> {
                try {
                    Main.database.execute(CREATE_TABLE);
                } catch (final SQLException e) {
                    LoggerUtilities.error(e);
                }
            });

            Main.EXECUTOR.scheduleAtFixedRate(IPFilterPlugin::list, 5, 30, TimeUnit.SECONDS);
        }
    }

    private final Bot bot;

    public IPFilterPlugin (final Bot bot) {
        this.bot = bot;

        if (Main.database == null) return;

        bot.players.addListener(this);
        bot.core.addListener(this);

        bot.executor.scheduleAtFixedRate(this::checkAllPlayers, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void coreReady () {
        checkAllPlayers();
    }

    @Override
    public void queriedPlayerIP (final PlayerEntry target, final String ip) {
        if (localList.isEmpty()) return;

        check(target);
    }

    private void check (final PlayerEntry target) {
        if (bot.options.useCorePlaceBlock) return; // it will spam the place block core so i ignored this

        final String ip = target.ip;

        if (ip == null) return;

        handleFilterManager(ip, target);
    }

    public static Map<String, String> list () {
        final Map<String, String> output = new LinkedHashMap<>();

        try (final ResultSet result = Main.database.query(LIST_FILTERS)) {
            if (result == null) return output;

            while (result.next()) {
                output.put(result.getString("ip"), result.getString("reason"));
            }
        } catch (final SQLException e) {
            LoggerUtilities.error(e);
        }

        localList = output;

        return output;
    }

    public void add (final String ip, final String reason) {
        try {
            final PreparedStatement statement = Main.database.connection.prepareStatement(INSERT_FILTER);

            statement.setString(1, ip);
            statement.setString(2, reason);

            statement.executeUpdate();

            list();
        } catch (final SQLException e) {
            bot.logger.error(e);
        }

        checkAllPlayers();
    }

    private void checkAllPlayers () {
        if (localList.isEmpty()) return;

        bot.executorService.submit(() -> {
            for (final PlayerEntry entry : bot.players.list) check(entry);
        });
    }

    public void remove (final String ip) {
        try {
            final PreparedStatement statement = Main.database.connection.prepareStatement(REMOVE_FILTER);

            statement.setString(1, ip);

            statement.executeUpdate();

            list();
        } catch (final SQLException e) {
            bot.logger.error(e);
        }
    }

    public void clear () {
        try {
            Main.database.update(CLEAR_FILTER);

            list();
        } catch (final SQLException e) {
            bot.logger.error(e);
        }
    }

    private void handleFilterManager (final String ip, final PlayerEntry entry) {
        for (final Map.Entry<String, String> ipEntry : localList.entrySet()) {
            final String eachIP = ipEntry.getKey();
            final String reason = ipEntry.getValue();

            if (entry.profile.equals(bot.profile) || !eachIP.equals(ip)) continue;

            bot.filterManager.add(entry, reason);
        }
    }
}
