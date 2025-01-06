package me.chayapak1.chomens_bot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// totallynotskidded™ from chipmunkbot and added colors (ignore the ohio code please,..,.)
public class ComponentUtilities {
    // component parsing
    public static final Map<String, String> LANGUAGE = loadJsonStringMap("language.json");
    public static final Map<String, String> VOICE_CHAT_LANGUAGE = loadJsonStringMap("voiceChatLanguage.json");
    public static final Map<String, String> KEYBINDINGS = loadJsonStringMap("keybinds.json");

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

    public static String getOrReturnFallback (TranslatableComponent component) {
        final String key = component.key();

        final String minecraftKey = LANGUAGE.get(key);
        final String voiceChatKey = VOICE_CHAT_LANGUAGE.get(key);

        if (minecraftKey != null) return minecraftKey;
        else if (voiceChatKey != null) return voiceChatKey;
        else return component.fallback() != null ? component.fallback() : key;
    }

    public static String stringify (Component message) { return new ComponentParser().stringify(message, ComponentParser.ParseType.PLAIN); }
    public static String stringifySectionSign (Component message) { return new ComponentParser().stringify(message, ComponentParser.ParseType.SECTION_SIGNS); }
    public static String stringifyAnsi (Component message) { return new ComponentParser().stringify(message, ComponentParser.ParseType.ANSI); }
    public static String stringifyDiscordAnsi (Component message) { return new ComponentParser().stringify(message, ComponentParser.ParseType.DISCORD_ANSI); }

    private static class ComponentParser {
        public static final Pattern ARG_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([s%])");

        public static final int MAX_DEPTH = 12;

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

        private ParseType type;
        private int depth = 0;

        private String lastStyle = "";

        private boolean isSubParsing = false;

        private String stringify (Component message, ParseType type) {
            this.type = type;

            if (depth > MAX_DEPTH) return "";

            try {
                final StringBuilder builder = new StringBuilder();

                final String color = getColor(message.color());
                final String style = getStyle(message.style());

                final String output = stringifyPartially(message, color, style);

                builder.append(output);

                for (Component child : message.children()) {
                    final ComponentParser parser = new ComponentParser();
                    parser.lastStyle = lastStyle + color + style;
                    parser.depth = depth;
                    parser.isSubParsing = true;
                    builder.append(parser.stringify(child, type));
                }

                if (
                        !isSubParsing &&
                                (type == ParseType.ANSI || type == ParseType.DISCORD_ANSI)
                ) {
                    builder.append(ansiMap.get("r"));
                }

                if (type == ParseType.DISCORD_ANSI) {
                    // as of the time writing this (2024-12-28) discord doesn't support the bright colors yet
                    return builder.toString().replace("\u001b[9", "\u001b[3");
                } else {
                    return builder.toString();
                }
            } catch (Exception e) {
                LoggerUtilities.error(e);
                return "";
            }
        }

        public String stringifyPartially (Component message, String color, String style) {
            return switch (message) {
                case TextComponent t_component -> stringifyPartially(t_component, color, style);
                case TranslatableComponent t_component -> stringifyPartially(t_component, color, style);
                case SelectorComponent t_component -> stringifyPartially(t_component, color, style);
                case KeybindComponent t_component -> stringifyPartially(t_component, color, style);
                default -> "";
            };
        }

        public String getStyle (Style textStyle) {
            if (textStyle == null) return "";

            StringBuilder style = new StringBuilder();

            for (Map.Entry<TextDecoration, TextDecoration.State> decorationEntry : textStyle.decorations().entrySet()) {
                final TextDecoration decoration = decorationEntry.getKey();
                final TextDecoration.State state = decorationEntry.getValue();

                if (state == TextDecoration.State.NOT_SET || state == TextDecoration.State.FALSE) continue;

                if (type == ParseType.ANSI) {
                    switch (decoration) {
                        case BOLD -> style.append(ansiMap.get("l"));
                        case ITALIC -> style.append(ansiMap.get("o"));
                        case OBFUSCATED -> style.append(ansiMap.get("k"));
                        case UNDERLINED -> style.append(ansiMap.get("n"));
                        case STRIKETHROUGH -> style.append(ansiMap.get("m"));
                    }
                } else if (type == ParseType.SECTION_SIGNS) {
                    switch (decoration) {
                        case BOLD -> style.append("§l");
                        case ITALIC -> style.append("§o");
                        case OBFUSCATED -> style.append("§k");
                        case UNDERLINED -> style.append("§n");
                        case STRIKETHROUGH -> style.append("§m");
                    }
                }
            }

            return style.toString();
        }

