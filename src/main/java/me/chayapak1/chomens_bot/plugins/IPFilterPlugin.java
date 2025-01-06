package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.LoggerUtilities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class IPFilterPlugin extends PlayersPlugin.Listener {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS ipFilters (ip VARCHAR(255) PRIMARY KEY);";
    private static final String LIST_FILTERS = "SELECT * FROM ipFilters;";
    private static final String INSERT_FILTER = "INSERT INTO ipFilters (ip) VALUES (?);";
    private static final String REMOVE_FILTER = "DELETE FROM ipFilters WHERE ip = ?;";
    private static final String CLEAR_FILTER = "DELETE FROM ipFilters;";

    public static List<String> localList = new ArrayList<>();

    static {
        if (Main.database != null) {
            DatabasePlugin.executorService.submit(() -> {
                try {
                    Main.database.execute(CREATE_TABLE);
                } catch (SQLException e) {
                    LoggerUtilities.error(e);
                }
            });

            Main.executor.scheduleAtFixedRate(IPFilterPlugin::list, 3, 5, TimeUnit.SECONDS);
        }
    }

    private final Bot bot;

    public IPFilterPlugin (Bot bot) {
        this.bot = bot;

        if (Main.database == null) return;

        bot.players.addListener(this);

        bot.executor.scheduleAtFixedRate(this::checkAllPlayers, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        if (localList.isEmpty()) return;

        check(target);
    }

    private void check (PlayerEntry target) {
        if (bot.options.useCorePlaceBlock) return; // it will spam the place block core so i ignored this

        final CompletableFuture<String> future = bot.players.getPlayerIP(target);

        if (future == null) return;

        future.thenApplyAsync(output -> {
            handleIP(output, target);

            return output;
        });
    }



    public static List<String> list () {
        final List<String> output = new ArrayList<>();

        try (ResultSet result = Main.database.query(LIST_FILTERS)) {
            if (result == null) return output;

            while (result.next()) output.add(result.getString("ip"));
        } catch (SQLException e) {
            LoggerUtilities.error(e);
        }

        localList = output;

        return output;
    }

    public void add (String ip) {
        try {
            final PreparedStatement statement = bot.database.connection.prepareStatement(INSERT_FILTER);

            statement.setString(1, ip);

            statement.executeUpdate();

            list();
        } catch (SQLException e) {
            bot.logger.error(e);
        }

        checkAllPlayers();
    }

    private void checkAllPlayers () {
        if (localList.isEmpty()) return;

        bot.executorService.submit(() -> {
            for (PlayerEntry entry : bot.players.list) check(entry);
        });
    }

    public void remove (String ip) {
        try {
            final PreparedStatement statement = bot.database.connection.prepareStatement(REMOVE_FILTER);

            statement.setString(1, ip);

            statement.executeUpdate();

            list();
        } catch (SQLException e) {
            bot.logger.error(e);
        }
    }

    public void clear () {
        try {
            bot.database.update(CLEAR_FILTER);

            list();
        } catch (SQLException e) {
            bot.logger.error(e);
        }
    }

    private void handleIP (String ip, PlayerEntry entry) {
        for (String eachIP : localList) {
            if (!eachIP.equals(ip)) continue;

            if (entry.profile.getId().equals(bot.profile.getId())) continue;

            bot.filter.doAll(entry);
        }
    }
}
