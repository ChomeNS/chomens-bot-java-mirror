package land.chipmunk.chayapak.chomens_bot.commands;

import com.github.ricksbrown.cowsay.plugin.CowExecutor;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class CowsayCommand extends Command {
    public CowsayCommand () {
        super(
                "cowsay",
                "Moo",
                new String[] { "<message>" },
                new String[] {},
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final String message = context.getString(true, true);

        final CowExecutor cowExecutor = new CowExecutor();
        cowExecutor.setMessage(message);

        final String result = cowExecutor.execute();

        return Component.text(result);
    }
}