        public String getColor (TextColor color) {
            if (color == null) return "";

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

            if (type == ParseType.SECTION_SIGNS) {
                return "§" + code;
            } else if (type == ParseType.ANSI || type == ParseType.DISCORD_ANSI) {
                String ansiCode = ansiMap.get(code);
                if (ansiCode == null) {
                    if (type == ParseType.DISCORD_ANSI) {
                        // gets the closest color to the hex

                        final int rgb = Integer.parseInt(code.substring(1), 16);

                        final String chatColor = ColorUtilities.getClosestChatColor(rgb);

                        ansiCode = ansiMap.get(chatColor);
                    } else {
                        ansiCode = "\u001b[38;2;" +
                                color.red() +
                                ";" +
                                color.green() +
                                ";" +
                                color.blue() +
                                "m";
                    }
                }

                return ansiCode;
            } else {
                return "";
            }
        }

        private String getPartialResultAndSetLastColor (String originalResult, String color, String style) {
            if (type == ParseType.PLAIN) return originalResult;

            String resetCode;
            if (type == ParseType.ANSI || type == ParseType.DISCORD_ANSI) resetCode = ansiMap.get("r");
            else resetCode = "§r";

            final String result =
                    lastStyle + color + style +
                    originalResult +
                    (lastStyle.isEmpty() ? resetCode : "");

            lastStyle = color + style;

            return result;
        }

        private String stringifyPartially (String message, String color, String style) {
            if (type == ParseType.PLAIN) return message;

            final boolean isAllAnsi = type == ParseType.ANSI || type == ParseType.DISCORD_ANSI;

            String replacedContent = message;
            // processes section signs
            // not processing invalid codes is INTENTIONAL and it is a FEATURE
            if (isAllAnsi && replacedContent.contains("§")) {
                // is try-catch a great idea for these?
                try {
                    replacedContent = Pattern
                            .compile("(§.)")
                            .matcher(message)
                            .replaceAll(m -> {
                                final String code = m.group(0).substring(1);

                                if (!code.equals("r")) return ansiMap.get(code);
                                else return color;
                            });
                } catch (Exception ignored) {}
            }

            return getPartialResultAndSetLastColor(replacedContent, color, style);
        }

        private String stringifyPartially (TextComponent message, String color, String style) {
            return stringifyPartially(message.content(), color, style);
        }

        private String stringifyPartially (TranslatableComponent message, String color, String style) {
            final String format = getOrReturnFallback(message);

            // totallynotskidded™️from HBot (and changed a bit)
            Matcher matcher = ARG_PATTERN.matcher(format);
            StringBuilder sb = new StringBuilder();

            // not checking if arguments length equals input format length
            // is INTENTIONAL and is a FEATURE
            int i = 0;
            while (matcher.find()) {
                depth++;
                if (matcher.group().equals("%%")) {
                    matcher.appendReplacement(sb, "%");
                } else {
                    final String idxStr = matcher.group(1);

                    int idx = idxStr == null ? i++ : (Integer.parseInt(idxStr) - 1);

                    if (idx >= 0 && idx < message.arguments().size()) {
                        final ComponentParser parser = new ComponentParser();

                        parser.lastStyle = lastStyle + color + style;
                        parser.depth = depth;
                        parser.isSubParsing = true;

                        matcher.appendReplacement(
                                sb,
                                Matcher.quoteReplacement(
                                        parser.stringify(
                                                message.arguments()
                                                        .get(idx)
                                                        .asComponent(),
                                                type
                                        ) + lastStyle + color + style // IMPORTANT!!!!
                                )
                        );
                    } else {
                        matcher.appendReplacement(sb, "");
                    }
                }
            }

            matcher.appendTail(sb);

            return getPartialResultAndSetLastColor(sb.toString(), color, style);
        }

        // on the client side, this acts just like TextComponent
        // and does NOT process any players stuff
        private String stringifyPartially (SelectorComponent message, String color, String style) {
            return stringifyPartially(message.pattern(), style, color);
        }

        public String stringifyPartially (KeybindComponent message, String color, String style) {
            final String keybind = message.keybind();

            // FIXME: this isn't the correct way to parse keybinds
            final Component component = KEYBINDINGS.containsKey(keybind) ?
                    Component.translatable(KEYBINDINGS.get(keybind)) :
                    Component.text(keybind);

            return stringifyPartially(component, color, style);
        }

        public enum ParseType {
            PLAIN,
            SECTION_SIGNS,
            ANSI,
            DISCORD_ANSI
        }
    }
}
