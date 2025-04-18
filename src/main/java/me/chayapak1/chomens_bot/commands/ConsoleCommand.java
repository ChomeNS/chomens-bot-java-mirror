package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class ConsoleCommand extends Command {
    public ConsoleCommand () {
        super(
                "console",
                "Controls stuff about console",
                new String[] {
                        "server <server>",
                        "logtoconsole <true|false>",
                        "printdisconnectedreason <true|false>"
                },
                new String[] { "csvr" },
                TrustLevel.OWNER,
                true
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = !context.userInputCommandName.equals(this.name)
                ? "server" // csvr alias (console server), like in the original chomens bot javascript
                : context.getString(false, true, true);

        switch (action) {
            case "server" -> {
                final List<String> servers = new ArrayList<>();

                for (final Bot eachBot : bot.bots) {
                    servers.add(eachBot.getServerString(true));
                }

                final String server = context.getString(true, true);

                if (server.equalsIgnoreCase("all")) {
                    Main.console.consoleServer = "all";
                    return Component.text(
                            "Set the console server to all servers"
                    ).color(bot.colorPalette.defaultColor);
                }

                try {
                    // servers.find(server => server.toLowerCase().includes(args.join(' '))) in js i guess
                    Main.console.consoleServer = servers.stream()
                            .filter(eachServer -> eachServer.toLowerCase().contains(server))
                            .findFirst()
                            .orElse("all");

                    return Component.translatable(
                            "Set the console server to %s",
                            Component.text(Main.console.consoleServer)
                    ).color(bot.colorPalette.defaultColor);
                } catch (final ArrayIndexOutOfBoundsException e) {
                    throw new CommandException(Component.text("Invalid server: " + server));
                }
            }
            case "logtoconsole" -> {
                context.checkOverloadArgs(2);

                final boolean bool = context.getBoolean(true);

                bot.logger.logToConsole = bool;

                return Component.translatable(
                        "Logging to console is now %s",
                        bool ? Component.text("enabled").color(NamedTextColor.GREEN) : Component.text("disabled").color(NamedTextColor.RED)
                ).color(bot.colorPalette.defaultColor);
            }
            case "printdisconnectedreason" -> {
                context.checkOverloadArgs(2);

                final boolean bool = context.getBoolean(true);

                bot.printDisconnectedCause = bool;

                return Component.translatable(
                        "Printing the disconnected cause is now %s",
                        bool ? Component.text("enabled").color(NamedTextColor.GREEN) : Component.text("disabled").color(NamedTextColor.RED)
                ).color(bot.colorPalette.defaultColor);
            }
        }

        return null;
    }
}
