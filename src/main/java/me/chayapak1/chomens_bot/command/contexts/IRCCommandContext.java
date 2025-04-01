package me.chayapak1.chomens_bot.command.contexts;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;

public class IRCCommandContext extends CommandContext {
    private final Bot bot;
    private final String nickName;

    public IRCCommandContext (Bot bot, String prefix, String nickName) {
        super(
                bot,
                prefix,
                new PlayerEntry(
                        new GameProfile(
                                UUIDUtilities.getOfflineUUID(nickName),
                                nickName
                        ),
                        GameMode.SURVIVAL,
                        -69420,
                        Component.text(nickName),
                        0L,
                        null,
                        new byte[0],
                        true
                ),
                false
        );
        this.bot = bot;
        this.nickName = nickName;
    }

    @Override
    public void sendOutput (Component component) {
        bot.irc.sendMessage(bot, ComponentUtilities.stringifyAnsi(component));
    }

    @Override
    public Component displayName () {
        return Component.text(nickName);
    }
}
