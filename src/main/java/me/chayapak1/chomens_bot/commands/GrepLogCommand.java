package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GrepLogCommand extends Command {
    private Thread thread;

    public GrepLogCommand () {
        super(
                "greplog",
                "Queries the bot's logs",
                new String[] { "<input>", "...-ignorecase...", "...-regex...", "stop" },
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

        boolean ignoreCase = false;
        boolean regex = false;

        String firstInput = context.getString(false, true);

        // run 2 times. for example `*greplog -ignorecase -regex test` will be both accepted
        for (int i = 0; i < 2; i++) {
            if (firstInput.equals("-ignorecase")) {
                ignoreCase = true;
                firstInput = context.getString(false, true);
            } else if (firstInput.equals("-regex")) {
                regex = true;
                firstInput = context.getString(false, true);
            }
        }

        // interesting code
        final String input = (firstInput + " " + context.getString(true, false)).trim();

        if (input.equals("stop")) {
            if (thread == null) throw new CommandException(Component.text("There is no query process running"));

            bot.grepLog.running = false;
            bot.grepLog.pattern = null;

            thread.interrupt(); // ? should i interrupt it this way?

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

        final boolean finalIgnoreCase = ignoreCase;
        final boolean finalRegex = regex;

        thread = new Thread(() -> {
            try {
                bot.grepLog.search(context, input, finalIgnoreCase, finalRegex);
            } catch (final CommandException e) {
                context.sendOutput(e.message.color(NamedTextColor.RED));
            }

            thread = null;
        });

        thread.start();

        return null;
    }
}
