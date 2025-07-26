package me.chayapak1.chomens_bot.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.json.JSONOptions;

import java.util.Map;
import java.util.Set;

// for 1.21.5 SNBT components, not sure how performant this is
// since i'm not good at writing performant code, but this should
// reduce the size of the result being sent to the server by quite a bit

// with some tests on my laptop, the output of help command can take as much as 43 ms (truly advanced)
// but some small components can be as low as 8764 nanoseconds
public class SNBTUtilities {
    private static final GsonComponentSerializer SERIALIZER_1_21_4 =
            GsonComponentSerializer.builder()
                    .options(JSONOptions.byDataVersion().at(4174)) // 24w44a
                    .build();
    private static final GsonComponentSerializer SERIALIZER_1_21_6 =
            GsonComponentSerializer.builder()
                    .options(JSONOptions.byDataVersion().at(4422)) // 25w15a
                    .build();

    private static final String QUOTE = "'";

    private static final String COMMA = ",";

    private static final String BEGIN_OBJECT = "{";
    private static final String COLON = ":";
    private static final String END_OBJECT = "}";

    private static final String BEGIN_ARRAY = "[";
    private static final String END_ARRAY = "]";

    public static String fromComponent (final boolean useSNBTComponents, final Component component) {
        if (!useSNBTComponents) return SERIALIZER_1_21_4.serialize(component);
        else return fromJson(SERIALIZER_1_21_6.serializeToTree(component));
    }

    // RIPPED from https://minecraft.wiki/w/NBT_format#Conversion_from_JSON
    // this is for jsons that are made by adventure's GsonComponentSerializer ONLY.
    // i cannot guarantee if all json stuff will work, it's just the basics
    private static String fromJson (final JsonElement json) {
        if (json.isJsonPrimitive()) {
            final JsonPrimitive primitive = json.getAsJsonPrimitive();

            if (primitive.isString()) return
                    needQuotes(primitive.getAsString())
                            ? QUOTE + escapeString(primitive.getAsString()) + QUOTE
                            : primitive.getAsString();
            else if (primitive.isBoolean()) return primitive.getAsBoolean() ? "1b" : "0b";
            else if (primitive.isNumber()) return String.valueOf(primitive.getAsNumber());
        } else if (json.isJsonArray()) {
            final StringBuilder stringBuilder = new StringBuilder(BEGIN_ARRAY);

            final JsonArray array = json.getAsJsonArray();

            int i = 1;
            for (final JsonElement element : array) {
                stringBuilder.append(fromJson(element));
                if (i++ != array.size()) stringBuilder.append(COMMA);
            }

            stringBuilder.append(END_ARRAY);

            return stringBuilder.toString();
        } else if (json.isJsonObject()) {
            final StringBuilder stringBuilder = new StringBuilder(BEGIN_OBJECT);

            final Set<Map.Entry<String, JsonElement>> entries = json.getAsJsonObject().entrySet();

            int i = 1;
            for (final Map.Entry<String, JsonElement> entry : entries) {
                stringBuilder.append(entry.getKey()) // no escape :O (optional for adventure)
                        .append(COLON)
                        .append(fromJson(entry.getValue()));
                if (i++ != entries.size()) stringBuilder.append(COMMA);
            }

            stringBuilder.append(END_OBJECT);

            return stringBuilder.toString();
        }

        return QUOTE + QUOTE;
    }

    private static String escapeString (final String string) {
        return string
                .replace("\\", "\\\\")
                .replace("'", "\\'")

                // acts like gson, even though it increases size
                // i still want it to behave like this
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    // don't wrap the string with quotes when not needed
    // like {abc:def} instead of {abc:'def'}
    // or some Advanced ones like `abc123.among--.--us__69` will also return false
    public static boolean needQuotes (final String string) {
        if (
                string == null
                        || string.isBlank()
                        // booleans can get interpreted too
                        || string.equalsIgnoreCase("true")
                        || string.equalsIgnoreCase("false")
        ) {
            return true;
        }

        final char firstChar = string.charAt(0);

        // cannot start with 0-9, '-', '.', or '+'
        if (Character.isDigit(firstChar) || firstChar == '.' || firstChar == '-' || firstChar == '+') {
            return true;
        }

        for (int i = 0; i < string.length(); i++) {
            if (!isAllowedChar(string.charAt(i))) return true;
        }

        // check PASSED
        return false;
    }

    private static boolean isAllowedChar (final char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '_' || c == '-' || c == '.';
    }
}
