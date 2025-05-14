package me.chayapak1.chomens_bot.selfCares.kaboom.extras;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import net.kyori.adventure.text.Component;

public class PrefixSelfCare extends SelfCare {
    public PrefixSelfCare (final Bot bot) {
        super(bot);
        this.needsRunning = true;
    }

    @Override
    public boolean shouldRun () {
        return bot.serverFeatures.hasExtras && bot.config.selfCare.prefix.enabled;
    }

    @Override
    public void onMessageReceived (final Component component, final String string) {
        if (
                string.equals("You now have the tag: " + bot.config.selfCare.prefix.prefix)
                    || string.equals("Something went wrong while saving the prefix. Please check console.")
        ) {
            this.needsRunning = false;
        } else if (
                string.startsWith("You no longer have a tag")
                        || string.startsWith("You now have the tag: ")
        ) {
            this.needsRunning = true;
        }
    }

    @Override
    public void run () {
        bot.chat.sendCommandInstantly("extras:prefix " + bot.config.selfCare.prefix.prefix);
    }

    @Override
    public void cleanup () {
        this.needsRunning = true;
    }
}
