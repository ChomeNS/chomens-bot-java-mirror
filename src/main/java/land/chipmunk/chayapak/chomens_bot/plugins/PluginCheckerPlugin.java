package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;

import java.util.concurrent.TimeUnit;

public class PluginCheckerPlugin extends Bot.Listener {
    public boolean hasExtras;
    public boolean hasEssentials;

    public PluginCheckerPlugin(Bot bot) {
        // dumb
        bot.executor.scheduleAtFixedRate(() -> {
            hasExtras = bot.serverPluginsManager.plugins.contains("Extras");
            hasEssentials = bot.serverPluginsManager.plugins.contains("Essentials");
        }, 0, 5, TimeUnit.SECONDS);
    }
}
