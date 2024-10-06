package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;

public class CommandBlockCommand extends Command {
    public CommandBlockCommand () {
        super(
                "cb",
                "Executes a command in the command core and return its output",
                new String[] { "<command>" },
                new String[] { "cmd", "commandblock", "run", "core" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final CompletableFuture<Component> future = bot.core.runTracked(context.getString(true, true));

        if (future == null) return null;

        future.thenApply(output -> {
            context.sendOutput(output);

            return output;
        });

        return null;
    }
}
