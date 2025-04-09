package me.chayapak1.chomens_bot.util;

public class CodeBlockUtilities {
    public static String escape (final String message) {
        return message.replace("`", "\u200b`");
    }
}
