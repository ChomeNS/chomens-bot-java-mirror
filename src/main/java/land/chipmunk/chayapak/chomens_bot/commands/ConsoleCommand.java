package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConsoleCommand extends Command {
    public ConsoleCommand() {
        super(
                "console",
                "Controls stuff about console",
                new String[] {
                        "<ownerHash> server <{server}>",
                        "<ownerHash> logtoconsole <true|false>"
                },
                new String[] {},
                TrustLevel.OWNER,
                true
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        if (args.length < 2) return Component.text("Not enough arguments").color(NamedTextColor.RED);

        switch (args[0]) {
            case "server" -> {
                final List<String> servers = new ArrayList<>();

                for (Bot eachBot : bot.bots) {
                    servers.add(eachBot.host + ":" + eachBot.port);
                }

                for (Bot eachBot : bot.bots) {
                    final String server = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

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
                        return Component.text("Invalid server: " + String.join(" ", args)).color(NamedTextColor.RED);
                    }
                }
            }
            case "logtoconsole" -> {
                final boolean bool = Boolean.parseBoolean(args[1]);

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
