package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class GrepLogCommand extends Command {
    private Thread thread;

    public GrepLogCommand () {
        super(
                "greplog",
                "Queries the bot's logs",
                new String[] { "<input>", "-ignorecase ...", "-regex ...", "stop" },
                new String[] { "logquery", "findlog" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        if (Main.discord == null || Main.discord.jda == null) {
            throw new CommandException(Component.text("The bot's Discord integration has to be enabled to use this command."));
        }

        final List<String> flags = context.getFlags(true, CommonFlags.IGNORE_CASE, CommonFlags.REGEX);

        final boolean ignoreCase = flags.contains(CommonFlags.IGNORE_CASE);
        final boolean regex = flags.contains(CommonFlags.REGEX);

        final String input = context.getString(true, true);

        if (input.equalsIgnoreCase("stop")) {
            if (thread == null) throw new CommandException(Component.text("There is no query process running"));

            bot.grepLog.running = false;
            bot.grepLog.pattern = null;

            thread = null;

            return Component.text("Stopped querying the logs").color(bot.colorPalette.defaultColor);
        }

        if (thread != null) throw new CommandException(Component.text("Another query is already running"));

        context.sendOutput(
                Component
                        .translatable("Started querying the logs for %s")
                        .color(bot.colorPalette.defaultColor)
                        .arguments(
                                Component
                                        .text(input)
                                        .color(bot.colorPalette.string)
                        )
        );

        thread = new Thread(() -> {
            try {
                bot.grepLog.search(context, input, ignoreCase, regex);
            } catch (final CommandException e) {
                context.sendOutput(e.message.color(NamedTextColor.RED));
            }

            thread = null;
        });

        thread.start();

        return null;
    }
}
