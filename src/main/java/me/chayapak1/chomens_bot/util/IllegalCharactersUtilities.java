package me.chayapak1.chomens_bot.util;

public class IllegalCharactersUtilities {
    public static String stripIllegalCharacters(String string) {
        final StringBuilder replaced = new StringBuilder();

        for (char character : string.toCharArray()) {
            if (
                    character == '§' ||
                            character == '\u007f' ||

                            // check if character is a control code, also space is the first character after
                            // the control characters so this is why we can do `character < ' '`
                            character < ' '
            ) continue;

            replaced.append(character);
        }

        return replaced.toString();
    }
}
