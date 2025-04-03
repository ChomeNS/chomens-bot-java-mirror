package me.chayapak1.chomens_bot.plugins;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.kyori.adventure.text.Component;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class DatabasePlugin {
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("ExecutorService (database)")
                    .build()
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
        }
    }

    public void checkOverloaded () throws CommandException {
        final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) EXECUTOR_SERVICE;

        if (threadPoolExecutor.getQueue().size() > 20) throw new CommandException(
                Component.text("The executor service is filled with requests!")
        );
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
        EXECUTOR_SERVICE.shutdown();

        try {
            connection.close();
        } catch (SQLException e) {
            LoggerUtilities.error(e);
        }
    }
}
