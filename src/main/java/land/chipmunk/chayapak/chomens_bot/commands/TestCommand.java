package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TestCommand extends Command {
    public TestCommand () {
        super(
                "test",
                "Tests if the bot is working",
                new String[] { "[{args}]" },
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        return Component.translatable(
                "Hello, World! Username: %s, Sender UUID: %s, Prefix: %s, Args: %s",
                Component.text(context.sender().profile().getName()),
                Component.text(context.sender().profile().getIdAsString()),
                Component.text(context.prefix()),
                Component.text(String.join(", ", args))
        ).color(NamedTextColor.GREEN);
    }
}
