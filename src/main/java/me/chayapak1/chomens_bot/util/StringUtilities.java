package me.chayapak1.chomens_bot.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

public class StringUtilities {
    public static String removeNamespace (String command) {
        final StringBuilder removedCommand = new StringBuilder(command);

        final String[] splitSpace = command.split("\\s+"); // [minecraft:test, arg1, arg2, ...]
        final String[] splitColon = splitSpace[0].split(":"); // [minecraft, test]

        if (splitColon.length >= 2) {
            removedCommand.setLength(0);
            removedCommand.append(String.join(":", Arrays.copyOfRange(splitColon, 1, splitColon.length)));

            if (splitSpace.length > 1) {
                removedCommand.append(' ');
                removedCommand.append(String.join(" ", Arrays.copyOfRange(splitSpace, 1, splitSpace.length)));
            }
        }

        return removedCommand.toString();
    }

    // https://stackoverflow.com/a/35148974/18518424
    public static String truncateToFitUtf8ByteLength (String s, int maxBytes) {
        if (s == null) {
            return null;
        }
        Charset charset = StandardCharsets.UTF_8;
        CharsetDecoder decoder = charset.newDecoder();
        byte[] sba = s.getBytes(charset);
        if (sba.length <= maxBytes) {
            return s;
        }
        // Ensure truncation by having byte buffer = maxBytes
        ByteBuffer bb = ByteBuffer.wrap(sba, 0, maxBytes);
        CharBuffer cb = CharBuffer.allocate(maxBytes);
        // Ignore an incomplete character
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.decode(bb, cb, true);
        decoder.flush(cb);
        return new String(cb.array(), 0, cb.position());
    }

    // https://stackoverflow.com/a/25379180/18518424
    public static boolean containsIgnoreCase(String src, String what) {
        final int length = what.length();
        if (length == 0)
            return true; // Empty string is contained

        final char firstLo = Character.toLowerCase(what.charAt(0));
        final char firstUp = Character.toUpperCase(what.charAt(0));

        for (int i = src.length() - length; i >= 0; i--) {
            // Quick check before calling the more expensive regionMatches() method:
            final char ch = src.charAt(i);
            if (ch != firstLo && ch != firstUp)
                continue;

            if (src.regionMatches(true, i, what, 0, length))
                return true;
        }

        return false;
    }

    public static String addPluralS (long amount, String unit) {
        return amount > 1 ? unit + "s" : unit;
    }

    public static boolean isNotNullAndNotBlank (String text) {
        return text != null && !text.isBlank();
    }

    public static String replaceAllWithMap (String input, Map<String, String> replacements) {
        String result = input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replaceAll(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
