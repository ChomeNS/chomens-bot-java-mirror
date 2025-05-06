package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.command.contexts.RemoteCommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class NetCommandCommand extends Command {
    public NetCommandCommand () {
        super(
                "netcmd",
                new String[] { "<servers separated by a comma> <command>" },
                new String[] { "networkcommand", "irccommand", "remotecommand" },
                TrustLevel.TRUSTED
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final String rawServers = context.getString(false, true, true);

        final List<Bot> allBots = context.bot.bots;

        final List<Bot> bots;

        if (rawServers.equals("all")) {
            bots = allBots;
        } else {
            bots = new ArrayList<>();

            final String[] servers = rawServers.split(",");

            for (final Bot bot : allBots) {
                for (final String server : servers) {
                    if (
                            server.isBlank()
                                    || !bot.getServerString(true)
                                            .toLowerCase()
                                            .trim()
                                            .contains(server.toLowerCase())
                    ) continue;

                    bots.add(bot);
                }
            }
        }

        if (bots.isEmpty()) throw new CommandException(Component.translatable("commands.netcmd.error.no_servers_found"));

        final String command = context.getString(true, true);

        for (final Bot bot : bots) {
            if (!bot.loggedIn) continue;

            final CommandContext remoteContext = new RemoteCommandContext(bot, context);

            context.bot.commandHandler.executeCommand(command, remoteContext);
        }

        return null;
    }
}
