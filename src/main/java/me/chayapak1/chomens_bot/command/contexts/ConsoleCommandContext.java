package me.chayapak1.chomens_bot.command.contexts;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.I18nUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;

public class ConsoleCommandContext extends CommandContext {
    private final Bot bot;

    public ConsoleCommandContext (final Bot bot, final String prefix) {
        super(
                bot,
                prefix,
                bot.players.getBotEntry() != null ?
                        bot.players.getBotEntry() :
                        new PlayerEntry(
                                new GameProfile(
                                        UUIDUtilities.getOfflineUUID(bot.username),
                                        bot.username
                                ),
                                GameMode.CREATIVE,
                                -69420,
                                Component.text(bot.username),
                                0,
                                null,
                                new byte[0],
                                true
                        ),
                false
        );
        this.bot = bot;
    }

    @Override
    public void sendOutput (final Component component) {
        final Component rendered = I18nUtilities.render(component);
        bot.logger.log(LogType.COMMAND_OUTPUT, rendered, false);
    }

    @Override
    public Component displayName () {
        return sender.displayName.color(NamedTextColor.YELLOW);
    }
}
