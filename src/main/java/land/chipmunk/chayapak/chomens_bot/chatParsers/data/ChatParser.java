package land.chipmunk.chayapak.chomens_bot.chatParsers.data;

import net.kyori.adventure.text.Component;

public interface ChatParser {
    PlayerMessage parse (Component message);
}
