package me.chayapak1.chomens_bot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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

public class ComponentUtilities {
    // https://github.com/kaboomserver/extras/blob/master/src/main/java/pw/kaboom/extras/modules/player/PlayerChat.java#L49C9-L81C26
    // with the hover event added
    public static final TextReplacementConfig URL_REPLACEMENT_CONFIG =
            TextReplacementConfig
                    .builder()
                    .match(Pattern
                            .compile("((https?://(ww(w|\\d)\\.)?|ww(w|\\d))[-a-zA-Z0-9@:%._+~#=]{1,256}"
                                    + "\\.[a-zA-Z0-9]{1,63}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*))"))
                    .replacement((b, c) -> {
                        if (c == null) {
                            return null;
                        }

                        if (b.groupCount() < 1) {
                            return null;
                        }

                        final String content = b.group(1);
                        final String url;

                    /*
                    Minecraft doesn't accept "www.google.com" as a URL
                    in click events
                     */
                        if (content.contains("://")) {
                            url = content;
                        } else {
                            url = "https://" + content;
                        }

                        return Component.text(content, NamedTextColor.BLUE)
                                .decorate(TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.openUrl(url))
                                .hoverEvent(
                                        HoverEvent.showText(
                                                Component
                                                        .text("Click here to open the URL")
                                                        .color(NamedTextColor.BLUE)
                                        )
                                );
                    })
                    .build();

    // component parsing
    // rewritten from chipmunkbot, a lot of stuff has changed, and also ANSI and section signs support, etc...
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

    public static String deserializeFromDiscordAnsi (String original) { return new ComponentParser().deserializeFromDiscordAnsi(original); }

    private static class ComponentParser {
        public static final Pattern ARG_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([s%])");

        public static final long MAX_TIME = 100; // this is actually more than we need

        public static final Map<String, String> ANSI_MAP = new HashMap<>();
        static {
            ANSI_MAP.put("0", "\u001b[38;2;0;0;0m");
            ANSI_MAP.put("1", "\u001b[38;2;0;0;170m");
            ANSI_MAP.put("2", "\u001b[38;2;0;170;0m");
            ANSI_MAP.put("3", "\u001b[38;2;0;170;170m");
            ANSI_MAP.put("4", "\u001b[38;2;170;0;0m");
            ANSI_MAP.put("5", "\u001b[38;2;170;0;170m");
            ANSI_MAP.put("6", "\u001b[38;2;255;170;0m");
            ANSI_MAP.put("7", "\u001b[38;2;170;170;170m");
            ANSI_MAP.put("8", "\u001b[38;2;85;85;85m");
            ANSI_MAP.put("9", "\u001b[38;2;85;85;255m");
            ANSI_MAP.put("a", "\u001b[38;2;85;255;85m");
            ANSI_MAP.put("b", "\u001b[38;2;85;255;255m");
            ANSI_MAP.put("c", "\u001b[38;2;255;85;85m");
            ANSI_MAP.put("d", "\u001b[38;2;255;85;255m");
            ANSI_MAP.put("e", "\u001b[38;2;255;255;85m");
            ANSI_MAP.put("f", "\u001b[38;2;255;255;255m");
            ANSI_MAP.put("l", "\u001b[1m");
            ANSI_MAP.put("o", "\u001b[3m");
            ANSI_MAP.put("n", "\u001b[4m");
            ANSI_MAP.put("m", "\u001b[9m");
            ANSI_MAP.put("k", "\u001b[6m");
            ANSI_MAP.put("r", "\u001b[0m");
        }

        public static final Map<String, String> DISCORD_ANSI_MAP = new HashMap<>();
        static {
            // map totallynotskidded™ from https://github.com/PrismarineJS/prismarine-chat/blob/master/index.js#L10

            // these bright colors have to come first because of deserialization
            DISCORD_ANSI_MAP.put("a", "\u001b[32m");
            DISCORD_ANSI_MAP.put("b", "\u001b[36m");
            DISCORD_ANSI_MAP.put("c", "\u001b[31m");
            DISCORD_ANSI_MAP.put("d", "\u001b[35m");
            DISCORD_ANSI_MAP.put("e", "\u001b[33m");
            DISCORD_ANSI_MAP.put("f", "\u001b[37m");

            DISCORD_ANSI_MAP.put("0", "\u001b[30m");
            DISCORD_ANSI_MAP.put("1", "\u001b[34m");
            DISCORD_ANSI_MAP.put("2", "\u001b[32m");
            DISCORD_ANSI_MAP.put("3", "\u001b[36m");
            DISCORD_ANSI_MAP.put("4", "\u001b[31m");
            DISCORD_ANSI_MAP.put("5", "\u001b[35m");
            DISCORD_ANSI_MAP.put("6", "\u001b[33m");
            DISCORD_ANSI_MAP.put("7", "\u001b[37m");
            DISCORD_ANSI_MAP.put("8", "\u001b[30m");
            DISCORD_ANSI_MAP.put("9", "\u001b[34m");

            DISCORD_ANSI_MAP.put("l", "\u001b[1m");
            DISCORD_ANSI_MAP.put("o", "\u001b[3m");
            DISCORD_ANSI_MAP.put("n", "\u001b[4m");
            DISCORD_ANSI_MAP.put("m", "\u001b[9m");
            DISCORD_ANSI_MAP.put("k", "\u001b[6m");
            DISCORD_ANSI_MAP.put("r", "\u001b[0m");
        }

