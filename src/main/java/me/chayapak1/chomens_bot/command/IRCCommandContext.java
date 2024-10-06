package me.chayapak1.chomens_bot.command;

import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class IRCCommandContext extends CommandContext {
    private final Bot bot;
    private final String nickName;

    public IRCCommandContext (Bot bot, String prefix, String nickName) {
        super(
                bot,
                prefix,
                new PlayerEntry(
                        new GameProfile(
                                UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickName).getBytes()),
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
        bot.irc.sendMessage(bot, ComponentUtilities.stringify(component));
    }

    @Override
    public Component displayName () {
        return Component.text(nickName);
    }
}
