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
                "Kicks a player",
                new String[] {},
                new String[] {},
                TrustLevel.TRUSTED
        );

        final String allMethods = String.join("|", Arrays.stream(Kick.values()).map(Enum::name).toList());

        this.usages = new String[] { "<" + allMethods + "> <player>" };
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final Kick method = context.getEnum(Kick.class);

        final PlayerEntry entry = bot.players.getEntry(context.getString(true, true));

        if (entry == null) throw new CommandException(Component.text("Invalid player name"));

        final String name = entry.profile.getName();
        final UUID uuid = entry.profile.getId();

        bot.exploits.kick(method, uuid);

        return Component.empty()
                .append(Component.text("Kicking player"))
                .append(Component.space())
                .append(Component.text(name).color(bot.colorPalette.username))
                .append(Component.space())
                .append(Component.text("with method"))
                .append(Component.space())
                .append(Component.text(method.name()).color(bot.colorPalette.string))
                .color(bot.colorPalette.defaultColor);
    }
}
