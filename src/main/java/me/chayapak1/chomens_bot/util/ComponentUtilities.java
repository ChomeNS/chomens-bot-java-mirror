package me.chayapak1.chomens_bot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// totallynotskidded™ from chipmunkbot and added colors (anyone please fix my shitcode)
public class ComponentUtilities {
    private static final Map<String, String> language = loadJsonStringMap("language.json");
    private static final Map<String, String> keybinds = loadJsonStringMap("keybinds.json");

    public static final Pattern ARG_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([s%])");

    public static final Map<String, String> ansiMap = new HashMap<>();
    static {
        // map totallynotskidded™ from https://github.com/PrismarineJS/prismarine-chat/blob/master/index.js#L10
        ansiMap.put("0", "\u001b[30m");
        ansiMap.put("1", "\u001b[34m");
        ansiMap.put("2", "\u001b[32m");
        ansiMap.put("3", "\u001b[36m");
        ansiMap.put("4", "\u001b[31m");
        ansiMap.put("5", "\u001b[35m");
        ansiMap.put("6", "\u001b[33m");
        ansiMap.put("7", "\u001b[37m");
        ansiMap.put("8", "\u001b[90m");
        ansiMap.put("9", "\u001b[94m");
        ansiMap.put("a", "\u001b[92m");
        ansiMap.put("b", "\u001b[96m");
        ansiMap.put("c", "\u001b[91m");
        ansiMap.put("d", "\u001b[95m");
        ansiMap.put("e", "\u001b[93m");
        ansiMap.put("f", "\u001b[97m");
        ansiMap.put("l", "\u001b[1m");
        ansiMap.put("o", "\u001b[3m");
        ansiMap.put("n", "\u001b[4m");
        ansiMap.put("m", "\u001b[9m");
        ansiMap.put("k", "\u001b[6m");
        ansiMap.put("r", "\u001b[0m");
    }

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

        builder.append(stringifyPartially(message, false, false));

        for (Component child : message.children()) builder.append(stringify(child));

        return builder.toString();
    }

    public static String stringifyMotd (Component message) {
        StringBuilder builder = new StringBuilder();

        builder.append(stringifyPartially(message, true, false));

        for (Component child : message.children()) builder.append(stringifyMotd(child));

        return builder.toString();
    }

    public static String stringifyAnsi (Component message) {
        StringBuilder builder = new StringBuilder();

        builder.append(stringifyPartially(message, false, true));

        for (Component child : message.children()) builder.append(stringifyAnsi(child));

        return builder.toString();
    }

    public static String stringifyPartially (Component message, boolean motd, boolean ansi) {
        if (message instanceof TextComponent) return stringifyPartially((TextComponent) message, motd, ansi);
        if (message instanceof TranslatableComponent) return stringifyPartially((TranslatableComponent) message, motd, ansi);
        if (message instanceof SelectorComponent) return stringifyPartially((SelectorComponent) message, motd, ansi);
        if (message instanceof KeybindComponent) return stringifyPartially((KeybindComponent) message, motd, ansi);

        return "";
    }

    public static String getColor (TextColor color, boolean motd, boolean ansi) {
        if (color == null) return null;

        // map totallynotskidded™ too from https://github.com/PrismarineJS/prismarine-chat/blob/master/index.js#L299
        String code;
        if (color == NamedTextColor.BLACK) code = "0";
        else if (color == NamedTextColor.DARK_BLUE) code = "1";
        else if (color == NamedTextColor.DARK_GREEN) code = "2";
        else if (color == NamedTextColor.DARK_AQUA) code = "3";
        else if (color == NamedTextColor.DARK_RED) code = "4";
        else if (color == NamedTextColor.DARK_PURPLE) code = "5";
        else if (color == NamedTextColor.GOLD) code = "6";
        else if (color == NamedTextColor.GRAY) code = "7";
        else if (color == NamedTextColor.DARK_GRAY) code = "8";
        else if (color == NamedTextColor.BLUE) code = "9";
        else if (color == NamedTextColor.GREEN) code = "a";
        else if (color == NamedTextColor.AQUA) code = "b";
        else if (color == NamedTextColor.RED) code = "c";
        else if (color == NamedTextColor.LIGHT_PURPLE) code = "d";
        else if (color == NamedTextColor.YELLOW) code = "e";
        else if (color == NamedTextColor.WHITE) code = "f";
        else {
            try {
                code = color.asHexString();
            } catch (NullPointerException e) {
                code = ""; // mabe...,,.,..,
            }
        }

        if (motd) {
            return "§" + code;
        } else if (ansi) {
            String ansiCode = ansiMap.get(code);
            if (ansiCode == null) ansiCode = "\u001b[38;2;" + color.red() + ";" + color.green() + ";" + color.blue() + "m";

            return ansiCode;
        } else return null;
    }

    public static String stringifyPartially (TextComponent message, boolean motd, boolean ansi) {
        if (motd || ansi) {
            final String color = getColor(message.color(), motd, ansi);
            return (color != null ? color : "") + message.content() + (ansi ? ansiMap.get("r") : "");
        }

        return message.content();
    }

    public static String stringifyPartially (TranslatableComponent message, boolean motd, boolean ansi) {
        String format = getOrReturnKey(message.key());

        // totallynotskidded™️ from HBot (and changed a bit)
        Matcher matcher = ARG_PATTERN.matcher(format);
        StringBuilder sb = new StringBuilder();

        final String _color = getColor(message.color(), motd, ansi);
        String color;
        if (_color == null) color = "";
        else color = _color;

        int i = 0;
        while (matcher.find()) {
            if (matcher.group().equals("%%")) {
                matcher.appendReplacement(sb, "%");
            } else {
                String idxStr = matcher.group(1);
                int idx = idxStr == null ? i++ : (Integer.parseInt(idxStr) - 1);
                if (idx >= 0 && idx < message.args().size()) {
                    matcher.appendReplacement(
                            sb,
                            Matcher.quoteReplacement(
                                    motd ?
                                            stringifyMotd(message.args().get(idx)) + color :
                                            (
                                                    ansi ?
                                                            stringifyAnsi(message.args().get(idx)) + color :
                                                            stringify(message.args().get(idx))
                                            )
                            )
                    );
                } else {
                    matcher.appendReplacement(sb, "");
                }
            }
        }
        matcher.appendTail(sb);

        return color + sb + (ansi ? ansiMap.get("r") : "");
    }

    public static String stringifyPartially (SelectorComponent message, boolean motd, boolean ansi) {
        final String _color = getColor(message.color(), motd, ansi);
        String color;
        if (_color == null) color = "";
        else color = _color;
        return color + message.pattern(); // * Client-side selector components are equivalent to text ones, and do NOT list entities.
    }

    public static String stringifyPartially (KeybindComponent message, boolean motd, boolean ansi) {
        String keybind = message.keybind();
        Component component = keybinds.containsKey(keybind) ? Component.translatable(keybind) : Component.text(keybind); // TODO: Fix some keys like `key.keyboard.a`
        return stringifyPartially(component, motd, ansi);
    }
}
