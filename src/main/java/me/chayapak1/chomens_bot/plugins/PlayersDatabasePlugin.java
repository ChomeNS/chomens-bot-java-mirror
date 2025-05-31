package me.chayapak1.chomens_bot.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayersDatabasePlugin implements Listener {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS players (username VARCHAR(255) PRIMARY KEY, data LONGTEXT);";
    private static final String INSERT_PLAYER = "INSERT IGNORE INTO players (username, data) VALUES (?, ?);";
    private static final String UPDATE_PLAYER = "UPDATE players SET data = JSON_SET(data, ?, JSON_MERGE_PATCH(JSON_EXTRACT(data, ?), ?)) WHERE username = ?;";
    private static final String GET_DATA = "SELECT data FROM players WHERE username = ?;";
    private static final String GET_IP = "SELECT JSON_UNQUOTE(JSON_VALUE(data, ?)) AS ip FROM players WHERE username = ?;";
    private static final String FIND_ALTS_SINGLE_SERVER = "SELECT * FROM players WHERE JSON_CONTAINS(JSON_EXTRACT(data, '$.ips'), JSON_OBJECT(?, ?)) LIMIT ?;";
    private static final String FIND_ALTS_ALL_SERVERS = "SELECT * FROM players WHERE JSON_SEARCH(JSON_EXTRACT(data, '$.ips'), 'one', ?) IS NOT NULL LIMIT ?;";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Bot bot;

    static {
        if (Main.database != null) {
            DatabasePlugin.EXECUTOR_SERVICE.execute(() -> {
                try {
                    Main.database.execute(CREATE_TABLE);
                } catch (final SQLException e) {
                    LoggerUtilities.error(e);
                }
            });
        }
    }

    public PlayersDatabasePlugin (final Bot bot) {
        this.bot = bot;

        if (Main.database == null) return;

        bot.listener.addListener(this);
    }

    public JsonNode getPlayerData (final String username) {
        if (Main.database == null || Main.database.connection == null) return null;

        try {
            final PreparedStatement statement = Main.database.connection.prepareStatement(GET_DATA);

            statement.setString(1, username);

            final ResultSet result = statement.executeQuery();

            if (!result.isBeforeFirst()) return null; // doesn't exist

            // this will use only the first one in the output
            result.next();
            final String stringJson = result.getString("data");

            return objectMapper.readTree(stringJson);
        } catch (final SQLException | JsonProcessingException e) {
            bot.logger.error(e);
            return null;
        }
    }

    public String getPlayerIP (final String username) {
        if (Main.database == null || Main.database.connection == null) return null;

        try {
            final PreparedStatement statement = Main.database.connection.prepareStatement(GET_IP);

            // this may be dangerous but the server address is configured only in the config
            // so this should still be safe
            statement.setString(1, "$.ips.\"" + bot.getServerString(true) + "\"");
            statement.setString(2, username);

            final ResultSet result = statement.executeQuery();

            if (!result.isBeforeFirst()) return null; // no ip for player in this server

            result.next();

            return result.getString("ip");
        } catch (final SQLException e) {
            bot.logger.error(e);
            return null;
        }
    }

    public Map<String, JsonNode> findPlayerAlts (final String ip, final boolean allServer, final int limit) {
        try {
            final Map<String, JsonNode> output = new HashMap<>();

            final PreparedStatement statement;

            if (allServer) {
                statement = Main.database.connection.prepareStatement(FIND_ALTS_ALL_SERVERS);

                statement.setString(1, ip);
                statement.setInt(2, limit);
            } else {
                statement = Main.database.connection.prepareStatement(FIND_ALTS_SINGLE_SERVER);

                statement.setString(1, bot.getServerString(true));
                statement.setString(2, ip);
                statement.setInt(3, limit);
            }

            final ResultSet result = statement.executeQuery();

            while (result.next()) {
                output.put(
                        result.getString("username"),
                        objectMapper.readTree(result.getString("data"))
                );
            }

            return output;
        } catch (final SQLException | JsonProcessingException e) {
            bot.logger.error(e);
            return null;
        }
    }

    @Override
    public void onPlayerJoined (final PlayerEntry target) {
        DatabasePlugin.EXECUTOR_SERVICE.execute(() -> {
            try {
                final PreparedStatement insertPlayerStatement = Main.database.connection.prepareStatement(INSERT_PLAYER);

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
            } catch (final SQLException | JsonProcessingException e) {
                bot.logger.error(e);
            }
        });
    }

    @Override
    public void onQueriedPlayerIP (final PlayerEntry target, final String ip) {
        DatabasePlugin.EXECUTOR_SERVICE.execute(() -> {
            try {
                final PreparedStatement updatePlayerStatement = Main.database.connection.prepareStatement(UPDATE_PLAYER);

                updatePlayerStatement.setString(1, "$.ips");
                updatePlayerStatement.setString(2, "$.ips");

                final ObjectNode ipsObject = JsonNodeFactory.instance.objectNode();
                ipsObject.put(bot.getServerString(true), ip);

                updatePlayerStatement.setString(3, objectMapper.writeValueAsString(ipsObject));

                updatePlayerStatement.setString(4, target.profile.getName());

                updatePlayerStatement.executeUpdate();
            } catch (final SQLException | JsonProcessingException e) {
                bot.logger.error(e);
            }
        });
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        if (Main.stopping) return;

        synchronized (bot.players.list) {
            final List<PlayerEntry> clonedList = new ArrayList<>(bot.players.list);

            DatabasePlugin.EXECUTOR_SERVICE.execute(() -> {
                for (final PlayerEntry target : clonedList) {
                    updateLastSeenEntry(target);
                }
            });
        }
    }

    @Override
    public void onPlayerLeft (final PlayerEntry target) {
        DatabasePlugin.EXECUTOR_SERVICE.execute(() -> updateLastSeenEntry(target));
    }

    private void updateLastSeenEntry (final PlayerEntry target) {
        try {
            final PreparedStatement updatePlayerStatement = Main.database.connection.prepareStatement(UPDATE_PLAYER);

            updatePlayerStatement.setString(1, "$.lastSeen");
            updatePlayerStatement.setString(2, "$.lastSeen");

            final ObjectNode lastSeenObject = getLastSeenObject();

            updatePlayerStatement.setString(3, objectMapper.writeValueAsString(lastSeenObject));

            updatePlayerStatement.setString(4, target.profile.getName());

            updatePlayerStatement.executeUpdate();
        } catch (final SQLException | JsonProcessingException e) {
            bot.logger.error(e);
        }
    }

    private ObjectNode getLastSeenObject () {
        final ObjectNode object = JsonNodeFactory.instance.objectNode();

        object.put("time", Instant.now().toEpochMilli());
        object.put("server", bot.getServerString(true));

        return object;
    }
}
