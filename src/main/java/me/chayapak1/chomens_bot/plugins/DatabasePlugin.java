package me.chayapak1.chomens_bot.plugins;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.Main;

import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabasePlugin {
    // is it OK to have a completely separate ExecutorService to do the executions?
    public static final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat("ExecutorService (database) #%d").build()
    );

    public Connection connection;

    public DatabasePlugin (Configuration config) {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + config.database.address + "/chomens_bot",
                    config.database.username,
                    config.database.password
            );
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }
}
