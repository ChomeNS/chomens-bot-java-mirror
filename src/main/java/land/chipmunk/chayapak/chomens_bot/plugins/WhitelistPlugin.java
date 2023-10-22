package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;

import java.util.ArrayList;
import java.util.List;

public class WhitelistPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    public final List<String> list = new ArrayList<>();

    private boolean enabled = false;

    public WhitelistPlugin (Bot bot) {
        this.bot = bot;

        bot.players.addListener(this);

        bot.chat.addListener(new ChatPlugin.Listener() {
            @Override
            public void playerMessageReceived(PlayerMessage message) {
                WhitelistPlugin.this.playerMessageReceived(message);
            }
        });

        bot.commandSpy.addListener(new CommandSpyPlugin.Listener() {
            @Override
            public void commandReceived(PlayerEntry sender, String command) {
                WhitelistPlugin.this.commandReceived(sender, command);
            }
        });
    }

    public void enable () {
        enabled = true;

        for (PlayerEntry entry : bot.players.list) {
            if (list.contains(entry.profile.getName())) continue;

            list.add(entry.profile.getName());
        }
    }

    public void disable () {
        enabled = false;
    }

    public void add (String player) { list.add(player); }
    public void remove (String player) {
        list.removeIf(eachPlayer -> eachPlayer.equals(player));

        final PlayerEntry entry = bot.players.getEntry(player);

        if (entry != null) handle(entry);
    }
    public void clear () {
        list.removeIf(eachPlayer -> !eachPlayer.equals(bot.profile.getName()));
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        handle(target);
    }

    public void playerMessageReceived (PlayerMessage message) {
        handle(message.sender);
    }

    public void commandReceived (PlayerEntry entry, String command) {
        if (!enabled || list.contains(entry.profile.getName())) return;

        if (
                command.startsWith("mute") ||
                        command.startsWith("silence") ||
                        command.startsWith("emute") ||
                        command.startsWith("esilence") ||
                        command.startsWith("essentials:mute") ||
                        command.startsWith("essentials:silence") ||
                        command.startsWith("essentials:emute") ||
                        command.startsWith("essentials:esilence")
        ) bot.filter.mute(entry);
        bot.filter.deOp(entry);
        bot.filter.gameMode(entry);
        bot.exploits.kick(entry.profile.getId());
    }

    private void handle (PlayerEntry entry) {
        if (!enabled || list.contains(entry.profile.getName())) return;

        bot.filter.doAll(entry);
    }
}
