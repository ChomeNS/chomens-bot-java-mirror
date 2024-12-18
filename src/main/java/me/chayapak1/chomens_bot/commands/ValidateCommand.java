package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.command.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ValidateCommand extends Command {
    public ValidateCommand () {
        super(
                "validate",
                "Validates/shows your trust level",
                new String[] { "" },
                new String[] { "checkhash" },
                TrustLevel.TRUSTED,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        if (context instanceof DiscordCommandContext) return Component
                .translatable("You are trusted! (%s)")
                .arguments(Component.text(context.trustLevel.name()))
                .color(NamedTextColor.GREEN);
        else if (context instanceof ConsoleCommandContext) return Component
                .text("You are the console! You have no trust level")
                .color(NamedTextColor.GREEN);
        else return Component
                .translatable("Valid hash (%s)")
                .arguments(Component.text(context.trustLevel.name()))
                .color(NamedTextColor.GREEN);
    }
}
