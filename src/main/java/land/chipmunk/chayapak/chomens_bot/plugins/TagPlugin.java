package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;

// TODO: self care
public class TagPlugin extends CorePlugin.Listener {
    private final Bot bot;

    public final String tag = "chomens_bot";

    public TagPlugin (Bot bot) {
        this.bot = bot;

        bot.core.addListener(this);
    }

    @Override
    public void ready () { // might not be the best idea but whatever
        final String command = "minecraft:tag " + UUIDUtilities.selector(bot.profile.getId()) + " add " + tag;

        bot.core.run(command);
    }
}
