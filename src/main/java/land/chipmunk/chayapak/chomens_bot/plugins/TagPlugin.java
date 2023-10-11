package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;

import java.util.concurrent.TimeUnit;

public class TagPlugin extends CorePlugin.Listener {
    private final Bot bot;

    public final String tag = "chomens_bot";

    public TagPlugin (Bot bot) {
        this.bot = bot;

        bot.executor.scheduleAtFixedRate(this::runCommand, 5, 20, TimeUnit.SECONDS);
    }

    private void runCommand () {
        final String botSelector = UUIDUtilities.selector(bot.profile.getId());

        bot.core.run("minecraft:tag " + botSelector + " add " + tag);
    }
}
