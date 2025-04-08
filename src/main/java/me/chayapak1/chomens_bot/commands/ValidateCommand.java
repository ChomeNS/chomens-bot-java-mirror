package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.command.contexts.ConsoleCommandContext;
import me.chayapak1.chomens_bot.command.contexts.DiscordCommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ValidateCommand extends Command {
    public ValidateCommand () {
        super(
                "validate",
                "Validates/shows your trust level",
                new String[] { "" },
                new String[] { "checkhash" },
                TrustLevel.TRUSTED
        );
    }

    @Override
    public Component execute (CommandContext context) {
        // <red>Trusted - 1
        // <dark_red>Admin - 2
        // .......
        final Component trustLevelComponent = context.trustLevel.component
                .append(Component.text(" - "))
                .append(Component.text(context.trustLevel.level));

        if (context instanceof DiscordCommandContext) return Component
                .translatable("You are trusted! (%s)")
                .arguments(trustLevelComponent)
                .color(NamedTextColor.GREEN);
        else if (context instanceof ConsoleCommandContext) return Component
                .text("You are the console! You have no trust level")
                .color(NamedTextColor.GREEN);
        else return Component
                    .translatable("Valid hash (%s)")
                    .arguments(trustLevelComponent)
                    .color(NamedTextColor.GREEN);
    }
}
