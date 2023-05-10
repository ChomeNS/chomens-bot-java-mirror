package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.data.FilteredPlayer;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FilterPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    @Getter private final List<FilteredPlayer> filteredPlayers = new ArrayList<>();

    public FilterPlugin (Bot bot) {
        this.bot = bot;

        bot.players().addListener(this);

        bot.tick().addListener(new TickPlugin.Listener() {
            @Override
            public void onTick() {
                tick();
            }
        });
    }

    private FilteredPlayer getPlayer (String name) {
        // mess
        // also regex and ignorecase codes from greplog plugin
        FilteredPlayer filteredPlayer = null;
        for (FilteredPlayer _filteredPlayer : filteredPlayers) {
            if (_filteredPlayer.regex) {
                Pattern pattern = null;
                if (_filteredPlayer.ignoreCase) {
                    pattern = Pattern.compile("\\b" + _filteredPlayer.playerName + "\\b", Pattern.CASE_INSENSITIVE);
                } else {
                    try {
                        pattern = Pattern.compile("\\b" + _filteredPlayer.playerName + "\\b");
                    } catch (Exception e) {
                        bot.chat().tellraw(Component.text(e.toString()).color(NamedTextColor.RED));
                    }
                }

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
    public void playerJoined (MutablePlayerListEntry target) {
        final FilteredPlayer player = getPlayer(target.profile().getName());

        if (player == null) return;

        bot.core().run("essentials:mute " + target.profile().getIdAsString() + " 10y");
        bot.core().run("minecraft:execute run deop " + UUIDUtilities.selector(target.profile().getId()));
        bot.exploits().kick(target.profile().getId());
    }

    public void tick () {
        for (MutablePlayerListEntry target : bot.players().list()) {
            final FilteredPlayer player = getPlayer(target.profile().getName());

            if (player == null) continue;


            bot.exploits().kick(target.profile().getId());
        }
    }

    public void add (String playerName, boolean regex, boolean ignoreCase) {
        filteredPlayers.add(new FilteredPlayer(playerName, regex, ignoreCase));
    }

    public FilteredPlayer remove (int index) {
        return filteredPlayers.remove(index);
    }

    public void clear () {
        filteredPlayers.clear();
    }
}
