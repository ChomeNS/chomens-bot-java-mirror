package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class ListCommand extends Command {
    public ListCommand () {
        super(
                "list",
                "Lists all players in the server (including vanished)",
                new String[] {},
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        final List<MutablePlayerListEntry> list = bot.players().list();

        final List<Component> playersComponent = new ArrayList<>();

        for (MutablePlayerListEntry entry : list) {
            playersComponent.add(
                    Component.translatable(
                            "%s › %s",
                            entry.displayName() == null ?
                                    Component.text(entry.profile().getName()).color(ColorUtilities.getColorByString(bot.config().colorPalette().username())) :
                                    entry.displayName()
                                    .hoverEvent(
                                            HoverEvent.showText(
                                                    Component
                                                            .text(entry.profile().getName())
                                                            .append(Component.newline())
                                                            .append(Component.text("Click here to copy the username to your clipboard").color(NamedTextColor.GREEN))
                                            )
                                    )
                                    .clickEvent(
                                            ClickEvent.copyToClipboard(entry.profile().getName())
                                    )
                                    .color(NamedTextColor.WHITE),
                            Component
                                    .text(entry.profile().getIdAsString())
                                    .hoverEvent(
                                            HoverEvent.showText(
                                                    Component.text("Click here to copy the UUID to your clipboard").color(NamedTextColor.GREEN)
                                            )
                                    )
                                    .clickEvent(
                                            ClickEvent.copyToClipboard(entry.profile().getIdAsString())
                                    )
                                    .color(ColorUtilities.getColorByString(bot.config().colorPalette().uuid()))
                    ).color(NamedTextColor.DARK_GRAY)
            );
        }

        return Component.empty()
                .append(Component.text("Players ").color(NamedTextColor.GREEN))
                .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(list.size()).color(NamedTextColor.GRAY))
                .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(
                        Component.join(JoinConfiguration.newlines(), playersComponent)
                );
    }
}
