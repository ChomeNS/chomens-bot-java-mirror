package me.chayapak1.chomens_bot.selfCares.essentials;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.EssentialsSelfCare;
import net.kyori.adventure.text.Component;

public class MuteSelfCare extends EssentialsSelfCare {
    public MuteSelfCare (final Bot bot) {
        super(bot);
        this.needsRunning = false;
    }

    @Override
    public boolean shouldRun () {
        return bot.serverFeatures.hasEssentials && bot.config.selfCare.mute;
    }

    @Override
    public void onMessageReceived (final Component component, final String string) {
        if (string.startsWith(messages.muted)) this.needsRunning = true;
        else if (string.equals(messages.unmuted)) this.needsRunning = false;
    }

    @Override
    public void run () {
        this.runCommand("essentials:mute " + getUUIDOrBlank());

        this.needsRunning = false; // is this a good idea? can this be hacked by AdminEvil?
    }

    @Override
    public void cleanup () {
        this.needsRunning = false;
    }
}
