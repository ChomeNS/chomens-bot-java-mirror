package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class ConsoleServerCommand extends Command {
    public ConsoleServerCommand () {
        super(
                "consoleserver",
                "Changes the console server",
                new String[] { "<ownerHash> <{server}>" },
                new String[] { "csvr" },
                TrustLevel.OWNER,
                true
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        final List<String> servers = new ArrayList<>();

        for (Bot eachBot : bot.bots) {
            servers.add(eachBot.host + ":" + eachBot.port);
        }

        for (Bot eachBot : bot.bots) {
            if (String.join(" ", args).equalsIgnoreCase("all")) {
                eachBot.console.consoleServer = "all";

                context.sendOutput(Component.text("Set the console server to all servers").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)));

                continue;
            }

            try {
                // servers.find(server => server.toLowerCase().includes(args.join(' '))) in js i guess
                eachBot.console.consoleServer = servers.stream()
                        .filter(server -> server.toLowerCase().contains(String.join(" ", args)))
                        .toArray(String[]::new)[0];

                context.sendOutput(Component.text("Set the console server to " + String.join(", ", bot.console.consoleServer)).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)));
            } catch (ArrayIndexOutOfBoundsException e) {
                return Component.text("Invalid server: " + String.join(" ", args)).color(NamedTextColor.RED);
            }
        }

        return null;
    }
}
