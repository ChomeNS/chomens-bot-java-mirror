package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class ClearChatQueueCommand extends Command {
    public ClearChatQueueCommand () {
        super(
                "clearchatqueue",
                new String[] {},
                new String[] { "ccq" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(0);

        final Bot bot = context.bot;

        bot.chat.clearQueue();

        return Component.translatable(
                "commands.clearchatqueue.output",
                bot.colorPalette.defaultColor
        );
    }
}
