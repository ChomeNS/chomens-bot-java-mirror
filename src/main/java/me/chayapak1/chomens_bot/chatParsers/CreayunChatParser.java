package me.chayapak1.chomens_bot.chatParsers;

import me.chayapak1.chomens_bot.util.UUIDUtilities;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.chat.ChatParser;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreayunChatParser implements ChatParser {
    private final Bot bot;

    // is parsing creayun chat using regex a good idea?
    // mabe mabe mabe
    // it doesn't use like translation or anything
    private static final Pattern PATTERN = Pattern.compile("([^ ]*) » (.*)");

    public CreayunChatParser (Bot bot) {
        this.bot = bot;
    }

    @Override
    public PlayerMessage parse (Component message) {
        final String stringified = ComponentUtilities.stringify(message);

        if (!bot.options.creayun) return null;

        final Matcher matcher = PATTERN.matcher(stringified);

        if (matcher.find()) {
            final String displayName = matcher.group(1);
            final String contents = matcher.group(2);

            PlayerEntry sender = bot.players.getEntry(displayName);
            if (sender == null) sender = new PlayerEntry(new GameProfile(UUIDUtilities.getOfflineUUID(displayName), displayName), GameMode.SURVIVAL, 0, Component.text(displayName), 0L, null, new byte[0], true);

            return new PlayerMessage(sender, Component.text(displayName), Component.text(contents));
        }

        return null;
    }
}
