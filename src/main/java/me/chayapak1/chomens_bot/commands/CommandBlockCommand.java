package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// based off of HBot's supercb command
public class CommandBlockCommand extends Command {
    private static final Pattern USER_PATTERN = Pattern.compile("\\{username\\{(.*?)}}");
    private static final Pattern UUID_PATTERN = Pattern.compile("\\{uuid\\{(.*?)}}");

    public CommandBlockCommand () {
        super(
                "cb",
                "Executes a command in the command core and return its output",
                new String[] {
                        "<command>",
                        "..{username}..",
                        "..{uuid}..",
                        "..{username{regex}}..",
                        "..{uuid{regex}}.."
                },
                new String[] { "cmd", "commandblock", "run", "core" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        runCommand(bot, context, context.getString(true, true));

        return null;
    }

    private void runCommand (Bot bot, CommandContext context, String command) {
        final Matcher userMatcher = USER_PATTERN.matcher(command);
        final Matcher uuidMatcher = UUID_PATTERN.matcher(command);

        final boolean userFound = userMatcher.find();
        final boolean uuidFound = uuidMatcher.find();

        if (userFound || uuidFound) {
            Pattern pattern;

            if (userFound) pattern = Pattern.compile(userMatcher.group(1));
            else pattern = Pattern.compile(uuidMatcher.group(1));

            for (PlayerEntry entry : bot.players.list) {
                final String username = entry.profile.getName();
                final String uuid = entry.profile.getIdAsString();

                if (!pattern.matcher(userFound ? username : uuid).matches()) continue;

                String replacedCommand;

                if (userFound) replacedCommand = new StringBuilder(command).replace(userMatcher.start(), userMatcher.end(), username).toString();
                else replacedCommand = new StringBuilder(command).replace(uuidMatcher.start(), uuidMatcher.end(), uuid).toString();

                replacedCommand = replacedCommand
                        .replace("{username}", username)
                        .replace("{uuid}", uuid);

                if (
                        !replacedCommand.contains("{username}") &&
                                !replacedCommand.contains("{uuid}") &&
                                !USER_PATTERN.matcher(username).find() &&
                                !UUID_PATTERN.matcher(username).find()
                ) {
                    runCommand(bot, context, replacedCommand);
                }
            }
        } else if (command.contains("{username}") || command.contains("{uuid}")) {
            for (PlayerEntry entry : bot.players.list) {
                final String username = entry.profile.getName();
                final String uuid = entry.profile.getIdAsString();

                final String replacedCommand = command
                        .replace("{username}", username)
                        .replace("{uuid}", uuid);

                if (
                        !replacedCommand.contains("{username}") &&
                                !replacedCommand.contains("{Uuuid}") &&
                                !USER_PATTERN.matcher(username).find() &&
                                !UUID_PATTERN.matcher(username).find()
                ) {
                    runCommand(bot, context, replacedCommand);
                }
            }
        } else {
            final CompletableFuture<Component> future = bot.core.runTracked(command);

            if (future == null) return;

            future.thenApplyAsync(output -> {
                context.sendOutput(output);

                return output;
            });
        }
    }
}
