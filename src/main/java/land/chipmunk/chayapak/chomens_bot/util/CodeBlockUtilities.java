package land.chipmunk.chayapak.chomens_bot.util;

public class CodeBlockUtilities {
    public static String escape (String message) {
        return message.replace("`", "\u200b`");
    }
}
