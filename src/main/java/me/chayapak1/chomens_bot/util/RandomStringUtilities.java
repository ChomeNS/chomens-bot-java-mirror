package me.chayapak1.chomens_bot.util;

import java.security.SecureRandom;

// https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
// but modified a lot
public class RandomStringUtilities {
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = UPPER.toLowerCase();
    private static final String DIGITS = "0123456789";

    public static final char[] ALPHANUMERIC = (UPPER + LOWER + DIGITS).toCharArray();
    public static final char[] ALPHABETS_ONLY = (UPPER + LOWER).toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generate (final int length, final char[] symbols) {
        final char[] buf = new char[length];

        for (int i = 0; i < buf.length; i++)
            buf[i] = symbols[RANDOM.nextInt(symbols.length)];

        return new String(buf);
    }
}
