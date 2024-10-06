package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class EchoCommand extends Command {
    public EchoCommand () {
        super(
                "echo",
                "Makes the bot say a message",
                new String[] { "<message>" },
                new String[] { "say" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        bot.chat.send(context.getString(true, true));

        return null;
    }
}
