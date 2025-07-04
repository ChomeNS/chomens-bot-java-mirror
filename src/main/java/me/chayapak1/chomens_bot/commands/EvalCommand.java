package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.eval.EvalOutput;
import me.chayapak1.chomens_bot.plugins.EvalPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.CompletableFuture;

public class EvalCommand extends Command {
    public EvalCommand () {
        super(
                "eval",
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

        if (!EvalPlugin.connected) throw new CommandException(Component.translatable("commands.eval.error.offline"));

        final String action = context.getAction();

        switch (action) {
            case "run" -> {
                final String command = context.getString(true, true);

                final CompletableFuture<EvalOutput> future = bot.eval.run(command);

                // it returns null when the eval server isn't online, even though we have already checked,
                // i'm just fixing the warning here
                if (future == null) return null;

                future.thenApply(result -> {
                    if (result.isError()) context.sendOutput(Component.text(result.output(), NamedTextColor.RED));
                    else context.sendOutput(Component.text(result.output()));

                    return result;
                });
            }
            case "reset" -> {
                EvalPlugin.reset();
                return Component.translatable("commands.eval.reset", bot.colorPalette.defaultColor);
            }
            default -> throw new CommandException(Component.translatable("commands.generic.error.invalid_action"));
        }

        return null;
    }
}
