package land.chipmunk.chayapak.chomens_bot.plugins;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.FilteredPlayer;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.util.PersistentDataUtilities;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class FilterPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    public static JsonArray filteredPlayers = new JsonArray();

    private final Gson gson = new Gson();

    static {
        if (PersistentDataUtilities.jsonObject.has("filters")) {
            filteredPlayers = PersistentDataUtilities.jsonObject.get("filters").getAsJsonArray();
        }
    }

    public FilterPlugin (Bot bot) {
        this.bot = bot;

        bot.players.addListener(this);

        bot.chat.addListener(new ChatPlugin.Listener() {
            @Override
            public void playerMessageReceived(PlayerMessage message) {
                FilterPlugin.this.playerMessageReceived(message);
            }
        });

        bot.commandSpy.addListener(new CommandSpyPlugin.Listener() {
            @Override
            public void commandReceived(PlayerEntry sender, String command) {
                FilterPlugin.this.commandSpyMessageReceived(sender);
            }
        });

        bot.executor.scheduleAtFixedRate(this::kick, 0, 10, TimeUnit.SECONDS);
    }

    private FilteredPlayer getPlayer (String name) {
        // mess
        // also regex and ignorecase codes from greplog plugin
        FilteredPlayer filteredPlayer = null;
        for (JsonElement filteredPlayerElement : filteredPlayers) {
            final FilteredPlayer _filteredPlayer = gson.fromJson(filteredPlayerElement, FilteredPlayer.class);

            if (_filteredPlayer.regex) {
                Pattern pattern = null;
                if (_filteredPlayer.ignoreCase) {
                    pattern = Pattern.compile(_filteredPlayer.playerName, Pattern.CASE_INSENSITIVE);
                } else {
                    try {
                        pattern = Pattern.compile(_filteredPlayer.playerName);
                    } catch (Exception e) {
                        bot.chat.tellraw(Component.text(e.toString()).color(NamedTextColor.RED));
                    }
                }

                if (pattern == null) break;

                if (pattern.matcher(name).find()) {
                    filteredPlayer = _filteredPlayer;
                    break;
                }
            } else {
                if (_filteredPlayer.ignoreCase) {
                    if (_filteredPlayer.playerName.toLowerCase().equals(name)) {
                        filteredPlayer = _filteredPlayer;
                        break;
                    }
                } else {
                    if (_filteredPlayer.playerName.equals(name)) {
                        filteredPlayer = _filteredPlayer;
                        break;
                    }
                }
            }
        }

        return filteredPlayer;
    }

    @Override
    public void playerJoined (PlayerEntry target) {
        final FilteredPlayer player = getPlayer(target.profile.getName());

        if (player == null) return;

        deOp(target);
        mute(target);

        bot.exploits.kick(target.profile.getId());
    }

    public void commandSpyMessageReceived (PlayerEntry sender) {
        final FilteredPlayer player = getPlayer(sender.profile.getName());

        if (player == null) return;

        deOp(sender);
    }

    public void playerMessageReceived (PlayerMessage message) {
        if (message.sender.profile.getName() == null) return;

        final FilteredPlayer player = getPlayer(message.sender.profile.getName());

        if (player == null) return;

        deOp(message.sender);
        mute(message.sender);
    }

    private void mute (PlayerEntry target) {
        bot.core.run("essentials:mute " + target.profile.getIdAsString() + " 10y");
    }

    private void deOp (PlayerEntry target) {
        bot.core.run("minecraft:execute run deop " + UUIDUtilities.selector(target.profile.getId()));
    }

    public void kick () {
        for (PlayerEntry target : bot.players.list) {
            final FilteredPlayer player = getPlayer(target.profile.getName());

            if (player == null) continue;

            bot.exploits.kick(target.profile.getId());
        }
    }

    public void add (String playerName, boolean regex, boolean ignoreCase) {
        filteredPlayers.add(gson.fromJson(gson.toJson(new FilteredPlayer(playerName, regex, ignoreCase)), JsonElement.class));

        PersistentDataUtilities.put("filters", filteredPlayers);

        final PlayerEntry target = bot.players.getEntry(playerName); // fix not working for regex and ignorecase

        if (target == null) return;

        deOp(target);
        mute(target);
    }

    public FilteredPlayer remove (int index) {
        final JsonElement element = filteredPlayers.remove(index);

        PersistentDataUtilities.put("filters", filteredPlayers);

        return gson.fromJson(element, FilteredPlayer.class);
    }

    public void clear () {
        for (int i = 0; i <= filteredPlayers.size(); i++) {
            filteredPlayers.remove(i);
        }

        PersistentDataUtilities.put("filters", filteredPlayers);
    }
}
