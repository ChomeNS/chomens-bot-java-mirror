package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;

public class AuthCommand extends Command {
    public AuthCommand () {
        super(
                "auth",
                new String[] { "[TrustLevel]" },
                new String[] {},
                TrustLevel.TRUSTED
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final boolean allowSettingOthers = context.trustLevel == TrustLevel.MAX;

        if (!allowSettingOthers) context.checkOverloadArgs(1);

        final Bot bot = context.bot;

        TrustLevel trustLevel = context.getEnum(false, TrustLevel.class);
        if (trustLevel == null) trustLevel = context.trustLevel;

        if (trustLevel.level > context.trustLevel.level) throw new CommandException(
                Component.translatable("commands.auth.error.privilege_escalate")
        );

        final String targetString = context.getString(true, false);

        PlayerEntry target = null;
        if (allowSettingOthers && !targetString.isEmpty()) target = bot.players.getEntry(targetString);
        if (target == null) target = context.sender;

        target.authenticatedTrustLevel = trustLevel;

        if (target.equals(context.sender)) {
            return Component.translatable(
                    "commands.auth.self",
                    bot.colorPalette.defaultColor,
                    target.authenticatedTrustLevel.component
            );
        } else {
            return Component.translatable(
                    "commands.auth.others",
                    bot.colorPalette.defaultColor,
                    Component.text(target.profile.getName(), bot.colorPalette.username),
                    target.authenticatedTrustLevel.component
            );
        }
    }
}
