package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
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
                        "<ownerHash> server <server>",
                        "<ownerHash> logtoconsole <true|false>"
                },
                new String[] {},
                TrustLevel.OWNER,
                true
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getString(false, true);

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
                                .toArray(String[]::new)[0];

                        context.sendOutput(Component.text("Set the console server to " + bot.console.consoleServer).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new CommandException(Component.text("Invalid server: " + server));
                    }
                }
            }
            case "logtoconsole" -> {
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
