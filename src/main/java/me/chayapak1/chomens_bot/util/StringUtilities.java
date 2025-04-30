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
    public static String removeNamespace (final String command) {
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

    // Author: ChatGPT
    public static String fromUTF8Lossy (final byte[] input) {
        final StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < input.length) {
            final byte b = input[i];

            if ((b & 0x80) == 0) {
                // ASCII byte (0xxxxxxx)
                result.append((char) b);
            } else {
                // Try to decode as UTF-8 multibyte sequence
                final int bytesRemaining = input.length - i;

                // UTF-8 rules: number of bytes in sequence based on first byte
                int seqLen = -1;
                if ((b & 0xE0) == 0xC0 && bytesRemaining >= 2) seqLen = 2;
                else if ((b & 0xF0) == 0xE0 && bytesRemaining >= 3) seqLen = 3;
                else if ((b & 0xF8) == 0xF0 && bytesRemaining >= 4) seqLen = 4;

                if (seqLen > 1) {
                    boolean valid = true;
                    for (int j = 1; j < seqLen; j++) {
                        if ((input[i + j] & 0xC0) != 0x80) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) {
                        try {
                            final String s = new String(input, i, seqLen, StandardCharsets.UTF_8);
                            result.append(s);
                            i += seqLen;
                            continue;
                        } catch (final Exception e) {
                            // Fall through to escape
                        }
                    }
                }

                // If invalid UTF-8 sequence or unknown pattern, escape the byte
                result.append(String.format("<%04X>", b & 0xFF));
            }

            i++;
        }

        return result.toString();
    }

    // https://stackoverflow.com/a/35148974/18518424
    public static String truncateToFitUtf8ByteLength (final String s, final int maxBytes) {
        if (s == null) {
            return null;
        }
        final Charset charset = StandardCharsets.UTF_8;
        final CharsetDecoder decoder = charset.newDecoder();
        final byte[] sba = s.getBytes(charset);
        if (sba.length <= maxBytes) {
            return s;
        }
        // Ensure truncation by having byte buffer = maxBytes
        final ByteBuffer bb = ByteBuffer.wrap(sba, 0, maxBytes);
        final CharBuffer cb = CharBuffer.allocate(maxBytes);
        // Ignore an incomplete character
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.decode(bb, cb, true);
        decoder.flush(cb);
        return new String(cb.array(), 0, cb.position());
    }

    // https://stackoverflow.com/a/25379180/18518424
    public static boolean containsIgnoreCase (final String src, final String what) {
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

    public static String addPlural (final long amount, final String unit) {
        return amount > 1 ? unit + "s" : unit;
    }

    public static boolean isNotNullAndNotBlank (final String text) {
        return text != null && !text.isBlank();
    }

    public static String replaceAllWithMap (final String input, final Map<String, String> replacements) {
        String result = input;
        for (final Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replaceAll(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
