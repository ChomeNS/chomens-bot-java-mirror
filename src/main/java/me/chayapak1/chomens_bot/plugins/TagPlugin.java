package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.UUIDUtilities;

import java.util.concurrent.TimeUnit;

public class TagPlugin extends CorePlugin.Listener {
    private final Bot bot;

    public final String tag = "chomens_bot";

    public TagPlugin (Bot bot) {
        this.bot = bot;

        bot.executor.scheduleAtFixedRate(this::runCommand, 5, 30, TimeUnit.SECONDS);
    }

    private void runCommand () {
        if (!bot.serverPluginsManager.hasPlugin(ServerPluginsManagerPlugin.EXTRAS)) return;

        final String botSelector = UUIDUtilities.selector(bot.profile.getId());

        bot.core.run("minecraft:tag " + botSelector + " add " + tag);
    }
}
