package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

// based off of HBot's supercb command
public class CommandBlockCommand extends Command {
    private static final Pattern USER_PATTERN = Pattern.compile("\\{username\\{(.*?)}}");
    private static final Pattern UUID_PATTERN = Pattern.compile("\\{uuid\\{(.*?)}}");

    public CommandBlockCommand () {
        super(
                "cb",
                "Executes a command in the command core and return its output",
                new String[] {
                        "",
                        "<command>",
                        "..{username}..",
                        "..{uuid}..",
                        "..{username{regex}}..",
                        "..{uuid{regex}}.."
                },
                new String[] { "cmd", "commandblock", "run", "core" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String command = context.getString(true, false);

        if (command.isEmpty()) {
            return getInfo(bot);
        }

        try {
            runCommand(bot, context, command, null);
        } catch (final PatternSyntaxException e) {
            throw new CommandException(Component.text(e.toString()));
        }

        return null;
    }

    private Component getInfo (final Bot bot) {
        final Vector3i from = bot.core.from;
        final Vector3i to = bot.core.to;
        final Vector3i block = bot.core.block;

        final int layers = Math.max(1, to.getY() - from.getY());

        final StringBuilder commandBuilder = new StringBuilder("/");

        if (bot.serverFeatures.hasEssentials) commandBuilder.append("essentials:");
        commandBuilder
                .append("tp ")
                .append(from.getX())
                .append(" ")
                .append(from.getY())
                .append(" ")
                .append(from.getZ());

        final String command = commandBuilder.toString();

        return Component.translatable(
                """
                        Size: %s
                        Layers: %s
                        From: %s
                        To: %s
                        Block: %s
                        Dimension: %s
                        %s""",
                Component
                        .text(256 * layers)
                        .append(Component.text(" blocks"))
                        .color(bot.colorPalette.string),
                Component.text(layers).color(bot.colorPalette.string),
                Component.text(from.toString()).color(bot.colorPalette.string),
                Component.text(to.toString()).color(bot.colorPalette.string),
                Component.text(block.toString()).color(bot.colorPalette.string),
                Component.text(bot.world.currentDimension).color(bot.colorPalette.string),
                Component
                        .text("Click here to teleport to the command core", NamedTextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(Component.text(command, bot.colorPalette.secondary)))
                        .clickEvent(ClickEvent.runCommand(command))
        ).color(bot.colorPalette.secondary);
    }

    private void runCommand (final Bot bot, final CommandContext context, final String command, final PlayerEntry player) {
        final Matcher userMatcher = USER_PATTERN.matcher(command);
        final Matcher uuidMatcher = UUID_PATTERN.matcher(command);

        final boolean userFound = userMatcher.find();
        final boolean uuidFound = uuidMatcher.find();

        if (userFound || uuidFound) {
            final Pattern pattern;

            if (userFound) pattern = Pattern.compile(userMatcher.group(1));
            else pattern = Pattern.compile(uuidMatcher.group(1));

            synchronized (bot.players.list) {
                for (final PlayerEntry entry : bot.players.list) {
                    final String username = entry.profile.getName();
                    final String uuid = entry.profile.getIdAsString();

                    if (!pattern.matcher(userFound ? username : uuid).matches()) continue;

                    String replacedCommand;

                    if (userFound)
                        replacedCommand = new StringBuilder(command).replace(userMatcher.start(), userMatcher.end(), username).toString();
                    else
                        replacedCommand = new StringBuilder(command).replace(uuidMatcher.start(), uuidMatcher.end(), uuid).toString();

                    replacedCommand = replacedCommand
                            .replace("{username}", username)
                            .replace("{uuid}", uuid);

                    if (
                            !replacedCommand.contains("{username}") &&
                                    !replacedCommand.contains("{uuid}") &&
                                    !USER_PATTERN.matcher(username).find() &&
                                    !UUID_PATTERN.matcher(username).find()
                    ) {
                        runCommand(bot, context, replacedCommand, entry);
                    }
                }
            }
        } else if (command.contains("{username}") || command.contains("{uuid}")) {
            synchronized (bot.players.list) {
                for (final PlayerEntry entry : bot.players.list) {
                    final String username = entry.profile.getName();
                    final String uuid = entry.profile.getIdAsString();

                    final String replacedCommand = command
                            .replace("{username}", username)
                            .replace("{uuid}", uuid);

                    if (
                            !replacedCommand.contains("{username}") &&
                                    !replacedCommand.contains("{uuid}") &&
                                    !USER_PATTERN.matcher(username).find() &&
                                    !UUID_PATTERN.matcher(username).find()
                    ) {
                        runCommand(bot, context, replacedCommand, entry);
                    }
                }
            }
        } else {
            final CompletableFuture<Component> future = bot.core.runTracked(command);

            if (future == null) return;

            future.thenApply(output -> {
                if (player == null) context.sendOutput(output);
                else {
                    final Component component = Component
                            .translatable(
                                    "[%s] %s",
                                    Component
                                            .text(player.profile.getName())
                                            .color(NamedTextColor.GRAY)
                                            .hoverEvent(
                                                    HoverEvent.showText(
                                                            Component
                                                                    .text(player.profile.getName())
                                                                    .append(Component.newline())
                                                                    .append(
                                                                            Component
                                                                                    .text(player.profile.getIdAsString())
                                                                                    .color(bot.colorPalette.uuid)
                                                                    )
                                                                    .append(Component.newline())
                                                                    .append(Component.text("Click to copy the username to your clipboard").color(NamedTextColor.GREEN))
                                                                    .append(Component.newline())
                                                                    .append(Component.text("Shift+Click to insert the UUID into your chat box").color(NamedTextColor.GREEN))
                                                    )
                                            )
                                            .clickEvent(ClickEvent.copyToClipboard(player.profile.getName()))
                                            .insertion(player.profile.getIdAsString()),
                                    Component
                                            .empty()
                                            .append(output)
                                            .color(NamedTextColor.WHITE)
                            )
                            .color(NamedTextColor.DARK_GRAY);

                    context.sendOutput(component);
                }

                return output;
            });
        }
    }
}
