package me.chayapak1.chomens_bot.song;

import me.chayapak1.chomens_bot.Bot;

public interface Converter {
    Song getSongFromBytes (byte[] bytes, String fileName, Bot bot) throws Exception;
}
