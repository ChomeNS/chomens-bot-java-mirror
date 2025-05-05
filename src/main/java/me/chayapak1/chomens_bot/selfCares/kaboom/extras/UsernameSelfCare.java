package me.chayapak1.chomens_bot.selfCares.kaboom.extras;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import me.chayapak1.chomens_bot.util.IllegalCharactersUtilities;
import net.kyori.adventure.text.Component;

public class UsernameSelfCare extends SelfCare {
    private long usernameStartTime = System.currentTimeMillis();

    public UsernameSelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public void onMessageReceived (final Component component, final String string) {
        if (
                string.equals("Successfully set your username to \"" + bot.username + "\"")
                        || string.startsWith("You already have the username \"" + bot.username + "\"")
        ) {
            this.needsRunning = false;
        } else if (
                string.startsWith("Successfully set your username to \"")
                        || string.startsWith("You already have the username \"")
        ) {
            this.needsRunning = true;
            this.usernameStartTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean shouldRun () {
        final String username = bot.username.replace("§", "&");

        return // running less than 2 seconds is useless since the bot will just get a cooldown message
                (System.currentTimeMillis() - usernameStartTime) >= 2 * 1000
                        && IllegalCharactersUtilities.isValidChatString(username);
    }

    @Override
    public void run () {
        bot.chat.sendCommandInstantly("extras:username " + bot.username);
    }
}
