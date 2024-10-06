package me.chayapak1.chomens_bot.data.chat;

import net.kyori.adventure.text.Component;

public interface ChatParser {
    PlayerMessage parse (Component message);
}
