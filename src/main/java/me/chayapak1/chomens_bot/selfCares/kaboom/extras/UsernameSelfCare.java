package me.chayapak1.chomens_bot.selfCares.kaboom.extras;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import me.chayapak1.chomens_bot.util.IllegalCharactersUtilities;

public class UsernameSelfCare extends SelfCare {
    private long usernameStartTime;
    private long successTime;

    public UsernameSelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public void onPlayerChangedUsername (final PlayerEntry target, final String from, final String to) {
        if (target.profile.getId().equals(bot.profile.getId())) {
            this.needsRunning = !to.equals(bot.username);

            if (this.needsRunning) this.usernameStartTime = System.currentTimeMillis();
            else this.successTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean shouldRun () {
        final String username = bot.username.replace("§", "&");

        return // running less than 2 seconds is useless since the bot will just get a cooldown message
                (System.currentTimeMillis() - usernameStartTime) >= 2 * 1000
                        && (System.currentTimeMillis() - successTime) >= 4 * 1000 // prevents the bot from spamming
                        && IllegalCharactersUtilities.isValidChatString(username);
    }

    @Override
    public void run () {
        bot.chat.sendCommandInstantly("extras:username " + bot.username);
    }
}
