package me.chayapak1.chomens_bot.command.contexts;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class RemoteCommandContext extends CommandContext {
    public final Bot targetBot;
    public final CommandContext source;

    public RemoteCommandContext (final Bot targetBot, final CommandContext source) {
        super(targetBot, source.prefix, source.sender, false);

        this.targetBot = targetBot;
        this.source = source;
    }

    @Override
    public void sendOutput (final Component component) {
        // we do not need rendering here, since that is handled by the source
        source.sendOutput(
                Component
                        .translatable(
                                "[%s] %s",
                                Component.text(targetBot.getServerString(), NamedTextColor.GRAY),
                                Component.empty()
                                        .color(NamedTextColor.WHITE)
                                        .append(component)
                        )
                        .color(NamedTextColor.DARK_GRAY)
        );
    }
}
