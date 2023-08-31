package land.chipmunk.chayapak.chomens_bot.song;

import land.chipmunk.chayapak.chomens_bot.Bot;

public interface Converter {
    Song getSongFromBytes (byte[] bytes, String fileName, Bot bot) throws Exception;
}
