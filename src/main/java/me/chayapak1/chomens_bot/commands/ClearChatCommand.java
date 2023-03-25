package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClearChatCommand implements Command {
    public String name() { return "clearchat"; }

    public String description() {
        return "Clears the chat";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("[player]");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("cc");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        if (args.length > 0) {
            final MutablePlayerListEntry entry = bot.players().getEntry(String.join(" ", args));

            if (entry == null) return Component.text("Invalid player name").color(NamedTextColor.RED);

            final UUID uuid = entry.profile().getId();

            bot.chat().tellraw(
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
            bot.chat().tellraw(
                    Component.empty()
                            .append(Component.text("\n".repeat(1000)))
                            .append(Component.text("The chat has been cleared").color(NamedTextColor.DARK_GREEN))
            );
        }

        return Component.text("success");
    }
}
