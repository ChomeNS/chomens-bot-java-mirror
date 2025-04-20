package me.chayapak1.chomens_bot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.chayapak1.chomens_bot.Bot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.Map;

// for 1.21.5 SNBT components, not sure how performant this is
// since i'm not good at writing performant code, but this should
// reduce the size of the result being sent to the server by quite a bit
public class SNBTUtilities {
    private static final String QUOTE = "'";

    private static final String COMMA = ",";

    private static final String BEGIN_OBJECT = "{";
    private static final String COLON = ":";
    private static final String END_OBJECT = "}";

    private static final String BEGIN_ARRAY = "[";
    private static final String END_ARRAY = "]";

    public static String fromComponent (final Bot bot, final Component component) {
        if (!bot.options.useSNBTComponents) return GsonComponentSerializer.gson().serialize(component);

        final JsonElement json = GsonComponentSerializer.gson().serializeToTree(component);

        return fromJson(json);
    }

    // RIPPED from https://minecraft.wiki/w/NBT_format#Conversion_from_JSON
    public static String fromJson (final JsonElement json) {
        if (json.isJsonPrimitive()) {
            final JsonPrimitive primitive = json.getAsJsonPrimitive();

            if (primitive.isString()) return
                    // don't wrap the string with quotes when not needed
                    // like {abc:def} instead of {abc:'def'}
                    needQuotes(primitive.getAsString())
                            ? QUOTE + escapeString(primitive.getAsString()) + QUOTE
                            : primitive.getAsString();
            else if (primitive.isBoolean()) return primitive.getAsBoolean() ? "1b" : "0b";
            else if (primitive.isNumber()) return String.valueOf(primitive.getAsNumber());
        } else if (json.isJsonArray()) {
            final StringBuilder stringBuilder = new StringBuilder(BEGIN_ARRAY);

            boolean notEmpty = false;

            for (final JsonElement element : json.getAsJsonArray()) {
                notEmpty = true;
                stringBuilder.append(fromJson(element));
                stringBuilder.append(COMMA);
            }

            if (notEmpty) stringBuilder.deleteCharAt(stringBuilder.length() - 1); // removes comma

            stringBuilder.append(END_ARRAY);

            return stringBuilder.toString();
        } else if (json.isJsonObject()) {
            final StringBuilder stringBuilder = new StringBuilder(BEGIN_OBJECT);

            boolean notEmpty = false;

            for (final Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                notEmpty = true;

                // FIXME: only works in 1.21.5 adventure, at the time of writing this,
                //        the 1.21.5 PR is still not merged yet, so i am doing
                //        a lazy fix for the hover event `contents` being replaced with
                //        `value` and the hoverEvent and clickEvent being snake case,
                //        so some things work

                if (entry.getKey().equals("contents")) {
                    stringBuilder.append("value");

                    // no click_event replacement yet

                } else {
                    stringBuilder.append(convertCamelCaseToSnake(entry.getKey()));
                }
                stringBuilder.append(COLON);
                stringBuilder.append(fromJson(entry.getValue()));
                stringBuilder.append(COMMA);
            }

            if (notEmpty) stringBuilder.deleteCharAt(stringBuilder.length() - 1); // removes comma

            stringBuilder.append(END_OBJECT);

            return stringBuilder.toString();
        }

        return QUOTE + QUOTE;
    }

    private static String escapeString (final String string) {
        return string
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    // should this be in StringUtilities?
    public static boolean needQuotes (final String string) {
        if (string.isBlank()) return true;
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            // even though we can do like `abc123.among.us__69` without quotes
            // i am too lazy to implement that
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))) {
                return true;
            }
        }
        return false;
    }

    // https://www.baeldung.com/java-camel-snake-case-conversion
    // 3. Manual Approach
    // FIXME: this is optional after adventure 1.21.5, remove this afterward
    //        it's why this is here instead of in StringUtilities
    public static String convertCamelCaseToSnake (final String input) {
        final StringBuilder result = new StringBuilder();
        for (final char c : input.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append("_").append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
