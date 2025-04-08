package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class BruhifyCommand extends Command {
    public BruhifyCommand () {
        super(
                "bruhify",
                "RecycleBots bruhify but actionbar",
                new String[] { "[message]" },
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        bot.bruhify.bruhifyText = context.getString(true, false);

        return null;
    }
}
