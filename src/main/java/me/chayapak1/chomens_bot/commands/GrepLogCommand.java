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
                new String[] { "<input>", "-ignorecase ...", "-regex ...", "stop" },
                new String[] { "logquery", "findlog" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        if (Main.discord == null || Main.discord.jda == null) {
            throw new CommandException(Component.translatable("commands.generic.error.discord_disabled"));
        }

        final List<String> flags = context.getFlags(true, CommonFlags.IGNORE_CASE, CommonFlags.REGEX);

        final boolean ignoreCase = flags.contains(CommonFlags.IGNORE_CASE);
        final boolean regex = flags.contains(CommonFlags.REGEX);

        final String input = context.getString(true, true);

        if (input.equalsIgnoreCase("stop")) {
            if (thread == null) throw new CommandException(Component.translatable("commands.greplog.error.not_running"));

            bot.grepLog.running = false;
            bot.grepLog.pattern = null;

            thread = null;

            return Component.translatable("commands.greplog.stopped").color(bot.colorPalette.defaultColor);
        }

        if (thread != null) throw new CommandException(Component.translatable("commands.greplog.error.already_running"));

        context.sendOutput(
                Component
                        .translatable("commands.greplog.started", bot.colorPalette.defaultColor)
                        .arguments(
                                Component.text(input, bot.colorPalette.string)
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
