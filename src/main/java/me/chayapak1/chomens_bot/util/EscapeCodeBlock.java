package me.chayapak1.chomens_bot.util;

public class EscapeCodeBlock {
    public static String escape (String message) {
        return message.replace("`", "\\`");
    }
}
