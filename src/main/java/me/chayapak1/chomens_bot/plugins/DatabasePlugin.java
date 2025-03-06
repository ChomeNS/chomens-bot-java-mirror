package me.chayapak1.chomens_bot.plugins;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.util.LoggerUtilities;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabasePlugin {
    public static final ExecutorService executorService = Executors.newFixedThreadPool(
            1,
            new ThreadFactoryBuilder().setNameFormat("ExecutorService (database)").build()
    );

    public Connection connection;

    public DatabasePlugin (Configuration config) {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mariadb://" + config.database.address + "/chomens_bot",
                    config.database.username,
                    config.database.password
            );
        } catch (SQLException e) {
            LoggerUtilities.error(e);
            return;
        }

        for (Bot bot : Main.bots) bot.database = this;
    }

    public boolean execute (String query) throws SQLException {
        final Statement statement = connection.createStatement();

        return statement.execute(query);
    }

    public ResultSet query (String query) throws SQLException {
        final Statement statement = connection.createStatement();

        return statement.executeQuery(query);
    }

    public int update (String query) throws SQLException {
        final Statement statement = connection.createStatement();

        return statement.executeUpdate(query);
    }

    public void stop () {
        executorService.shutdown();

        try {
            connection.close();
        } catch (SQLException e) {
            LoggerUtilities.error(e);
        }
    }
}
