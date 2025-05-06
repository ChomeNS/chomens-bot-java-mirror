package me.chayapak1.chomens_bot.commands;

import com.github.ricksbrown.cowsay.plugin.CowExecutor;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class CowsayCommand extends Command {
    public CowsayCommand () {
        super(
                "cowsay",
                new String[] { "<message>" },
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final String message = context.getString(true, true);

        final CowExecutor cowExecutor = new CowExecutor();
        cowExecutor.setMessage(message);

        final String result = cowExecutor.execute();

        return Component.text(result);
    }
}
