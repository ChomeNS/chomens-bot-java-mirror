package me.chayapak1.chomens_bot.util;

public class IllegalCharactersUtilities {
    public static String stripIllegalCharacters (final String string) {
        final StringBuilder replaced = new StringBuilder();

        for (final char character : string.toCharArray()) {
            // ripped STRAIGHT from minecraft code
            if (character == 167 || character < ' ' || character == 127) continue;

            replaced.append(character);
        }

        return replaced.toString();
    }
}
