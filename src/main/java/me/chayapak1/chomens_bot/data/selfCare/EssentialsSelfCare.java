package me.chayapak1.chomens_bot.data.selfCare;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;

public abstract class EssentialsSelfCare extends SelfCare {
    public final Configuration.BotOption.EssentialsMessages messages;

    public EssentialsSelfCare (final Bot bot) {
        super(bot);
        this.messages = bot.options.essentialsMessages;
        this.needsRunning = true;
    }

    @Override
    public void cleanup () {
        this.needsRunning = true;
    }

    private String getUseCoreValueOrBlank (final String value) {
        return !bot.options.useChat ?
                bot.username + " " :
                "";
    }

    public String getUsernameOrBlank () {
        return getUseCoreValueOrBlank(bot.username);
    }

    public String getUUIDOrBlank () {
        return getUseCoreValueOrBlank(bot.profile.getIdAsString());
    }

    public void runCommand (final String command) {
        if (bot.options.useChat) bot.chat.sendCommandInstantly(command);
        else bot.core.run(command);
    }
}
