package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class UUIDCommand implements Command {
    public String name() { return "uuid"; }

    public String description() {
        return "Shows your UUID or other player's UUID";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("[{username}]");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

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

            final String name = entry.profile().getName();
            final String uuid = entry.profile().getIdAsString();

            return Component.translatable(
                    "%s's UUID: %s",
                    Component.text(name),
                    Component
                            .text(uuid)
                            .hoverEvent(
                                    HoverEvent.showText(
                                            Component.text("Click here to copy the UUID to your clipboard").color(NamedTextColor.GREEN)
                                    )
                            )
                            .clickEvent(
                                    ClickEvent.copyToClipboard(uuid)
                            )
                            .color(NamedTextColor.AQUA)
            ).color(NamedTextColor.GREEN);
        } else {
            final MutablePlayerListEntry entry = context.sender();

            final String uuid = entry.profile().getIdAsString();

            return Component.translatable(
                    "Your UUID: %s",
                    Component
                            .text(uuid)
                            .hoverEvent(
                                    HoverEvent.showText(
                                            Component.text("Click here to copy the UUID to your clipboard").color(NamedTextColor.GREEN)
                                    )
                            )
                            .clickEvent(
                                    ClickEvent.copyToClipboard(uuid)
                            )
                            .color(NamedTextColor.AQUA)
            ).color(NamedTextColor.GREEN);
        }
    }
}
