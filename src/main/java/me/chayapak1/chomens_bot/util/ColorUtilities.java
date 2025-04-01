package me.chayapak1.chomens_bot.util;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextFormat;
import net.kyori.adventure.text.serializer.legacy.CharacterAndFormat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ColorUtilities {
    private static final Map<TextFormat, Character> formatToLegacyMap = CharacterAndFormat.defaults().stream()
            .collect(Collectors.toUnmodifiableMap(CharacterAndFormat::format, CharacterAndFormat::character));
    private static final Map<Integer, String> ansiToIrcMap = new HashMap<>();
    private static final Map<Integer, String> ansiStyleToIrcMap = new HashMap<>();

    public static TextColor getColorByString (String _color) {
        final String color = _color.toLowerCase();

        if (color.startsWith("#")) return TextColor.fromHexString(color);
        else {
            // am i reinventing the wheel here? Yes.
            return Optional.ofNullable(NamedTextColor.NAMES.value(color))
                    .orElse(NamedTextColor.WHITE);
        }
    }

    public static char getClosestChatColor (int rgb) {
        final NamedTextColor closestNamed = NamedTextColor.nearestTo(TextColor.color(rgb));
        return formatToLegacyMap.get(closestNamed);
    }

    // Author: ChatGPT
    static {
        ansiToIrcMap.put(0, "0"); // Reset -> IRC White (is this correct?)

        ansiToIrcMap.put(30, "1"); // Black -> IRC Black
        ansiToIrcMap.put(31, "40"); // Red -> IRC Red
        ansiToIrcMap.put(32, "32"); // Green -> IRC Green
        ansiToIrcMap.put(33, "53"); // Yellow -> IRC Yellow
        ansiToIrcMap.put(34, "60"); // Blue -> IRC Blue
        ansiToIrcMap.put(35, "49"); // Magenta -> IRC Magenta (Purple)
        ansiToIrcMap.put(36, "46"); // Cyan -> IRC Cyan
        ansiToIrcMap.put(37, "96"); // Gray -> IRC Gray
        ansiToIrcMap.put(39, "0"); // White -> IRC White

        ansiToIrcMap.put(90, "92"); // Dark Gray -> IRC Dark Gray
        ansiToIrcMap.put(92, "56"); // Green -> IRC Green
        ansiToIrcMap.put(96, "58"); // Blue -> IRC Blue
        ansiToIrcMap.put(91, "52"); // Red -> IRC Red
        ansiToIrcMap.put(94, "60"); // Blue -> IRC Blue
        ansiToIrcMap.put(95, "61"); // Magenta -> IRC Magenta (Purple)
        ansiToIrcMap.put(93, "54"); // Yellow -> IRC Yellow
        ansiToIrcMap.put(97, "0"); // White -> IRC White

        final Map<Integer, String> clone = new HashMap<>(ansiToIrcMap);

        for (Map.Entry<Integer, String> entry : clone.entrySet()) {
            ansiToIrcMap.put(entry.getKey() + 10, ansiToIrcMap.get(entry.getKey()));
        }

        ansiToIrcMap.put(49, "01");

        // Styles
        ansiStyleToIrcMap.put(1, "\u0002"); // Bold
        ansiStyleToIrcMap.put(3, "\u001d"); // Italic
        ansiStyleToIrcMap.put(4, "\u001f"); // Underlined
    }

    public static String convertAnsiToIrc (String input) {
        StringBuilder result = new StringBuilder();
        boolean insideEscape = false;
        StringBuilder ansiCode = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (insideEscape) {
                if (c == 'm') {
                    insideEscape = false;
                    // Parse the ANSI color code and map it to IRC color
                    String[] codes = ansiCode.toString().split(";");
                    for (String code : codes) {
                        try {
                            int ansiColorCode = Integer.parseInt(code);
                            if (ansiToIrcMap.containsKey(ansiColorCode)) {
                                result.append("\u0003").append(ansiToIrcMap.get(ansiColorCode));
                            } else if (ansiStyleToIrcMap.containsKey(ansiColorCode)) {
                                result.append(ansiStyleToIrcMap.get(ansiColorCode));
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    ansiCode.setLength(0); // Clear the buffer
                } else {
                    ansiCode.append(c); // Collect the rest of the ANSI sequence
                }
            } else {
                if (c == '\u001B' && i + 1 < input.length() && input.charAt(i + 1) == '[') {
                    insideEscape = true;
                    i++; // Skip the '[' character
                } else {
                    result.append(c); // Append non-ANSI characters directly
                }
            }
        }

        return result.toString();
    }
}
