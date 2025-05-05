package me.chayapak1.chomens_bot.selfCares.essentials;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.EssentialsSelfCare;
import net.kyori.adventure.text.Component;

public class SocialSpySelfCare extends EssentialsSelfCare {
    public SocialSpySelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public boolean shouldRun () {
        return bot.serverFeatures.hasEssentials && bot.config.selfCare.socialspy;
    }

    @Override
    public void onMessageReceived (final Component component, final String string) {
        if (string.equals(String.format(messages.socialSpyEnable, bot.username))) this.needsRunning = false;
        else if (string.equals(String.format(messages.socialSpyDisable, bot.username))) this.needsRunning = true;
    }

    @Override
    public void run () {
        this.runCommand("essentials:socialspy " + getUsernameOrBlank() + "enable");
    }
}
