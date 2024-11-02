package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class ConsoleCommand extends Command {
    public ConsoleCommand() {
        super(
                "console",
                "Controls stuff about console",
                new String[] {
                        "server <server>",
                        "logtoconsole <true|false>"
                },
                new String[] {},
                TrustLevel.OWNER,
                true
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getString(false, true, true);

        switch (action) {
            case "server" -> {
                final List<String> servers = new ArrayList<>();

                for (Bot eachBot : bot.bots) {
                    servers.add(eachBot.host + ":" + eachBot.port);
                }

                final String server = context.getString(true, true);

                for (Bot eachBot : bot.bots) {
                    if (server.equalsIgnoreCase("all")) {
                        eachBot.console.consoleServer = "all";

                        context.sendOutput(Component.text("Set the console server to all servers").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)));

                        continue;
                    }

                    try {
                        // servers.find(server => server.toLowerCase().includes(args.join(' '))) in js i guess
                        eachBot.console.consoleServer = servers.stream()
                                .filter(eachServer -> eachServer.toLowerCase().contains(server))
                                .findFirst()
                                .orElse("all");

                        context.sendOutput(Component.text("Set the console server to " + bot.console.consoleServer).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new CommandException(Component.text("Invalid server: " + server));
                    }
                }
            }
            case "logtoconsole" -> {
                context.checkOverloadArgs(2);

                final boolean bool = context.getBoolean(true);

                bot.logger.logToConsole = bool;

                return Component.translatable(
                        "Logging to console is now %s",
                        bool ? Component.text("enabled").color(NamedTextColor.GREEN) : Component.text("disabled").color(NamedTextColor.RED)
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
        }

        return null;
    }
}
