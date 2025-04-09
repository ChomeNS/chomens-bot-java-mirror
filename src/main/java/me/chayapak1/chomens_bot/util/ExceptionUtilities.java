package me.chayapak1.chomens_bot.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtilities {
    // totallynotskidded™ from apache's common utils thingy
    public static String getStacktrace (final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);

        throwable.printStackTrace(pw);

        return sw.getBuffer().toString();
    }
}
