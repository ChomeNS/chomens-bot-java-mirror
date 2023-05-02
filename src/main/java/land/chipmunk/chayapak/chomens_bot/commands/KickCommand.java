package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KickCommand implements Command {
    public String name() { return "kick"; }

    public String description() {
        return "Kicks a player using the complex NBT exploit (from KittyCorpBot)";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{player}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

        return aliases;
    }

    public int trustLevel() {
        return 1;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        final MutablePlayerListEntry entry = bot.players().getEntry(String.join(" ", args));

        if (entry == null) return Component.text("Invalid player name").color(NamedTextColor.RED);

        final String name = entry.profile().getName();
        final UUID uuid = entry.profile().getId();

        bot.exploits().kick(uuid);

        return Component.empty()
                .append(Component.text("Kicking player "))
                .append(Component.text(name).color(NamedTextColor.GOLD));
    }
}
