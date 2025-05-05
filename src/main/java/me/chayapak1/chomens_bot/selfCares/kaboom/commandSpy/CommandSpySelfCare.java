package me.chayapak1.chomens_bot.selfCares.kaboom.commandSpy;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import net.kyori.adventure.text.Component;

public class CommandSpySelfCare extends SelfCare {
    public CommandSpySelfCare (final Bot bot) {
        super(bot);
        this.needsRunning = true;
    }

    @Override
    public boolean shouldRun () {
        return bot.serverFeatures.hasCommandSpy && bot.config.selfCare.cspy;
    }

    @Override
    public void onMessageReceived (final Component component, final String string) {
        if (string.equals("Successfully enabled CommandSpy")) this.needsRunning = false;
        else if (string.equals("Successfully disabled CommandSpy")) this.needsRunning = true;
    }

    @Override
    public void run () {
        if (bot.options.useChat || !bot.options.coreCommandSpy) {
            bot.chat.sendCommandInstantly("commandspy:commandspy on");
        } else {
            bot.core.run("commandspy:commandspy " + bot.profile.getIdAsString() + " on");
        }
    }

    @Override
    public void cleanup () {
        this.needsRunning = true;
    }
}
