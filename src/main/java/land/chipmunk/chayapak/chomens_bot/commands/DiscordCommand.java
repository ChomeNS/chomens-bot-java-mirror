package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class DiscordCommand implements Command {
    public String name() { return "discord"; }

    public String description() {
        return "Shows the Discord invite";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("");

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
        final String link = "https://discord.gg/xdgCkUyaA4";
        return Component.empty()
                .append(Component.text("The Discord invite is ").color(NamedTextColor.WHITE))
                .append(
                        Component
                                .text(link)
                                .clickEvent(ClickEvent.openUrl(link))
                                .color(NamedTextColor.BLUE)
                );
    }
}
