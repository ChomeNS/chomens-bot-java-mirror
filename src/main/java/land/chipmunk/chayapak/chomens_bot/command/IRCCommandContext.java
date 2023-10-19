package land.chipmunk.chayapak.chomens_bot.command;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
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
