package me.chayapak1.chomens_bot.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.FilteredPlayer;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.PersistentDataUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class FilterPlugin extends PlayersPlugin.Listener {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    private final Bot bot;

    public static ArrayNode filteredPlayers = (ArrayNode) PersistentDataUtilities.getOrDefault("filters", JsonNodeFactory.instance.arrayNode());

    public FilterPlugin (Bot bot) {
        this.bot = bot;

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

    private FilteredPlayer getPlayer (String name) {
        for (JsonNode filteredPlayerElement : filteredPlayers.deepCopy()) {
            try {
                final FilteredPlayer filteredPlayer = objectMapper.treeToValue(filteredPlayerElement, FilteredPlayer.class);

                if (matchesPlayer(name, filteredPlayer)) {
                    return filteredPlayer;
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
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
        filteredPlayers.add(objectMapper.valueToTree(new FilteredPlayer(playerName, regex, ignoreCase)));

        PersistentDataUtilities.put("filters", filteredPlayers);

        final PlayerEntry target = bot.players.getEntry(playerName); // fix not working for regex and ignorecase

        if (target == null) return;

        doAll(target);
    }

    public FilteredPlayer remove (int index) {
        final JsonNode element = filteredPlayers.remove(index);

        PersistentDataUtilities.put("filters", filteredPlayers);

        try {
            return objectMapper.treeToValue(element, FilteredPlayer.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void clear () {
        while (!filteredPlayers.isEmpty()) filteredPlayers.remove(0);

        PersistentDataUtilities.put("filters", filteredPlayers);
    }
}
