package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class ClearChatCommand extends Command {
    public ClearChatCommand () {
        super(
                "clearchat",
                "Clears the chat",
                new String[] { "[player]" },
                new String[] { "cc" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String name = context.getString(true, false);

        if (!name.isEmpty()) {
            final PlayerEntry entry = bot.players.getEntry(name);

            if (entry == null) throw new CommandException(Component.text("Invalid player name"));

            final UUID uuid = entry.profile.getId();

            bot.chat.tellraw(
                    Component.empty()
                            .append(Component.text("\n".repeat(1000)))
                            .append(
                                    Component.empty()
                                            .append(Component.text("Your chat has been cleared by "))
                                            .append(context.displayName())
                                            .color(NamedTextColor.DARK_GREEN)
                            ),
                    uuid
            );
        } else {
            bot.chat.tellraw(
                    Component.empty()
                            .append(Component.text("\n".repeat(1000)))
                            .append(Component.text("The chat has been cleared").color(NamedTextColor.DARK_GREEN))
            );
        }

        return null;
    }
}
