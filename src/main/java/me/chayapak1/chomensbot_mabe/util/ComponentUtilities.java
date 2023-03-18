package me.chayapak1.chomensbot_mabe.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.KeybindComponent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// totallynotskidded™ from chipmunkbot
public class ComponentUtilities {
    private static final Map<String, String> language = loadJsonStringMap("language.json");
    private static final Map<String, String> keybinds = loadJsonStringMap("keybinds.json");

    public static final Pattern ARG_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([s%])");

    private ComponentUtilities () {
    }

    private static Map<String, String> loadJsonStringMap (String name) {
        Map<String, String> map = new HashMap<>();

        InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            map.put(entry.getKey(), json.get(entry.getKey()).getAsString());
        }

        return map;
    }

    private static String getOrReturnKey (String key) {
        return ComponentUtilities.language.getOrDefault(key, key);
    }

    public static String stringify (Component message) {
        StringBuilder builder = new StringBuilder();

        builder.append(stringifyPartially(message));

        for (Component child : message.children()) builder.append(stringify(child));

        return builder.toString();
    }

    public static String stringifyPartially (Component message) {
        if (message instanceof TextComponent) return stringifyPartially((TextComponent) message);
        if (message instanceof TranslatableComponent) return stringifyPartially((TranslatableComponent) message);
        if (message instanceof SelectorComponent) return stringifyPartially((SelectorComponent) message);
        if (message instanceof KeybindComponent) return stringifyPartially((KeybindComponent) message);

        return "";
    }

    public static String stringifyPartially (TextComponent message) {
        return message.content();
    }

    public static String stringifyPartially (TranslatableComponent message) {
        String format = getOrReturnKey(message.key());

        // totallynotskidded™️ from HBot (and changed a bit)
        Matcher matcher = ARG_PATTERN.matcher(format);
        StringBuilder sb = new StringBuilder();

        int i = 0;
        while (matcher.find()) {
            if (matcher.group().equals("%%")) {
                matcher.appendReplacement(sb, "%");
            } else {
                String idxStr = matcher.group(1);
                int idx = idxStr == null ? i++ : (Integer.parseInt(idxStr) - 1);
                if (idx >= 0 && idx < message.args().size()) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement( stringify(message.args().get(idx)) ));
                } else {
                    matcher.appendReplacement(sb, "");
                }
            }
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static String stringifyPartially (SelectorComponent message) {
        return message.pattern(); // * Client-side selector components are equivalent to text ones, and do NOT list entities.
    }

    public static String stringifyPartially (KeybindComponent message) {
        String keybind = message.keybind();
        Component component = keybinds.containsKey(keybind) ? Component.translatable(keybind) : Component.text(keybind); // TODO: Fix some keys like `key.keyboard.a`
        return stringifyPartially(component);
    }
}
