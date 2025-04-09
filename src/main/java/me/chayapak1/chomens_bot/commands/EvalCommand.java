package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.eval.EvalOutput;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.CompletableFuture;

public class EvalCommand extends Command {
    public EvalCommand () {
        super(
                "eval",
                "Evaluate JavaScript codes on a Node.JS container running @n8n/vm2",
                new String[] { "run <code>", "reset" },
                new String[] {},
                TrustLevel.PUBLIC,
                false,
                new ChatPacketType[]{ ChatPacketType.DISGUISED }
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        if (!bot.eval.connected) throw new CommandException(Component.text("Eval server is not online"));

        final String action = context.getAction();

        switch (action) {
            case "run" -> {
                final String command = context.getString(true, true);

                final CompletableFuture<EvalOutput> future = bot.eval.run(command);

                future.thenApply(output -> {
                    if (output.isError()) context.sendOutput(Component.text(output.output()).color(NamedTextColor.RED));
                    else context.sendOutput(Component.text(output.output()));

                    return output;
                });
            }
            case "reset" -> {
                bot.eval.reset();

                return Component.text("Reset the eval worker").color(bot.colorPalette.defaultColor);
            }
            default -> throw new CommandException(Component.text("Invalid action"));
        }

        return null;
    }
}
