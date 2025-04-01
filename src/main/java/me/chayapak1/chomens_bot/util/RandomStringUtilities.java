package me.chayapak1.chomens_bot.util;

import java.security.SecureRandom;

// https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
// but modified
public class RandomStringUtilities {
    private static String nextString () {
        // fard
        for (int i = 0; i < buf.length; i++)
            buf[i] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }

    private static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String lower = upper.toLowerCase();

    private static final String digits = "0123456789";

    private static final String alphanum = upper + lower + digits;

    private static final char[] symbols = alphanum.toCharArray();

    private static char[] buf = null;

    private static final SecureRandom random = new SecureRandom();

    public static String generate (int length) {
        buf = new char[length];

        return nextString();
    }
}
