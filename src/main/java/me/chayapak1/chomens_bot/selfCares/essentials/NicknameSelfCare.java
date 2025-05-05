package me.chayapak1.chomens_bot.selfCares.essentials;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.EssentialsSelfCare;
import net.kyori.adventure.text.Component;

public class NicknameSelfCare extends EssentialsSelfCare {
    public NicknameSelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public boolean shouldRun () {
        return bot.serverFeatures.hasEssentials && bot.config.selfCare.nickname;
    }

    @Override
    public void onMessageReceived (final Component component, final String string) {
        if (string.equals(messages.nickNameRemove)) this.needsRunning = false;
        else if (string.startsWith(messages.nickNameSet)) this.needsRunning = true;
    }

    @Override
    public void run () {
        this.runCommand("essentials:nickname " + getUUIDOrBlank() + "off");
    }
}
