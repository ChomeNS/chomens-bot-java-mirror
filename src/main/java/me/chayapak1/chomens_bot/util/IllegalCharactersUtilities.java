package me.chayapak1.chomens_bot.util;

public class IllegalCharactersUtilities {
    public static boolean isInvalidChatCharacter (final char character) {
        // RIPPED straight from minecraft code
        return character == 167 || character < ' ' || character == 127;
    }

    public static boolean isValidChatString (final String string) {
        for (final char character : string.toCharArray()) {
            if (isInvalidChatCharacter(character)) return false;
        }

        return true;
    }

    public static String stripIllegalCharacters (final String string) {
        final StringBuilder replaced = new StringBuilder();

        for (final char character : string.toCharArray()) {
            if (isInvalidChatCharacter(character)) continue;

            replaced.append(character);
        }

        return replaced.toString();
    }
}
