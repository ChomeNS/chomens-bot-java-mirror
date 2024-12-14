package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.FilteredPlayer;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class FilterPlugin extends PlayersPlugin.Listener {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS filters (name VARCHAR(255) PRIMARY KEY, regex BOOLEAN, ignoreCase BOOLEAN);";
    private static final String LIST_FILTERS = "SELECT * FROM filters;";
    private static final String INSERT_FILTER = "INSERT INTO filters (name, regex, ignoreCase) VALUES (?, ?, ?);";
    private static final String REMOVE_FILTER = "DELETE FROM filters WHERE name = ?;";
    private static final String CLEAR_FILTER = "DELETE FROM filters;";

    public static List<FilteredPlayer> localList = new ArrayList<>();

    static {
        if (Main.database != null) {
            DatabasePlugin.executorService.submit(() -> {
                try {
                    Main.database.execute(CREATE_TABLE);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            Main.executor.scheduleAtFixedRate(FilterPlugin::list, 3, 5, TimeUnit.SECONDS);
        }
    }

    private final Bot bot;

    public FilterPlugin (Bot bot) {
        this.bot = bot;

        if (Main.database == null) return;

        bot.players.addListener(this);

        bot.chat.addListener(new ChatPlugin.Listener() {
            @Override
            public boolean playerMessageReceived(PlayerMessage message) {
                FilterPlugin.this.playerMessageReceived(message);

                return true;
            }
        });

        bot.commandSpy.addListener(new CommandSpyPlugin.Listener() {
            @Override
            public void commandReceived(PlayerEntry sender, String command) {
                FilterPlugin.this.commandSpyMessageReceived(sender, command);
            }
        });

        bot.executor.scheduleAtFixedRate(this::kick, 0, 10, TimeUnit.SECONDS);
    }

    public static List<FilteredPlayer> list () {
        final List<FilteredPlayer> output = new ArrayList<>();

        try (ResultSet result = Main.database.query(LIST_FILTERS)) {
            if (result == null) return output;

            while (result.next()) {
                final FilteredPlayer filteredPlayer = new FilteredPlayer(
                        result.getString("name"),
                        result.getBoolean("regex"),
                        result.getBoolean("ignoreCase")
                );

                output.add(filteredPlayer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

            doAll(target);
        });
    }

    @Override
    public void playerDisplayNameUpdated(PlayerEntry target, Component displayName) {
        final FilteredPlayer player = getPlayer(target.profile.getName());

        if (player == null) return;

        // we use the stringified instead of the component because you can configure the OP and DeOP tag in
        // the extras config
        final String stringifiedDisplayName = ComponentUtilities.stringify(displayName);

        if (stringifiedDisplayName.startsWith("[OP] ")) deOp(target);
    }

    public void commandSpyMessageReceived (PlayerEntry entry, String command) {
        final FilteredPlayer player = getPlayer(entry.profile.getName());

        if (player == null) return;

        if (
                command.startsWith("/mute") ||
                        command.startsWith("/emute") ||
                        command.startsWith("/silence") ||
                        command.startsWith("/esilence") ||
                        command.startsWith("/essentials:mute") ||
                        command.startsWith("/essentials:emute") ||
                        command.startsWith("/essentials:silence") ||
                        command.startsWith("/essentials:esilence")
        ) mute(entry);

        deOp(entry);
        gameMode(entry);
        bot.exploits.kick(entry.profile.getId());
    }

    public void playerMessageReceived (PlayerMessage message) {
        if (message.sender.profile.getName() == null) return;

        final FilteredPlayer player = getPlayer(message.sender.profile.getName());

        if (player == null || message.sender.profile.getId().equals(new UUID(0L, 0L))) return;

        doAll(message.sender);
    }

    public void doAll (PlayerEntry entry) {
        mute(entry);
        deOp(entry);
        gameMode(entry);
        bot.exploits.kick(entry.profile.getId());
    }

    public void mute (PlayerEntry target) { mute(target, ""); }
    public void mute (PlayerEntry target, String reason) {
        bot.core.run("essentials:mute " + target.profile.getIdAsString() + " 10y " + reason);
    }

    public void deOp (PlayerEntry target) {
        bot.core.run("minecraft:execute run deop " + UUIDUtilities.selector(target.profile.getId()));
    }

    public void gameMode(PlayerEntry target) {
        bot.core.run("minecraft:gamemode adventure " + UUIDUtilities.selector(target.profile.getId()));
    }

    public void kick () {
        for (PlayerEntry target : bot.players.list) {
            final FilteredPlayer player = getPlayer(target.profile.getName());

            if (player == null) continue;

            bot.exploits.kick(target.profile.getId());
        }
    }

    public void add (String playerName, boolean regex, boolean ignoreCase) {
        try {
            final PreparedStatement statement = bot.database.connection.prepareStatement(INSERT_FILTER);

            statement.setString(1, playerName);
            statement.setBoolean(2, regex);
            statement.setBoolean(3, ignoreCase);

            statement.executeUpdate();

            list();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        final PlayerEntry target = bot.players.getEntry(playerName); // fix not working for regex and ignorecase

        if (target == null) return;

        doAll(target);
    }

    public void remove (String playerName) {
        try {
            final PreparedStatement statement = bot.database.connection.prepareStatement(REMOVE_FILTER);

            statement.setString(1, playerName);

            statement.executeUpdate();

            list();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clear () {
        try {
            bot.database.update(CLEAR_FILTER);

            list();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
