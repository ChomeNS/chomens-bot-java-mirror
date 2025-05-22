package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;

import java.util.Arrays;

public class AuthCommand extends Command {
    public AuthCommand () {
        super(
                "auth",
                new String[] {},
                new String[] {},
                TrustLevel.TRUSTED
        );

        final String trustLevels = String.join(" | ", Arrays.stream(TrustLevel.values()).map(Enum::name).toList());

        this.usages = new String[] { "[player] [" + trustLevels + "]" };
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String playerString = context.getString(false, false);

        PlayerEntry player = null;
        if (!playerString.isEmpty()) player = bot.players.getEntry(playerString);
        if (player == null) player = context.sender;

        TrustLevel trustLevel = context.getEnum(false, TrustLevel.class);
        if (trustLevel == null) trustLevel = context.trustLevel;

        if (trustLevel.level > context.trustLevel.level) throw new CommandException(
                Component.translatable(
                        "commands.auth.error.privilege_escalate"
                )
        );

        player.authenticatedTrustLevel = trustLevel;

        return Component.translatable(
                "commands.auth.output",
                bot.colorPalette.defaultColor,
                Component.text(player.profile.getName(), bot.colorPalette.username),
                player.authenticatedTrustLevel.component
        );
    }
}
