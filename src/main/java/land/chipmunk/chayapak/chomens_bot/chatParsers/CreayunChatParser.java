package land.chipmunk.chayapak.chomens_bot.chatParsers;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.ChatParser;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
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

        if (stringified.length() > 512) return null; // will this fix the fard problem?

        final Matcher matcher = PATTERN.matcher(stringified);

        if (matcher.find()) {
            final String displayName = matcher.group(1);
            final String contents = matcher.group(2);

            MutablePlayerListEntry sender = bot.players().getEntry(displayName);
            if (sender == null) sender = new MutablePlayerListEntry(new GameProfile(new UUID(0L, 0L), displayName), GameMode.SURVIVAL, 0, Component.text(displayName), 0L, null, new byte[0], true);

            return new PlayerMessage(sender, Component.text(displayName), Component.text(contents));
        }

        return null;
    }
}
