package land.chipmunk.chayapak.chomens_bot.util;

public class IllegalCharactersUtilities {
    public static boolean isAllowedCharacter (char character) {
        return character != '\u00a7' && character != '\u007f';
    }

    public static boolean containsIllegalCharacters (String string) {
        for (int i = 0; i < string.length(); i++) if (!isAllowedCharacter(string.charAt(i))) return true;

        return false;
    }
}
