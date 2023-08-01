package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.EvalOutput;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class EvalCommand extends Command {
    public EvalCommand () {
        super(
                "eval",
                "Evaluate JavaScript codes",
                new String[] { "run <{code}>", "reset" },
                new String[] {},
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        if (args.length < 1) return Component.text("Not enough arguments").color(NamedTextColor.RED);

        final Bot bot = context.bot;

        if (!bot.eval.connected) return Component.text("Eval server is not online").color(NamedTextColor.RED);

        switch (args[0]) {
            case "run" -> {
                final String command = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                final CompletableFuture<EvalOutput> future = bot.eval.run(command);

                future.thenApply((output) -> {
                    if (output.isError()) context.sendOutput(Component.text(output.output()).color(NamedTextColor.RED));
                    else context.sendOutput(Component.text(output.output()));

                    return output;
                });
            }
            case "reset" -> {
                bot.eval.reset();

                return Component.text("Reset the eval context").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            default -> {
                return Component.text("Invalid action").color(NamedTextColor.RED);
            }
        }

        return null;
    }
}
