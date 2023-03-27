package land.chipmunk.chayapak.chomens_bot.util;

public class EscapeCodeBlock {
    public static String escape (String message) {
        return message.replace("`", "\u200b`");
    }
}
