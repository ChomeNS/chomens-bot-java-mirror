package me.chayapak1.chomens_bot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.chayapak1.chomens_bot.Bot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.Map;

// for 1.21.5 SNBT components, not sure how performant this is
// since i'm not good at writing performant code, but this should
// reduce the size of the result being sent to the server by quite a bit

// with some tests on my laptop, the output of help command can take as much as 43 ms (truly advanced)
// but some small components can be as low as 8764 nanoseconds
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

                // FIXME: remove all these events fixer after adventure updates to 1.21.5, this is a SUPER lazy and temporary fix
                if (entry.getKey().equals("hoverEvent") && entry.getValue().isJsonObject()) {
                    final JsonObject object = entry.getValue().getAsJsonObject();
                    final String action = object.getAsJsonPrimitive("action").getAsString();

                    final JsonObject fixedObject = new JsonObject();

                    fixedObject.addProperty("action", action);

                    switch (action) {
                        case "show_text" -> fixedObject.add("value", object.get("contents"));
                        case "show_item" -> {
                            final JsonObject contents = object.get("contents").getAsJsonObject();
                            fixedObject.add("id", contents.get("id"));
                            fixedObject.add("count", contents.get("count"));
                            fixedObject.add("components", contents.get("components"));
                        }
                        case "show_entity" -> {
                            final JsonObject contents = object.get("contents").getAsJsonObject();
                            fixedObject.add("name", contents.get("name"));
                            fixedObject.add("id", contents.get("id"));
                            fixedObject.add("uuid", contents.get("uuid"));
                        }
                    }

                    stringBuilder.append("hover_event")
                            .append(COLON)
                            .append(fromJson(fixedObject))
                            .append(COMMA);
                } else if (entry.getKey().equals("clickEvent") && entry.getValue().isJsonObject()) {
                    final JsonObject object = entry.getValue().getAsJsonObject();

                    final String action = object.getAsJsonPrimitive("action").getAsString();
                    final JsonElement value = object.getAsJsonPrimitive("value");

                    final JsonObject fixedObject = new JsonObject();

                    fixedObject.addProperty("action", action);

                    switch (action) {
                        case "open_url" -> fixedObject.add("url", value);
                        case "open_file" -> fixedObject.add("path", value);
                        case "run_command", "suggest_command" -> fixedObject.add("command", value);
                        case "change_page" -> fixedObject.add("page", value);
                        case "copy_to_clipboard" -> fixedObject.add("value", value);
                    }

                    stringBuilder.append("click_event")
                            .append(COLON)
                            .append(fromJson(fixedObject))
                            .append(COMMA);
                } else {
                    stringBuilder.append(entry.getKey()) // no escape :O (optional for adventure)
                            .append(COLON)
                            .append(fromJson(entry.getValue()))
                            .append(COMMA);
                }
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
    public static boolean needQuotes (final String string) {
        if (
                string.isBlank()
                        // booleans can get interpreted too
                        || string.equalsIgnoreCase("true")
                        || string.equalsIgnoreCase("false")
        ) return true;

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
}
