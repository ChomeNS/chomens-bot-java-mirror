package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.EvalOutput;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.CompletableFuture;

public class EvalCommand extends Command {
    public EvalCommand () {
        super(
                "eval",
                "Evaluate JavaScript codes",
                new String[] { "run <code>", "reset" },
                new String[] {},
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        if (!bot.eval.connected) throw new CommandException(Component.text("Eval server is not online"));

        final String action = context.getString(false, true);

        switch (action) {
            case "run" -> {
                final String command = context.getString(true, true);

                final CompletableFuture<EvalOutput> future = bot.eval.run(command);

                future.thenApply((output) -> {
                    if (output.isError()) context.sendOutput(Component.text(output.output()).color(NamedTextColor.RED));
                    else context.sendOutput(Component.text(output.output()));

                    return output;
                });
            }
            case "reset" -> {
                bot.eval.reset();

                return Component.text("Reset the eval worker").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            default -> {
                throw new CommandException(Component.text("Invalid action"));
            }
        }

        return null;
    }
}
