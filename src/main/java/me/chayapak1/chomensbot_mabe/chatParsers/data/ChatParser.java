package me.chayapak1.chomensbot_mabe.chatParsers.data;

import net.kyori.adventure.text.Component;

public interface ChatParser {
    PlayerMessage parse (Component message);
}
