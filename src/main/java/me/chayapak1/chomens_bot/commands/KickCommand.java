package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.exploitMethods.Kick;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.UUID;

public class KickCommand extends Command {
    public KickCommand () {
        super(
                "kick",
                new String[] {},
                new String[] {},
                TrustLevel.TRUSTED
        );

        final String allMethods = String.join(" | ", Arrays.stream(Kick.values()).map(Enum::name).toList());

        this.usages = new String[] { "<" + allMethods + "> <player>" };
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final Kick method = context.getEnum(true, Kick.class);

        final PlayerEntry entry = bot.players.getEntry(context.getString(true, true));

        if (entry == null) throw new CommandException(Component.translatable("commands.generic.error.invalid_player"));

        final String name = entry.profile.getName();
        final UUID uuid = entry.profile.getId();

        bot.exploits.kick(method, uuid);

        return Component.translatable(
                "commands.kick.output",
                bot.colorPalette.defaultColor,
                Component.text(name, bot.colorPalette.username),
                Component.text(method.toString(), bot.colorPalette.string)
        );
    }
}
