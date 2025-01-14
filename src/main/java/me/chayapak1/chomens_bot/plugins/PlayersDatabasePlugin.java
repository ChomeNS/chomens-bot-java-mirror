package me.chayapak1.chomens_bot.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.LoggerUtilities;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayersDatabasePlugin extends PlayersPlugin.Listener {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS players (username VARCHAR(255) PRIMARY KEY, data JSON);";
    private static final String INSERT_PLAYER = "INSERT IGNORE INTO players (username, data) VALUES (?, ?);";
    private static final String UPDATE_PLAYER = "UPDATE players SET data = JSON_SET(data, ?, JSON_MERGE_PATCH(data -> ?, ?)) WHERE username = ?;";
    private static final String GET_DATA = "SELECT data FROM players WHERE username = ?;";
    private static final String FIND_ALTS = "SELECT username FROM players WHERE JSON_CONTAINS(data -> '$.ips', JSON_OBJECT(?, ?));";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Bot bot;

    static {
        if (Main.database != null) {
            DatabasePlugin.executorService.submit(() -> {
                try {
                    Main.database.execute(CREATE_TABLE);
                } catch (SQLException e) {
                    LoggerUtilities.error(e);
                }
            });
        }
    }

    public PlayersDatabasePlugin (Bot bot) {
        this.bot = bot;

        if (Main.database == null) return;

        bot.players.addListener(this);
    }

    public JsonNode getPlayerData (String username) {
        if (bot.database == null || bot.database.connection == null) return null;

        try {
            final PreparedStatement statement = bot.database.connection.prepareStatement(GET_DATA);

            statement.setString(1, username);

            final ResultSet result = statement.executeQuery();

            if (!result.isBeforeFirst()) return null; // doesn't exist

            // this will use only the first one in the output
            result.next();
            final String stringJson = result.getString("data");

            return objectMapper.readTree(stringJson);
        } catch (SQLException | JsonProcessingException e) {
            bot.logger.error(e);
            return null;
        }
    }

    public List<String> findPlayerAlts (String ip) {
        try {
            final List<String> output = new ArrayList<>();

            final PreparedStatement statement = bot.database.connection.prepareStatement(FIND_ALTS);

            statement.setString(1, bot.host + ":" + bot.port);
            statement.setString(2, ip);

            final ResultSet result = statement.executeQuery();

            while (result.next()) output.add(result.getString("username"));

            return output;
        } catch (SQLException e) {
            bot.logger.error(e);
            return null;
        }
    }

    @Override
    public void playerJoined (PlayerEntry target) {
        DatabasePlugin.executorService.submit(() -> {
            try {
                final PreparedStatement insertPlayerStatement = bot.database.connection.prepareStatement(INSERT_PLAYER);

                insertPlayerStatement.setString(1, target.profile.getName());

                final ObjectNode baseObject = JsonNodeFactory.instance.objectNode();
                baseObject.put("uuid", target.profile.getIdAsString());
                baseObject.set("ips", JsonNodeFactory.instance.objectNode());
                // ||| this will be replaced after the player leaves, it is here
                // ||| in case the bot leaves before the player leaves it will
                // VVV prevent the last seen entry being empty
                baseObject.set("lastSeen", getLastSeenObject());

                insertPlayerStatement.setString(2, objectMapper.writeValueAsString(baseObject));

                insertPlayerStatement.executeUpdate();

                final CompletableFuture<String> future = bot.players.getPlayerIP(target, true);

                if (future == null) return;

                future.thenApplyAsync(output -> {
                    if (output == null) return null;

                    try {
                        final PreparedStatement updatePlayerStatement = bot.database.connection.prepareStatement(UPDATE_PLAYER);

                        updatePlayerStatement.setString(1, "$.ips");
                        updatePlayerStatement.setString(2, "$.ips");

                        final ObjectNode ipsObject = JsonNodeFactory.instance.objectNode();
                        ipsObject.put(bot.host + ":" + bot.port, output);

                        updatePlayerStatement.setString(3, objectMapper.writeValueAsString(ipsObject));

                        updatePlayerStatement.setString(4, target.profile.getName());

                        updatePlayerStatement.executeUpdate();
                    } catch (SQLException | JsonProcessingException e) {
                        bot.logger.error(e);
                    }

                    return output;
                });
            } catch (SQLException | JsonProcessingException e) {
                bot.logger.error(e);
            }
        });
    }

    @Override
    public void playerLeft(PlayerEntry target) {
        DatabasePlugin.executorService.submit(() -> {
            try {
                final PreparedStatement updatePlayerStatement = bot.database.connection.prepareStatement(UPDATE_PLAYER);

                updatePlayerStatement.setString(1, "$.lastSeen");
                updatePlayerStatement.setString(2, "$.lastSeen");

                final ObjectNode lastSeenObject = getLastSeenObject();

                updatePlayerStatement.setString(3, objectMapper.writeValueAsString(lastSeenObject));

                updatePlayerStatement.setString(4, target.profile.getName());

                updatePlayerStatement.executeUpdate();
            } catch (SQLException | JsonProcessingException e) {
                bot.logger.error(e);
            }
        });
    }

    private ObjectNode getLastSeenObject () {
        final ObjectNode object = JsonNodeFactory.instance.objectNode();

        object.put("time", Instant.now().toEpochMilli());
        object.put("server", bot.host + ":" + bot.port);

        return object;
    }
}
