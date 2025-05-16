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
                new String[] {
                        "server <server>",
                        "discord <message>",
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
                    return Component.translatable(
                            "commands.console.server.set",
                            bot.colorPalette.defaultColor,
                            Component.translatable("commands.console.server.all_servers")
                    );
                }

                try {
                    // servers.find(server => server.toLowerCase().includes(args.join(' '))) in js i guess
                    Main.console.consoleServer = servers.stream()
                            .filter(eachServer -> eachServer.toLowerCase().contains(server))
                            .findFirst()
                            .orElse("all");

                    return Component.translatable(
                            "commands.console.server.set",
                            bot.colorPalette.defaultColor,
                            Component.text(Main.console.consoleServer)
                    );
                } catch (final ArrayIndexOutOfBoundsException e) {
                    throw new CommandException(
                            Component.translatable(
                                    "commands.console.server.error.invalid_server",
                                    Component.text(server)
                            )
                    );
                }
            }
            case "discord" -> {
                if (Main.discord == null || Main.discord.jda == null) {
                    throw new CommandException(Component.translatable("commands.generic.error.discord_disabled"));
                }

                final String channelId = Main.discord.findChannelId(context.bot.options.discordChannel);

                if (channelId == null) return null;

                final String message = context.getString(true, true);

                Main.discord.sendMessageInstantly(message, channelId, true);

                return null;
            }
            case "logtoconsole" -> {
                context.checkOverloadArgs(2);

                final boolean bool = context.getBoolean(true);

                bot.logger.logToConsole = bool;

                return Component.translatable(
                        "commands.console.logtoconsole.set",
                        bot.colorPalette.defaultColor,
                        bool
                                ? Component.translatable("commands.generic.enabled").color(NamedTextColor.GREEN)
                                : Component.translatable("commands.generic.disabled").color(NamedTextColor.RED)
                );
            }
            case "printdisconnectedreason" -> {
                context.checkOverloadArgs(2);

                final boolean bool = context.getBoolean(true);

                bot.printDisconnectedCause = bool;

                return Component.translatable(
                        "commands.console.printdisconnectedreason.set",
                        bot.colorPalette.defaultColor,
                        bool
                                ? Component.translatable("commands.generic.enabled").color(NamedTextColor.GREEN)
                                : Component.translatable("commands.generic.disabled").color(NamedTextColor.RED)
                );
            }
        }

        return null;
    }
}
