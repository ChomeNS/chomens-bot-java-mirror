package me.chayapak1.chomens_bot.selfCares.essentials;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.EssentialsSelfCare;
import net.kyori.adventure.text.Component;

public class VanishSelfCare extends EssentialsSelfCare {
    public boolean visible = !bot.config.selfCare.vanish;

    private boolean isVanished = false;

    public VanishSelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public boolean shouldRun () {
        return bot.serverFeatures.hasEssentials
                && bot.config.selfCare.vanish
                && this.visible == isVanished;
    }

    @Override
    public void onMessageReceived (final Component component, final String string) {
        if (
                string.equals(String.format(messages.vanishEnable1, bot.username))
                    || string.equals(messages.vanishEnable2)
        ) {
            this.needsRunning = visible;
            this.isVanished = true;
        } else if (string.equals(String.format(messages.vanishDisable, bot.username))) {
            this.needsRunning = !visible;
            this.isVanished = false;
        }
    }

    @Override
    public void run () {
        this.runCommand("essentials:vanish " + getUsernameOrBlank() + (this.visible ? "disable" : "enable"));
    }

    @Override
    public void cleanup () {
        super.cleanup();
        this.isVanished = false;
    }
}
