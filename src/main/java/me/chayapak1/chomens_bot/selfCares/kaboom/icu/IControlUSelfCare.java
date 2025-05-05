package me.chayapak1.chomens_bot.selfCares.kaboom.icu;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;

import java.util.UUID;

public class IControlUSelfCare extends SelfCare {
    public IControlUSelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public boolean shouldRun () {
        return false;
    }

    @Override
    public void onCommandSpyMessageReceived (final PlayerEntry sender, final String command) {
        if (!bot.serverFeatures.hasIControlU || !bot.config.selfCare.icu) return;

        final String[] args = command.split("\\s+");

        if (args.length < 3) return;

        if (
                (!args[0].equals("/icontrolu:icu") && !args[0].equals("/icu"))
                        || !args[1].equalsIgnoreCase("control")
        ) return;

        // interestingly, icu only uses the third argument for the player, and not a greedy string
        final String player = args[2];

        PlayerEntry target = bot.players.getEntryTheBukkitWay(player);

        if (target == null && args[2].matches("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})")) {
            target = bot.players.getEntry(UUID.fromString(args[2]));
        }

        if (target == null || !target.profile.getId().equals(bot.profile.getId())) return;

        bot.core.run("essentials:sudo " + sender.profile.getIdAsString() + " icu stop");
    }
}
