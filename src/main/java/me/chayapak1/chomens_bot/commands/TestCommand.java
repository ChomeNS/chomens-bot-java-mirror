package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TestCommand extends Command {
    public TestCommand () {
        super(
                "test",
                "Tests if the bot is working",
                new String[] { "[args]" },
                new String[] {},
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute (CommandContext context) throws CommandException {
        return Component.translatable(
                "Hello, World! Username: %s, Sender UUID: %s, Prefix: %s, Args: %s",
                Component.text(context.sender.profile.getName()),
                Component.text(context.sender.profile.getIdAsString()),
                Component.text(context.prefix),
                Component.text(context.getString(true, false))
        ).color(NamedTextColor.GREEN);
    }
}
