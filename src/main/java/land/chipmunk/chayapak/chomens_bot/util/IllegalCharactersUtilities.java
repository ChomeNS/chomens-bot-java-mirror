package land.chipmunk.chayapak.chomens_bot.util;

// Original code made by _ChipMC_ IIRC and I ported it to Java
public class IllegalCharactersUtilities {
    public static boolean isAllowedCharacter (char character) {
        return character != '§' && character != '\u007f';
    }

    public static boolean containsIllegalCharacters (String string) {
        for (int i = 0; i < string.length(); i++) if (!isAllowedCharacter(string.charAt(i))) return true;

        return false;
    }
}