        // we only focus on the discord ones that we made
        private static final Pattern DISCORD_ANSI_PATTERN = Pattern.compile("(\\u001b\\[\\d+m)");

        private ParseType type;

        private long parseStartTime = System.currentTimeMillis();

        private String lastStyle = "";

        private boolean isSubParsing = false;

        public String deserializeFromDiscordAnsi (String original) {
            final Matcher matcher = DISCORD_ANSI_PATTERN.matcher(original);
            final StringBuilder builder = new StringBuilder();

            while (matcher.find()) {
                final String match = matcher.group();

                boolean replaced = false;

                for (Map.Entry<String, String> entry : DISCORD_ANSI_MAP.entrySet()) {
                    if (!entry.getValue().equals(match)) continue;

                    matcher.appendReplacement(builder, "§" + entry.getKey());

                    replaced = true;

                    break;
                }

                if (!replaced) matcher.appendReplacement(builder, match);
            }

            matcher.appendTail(builder);

            return builder.toString();
        }

        private String stringify (Component message, ParseType type) {
            this.type = type;

            if (System.currentTimeMillis() > parseStartTime + MAX_TIME) return "";

            try {
                final StringBuilder builder = new StringBuilder();

                final String color = getColor(message.color());
                final String style = getStyle(message.style());

                final String output = stringifyPartially(message, color, style);

                builder.append(output);

                for (Component child : message.children()) {
                    final ComponentParser parser = new ComponentParser();
                    parser.lastStyle = lastStyle + color + style;
                    parser.parseStartTime = parseStartTime;
                    parser.isSubParsing = true;
                    builder.append(parser.stringify(child, type));
                }

                if (
                        !isSubParsing &&
                                (type == ParseType.ANSI || type == ParseType.DISCORD_ANSI)
                ) {
                    builder.append(ANSI_MAP.get("r"));
                }

                return builder.toString();
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
                default -> String.format("[Component type %s not implemented!]", message.getClass().getSimpleName());
            };
        }

        public String getStyle (Style textStyle) {
            if (textStyle == null) return "";

            StringBuilder style = new StringBuilder();

            for (Map.Entry<TextDecoration, TextDecoration.State> decorationEntry : textStyle.decorations().entrySet()) {
                final TextDecoration decoration = decorationEntry.getKey();
                final TextDecoration.State state = decorationEntry.getValue();

                if (state == TextDecoration.State.NOT_SET || state == TextDecoration.State.FALSE) continue;

                if (type == ParseType.ANSI || type == ParseType.DISCORD_ANSI) {
                    final Map<String, String> map = type == ParseType.ANSI ?
                            ANSI_MAP :
                            DISCORD_ANSI_MAP;

                    switch (decoration) {
                        case BOLD -> style.append(map.get("l"));
                        case ITALIC -> style.append(map.get("o"));
                        case OBFUSCATED -> style.append(map.get("k"));
                        case UNDERLINED -> style.append(map.get("n"));
                        case STRIKETHROUGH -> style.append(map.get("m"));
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

            // map totallynotskidded™ from https://github.com/PrismarineJS/prismarine-chat/blob/master/index.js#L299
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
                String ansiCode = type == ParseType.ANSI ?
                        ANSI_MAP.get(code) :
                        DISCORD_ANSI_MAP.get(code);
                if (ansiCode == null) {
                    if (type == ParseType.DISCORD_ANSI) {
                        // gets the closest color to the hex

                        final int rgb = Integer.parseInt(code.substring(1), 16);
                        final String chatColor = Character.toString(ColorUtilities.getClosestChatColor(rgb));

                        ansiCode = DISCORD_ANSI_MAP.get(chatColor);
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
            if (type == ParseType.ANSI || type == ParseType.DISCORD_ANSI) resetCode = ANSI_MAP.get("r");
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

                                if (!code.equals("r")) return type == ParseType.ANSI ?
                                        ANSI_MAP.get(code) :
                                        DISCORD_ANSI_MAP.get(code);
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
                parseStartTime++;
                if (matcher.group().equals("%%")) {
                    matcher.appendReplacement(sb, "%");
                } else {
                    final String idxStr = matcher.group(1);

                    try {
                        int idx = idxStr == null ? i++ : (Integer.parseInt(idxStr) - 1);

                        if (idx >= 0 && idx < message.arguments().size()) {
                            final ComponentParser parser = new ComponentParser();

                            parser.lastStyle = lastStyle + color + style;
                            parser.parseStartTime = parseStartTime;
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
                    } catch (NumberFormatException ignored) {} // is this a good idea?
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
