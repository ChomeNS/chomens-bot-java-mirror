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

    private static Map<String, String> loadJsonStringMap (final String name) {
        final Map<String, String> map = new HashMap<>();

        final InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
        assert is != null;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
            map.put(entry.getKey(), json.get(entry.getKey()).getAsString());
        }

        return map;
    }

    public static String getOrReturnFallback (final TranslatableComponent component) {
        final String key = component.key();

        final String minecraftKey = LANGUAGE.get(key);
        final String voiceChatKey = VOICE_CHAT_LANGUAGE.get(key);

        if (minecraftKey != null) return minecraftKey;
        else if (voiceChatKey != null) return voiceChatKey;
        else return component.fallback() != null ? component.fallback() : key;
    }

    public static String stringify (final Component message) { return new ComponentParser().stringify(message, ComponentParser.ParseType.PLAIN); }

    public static String stringifySectionSign (final Component message) { return new ComponentParser().stringify(message, ComponentParser.ParseType.SECTION_SIGNS); }

    public static String stringifyAnsi (final Component message) { return new ComponentParser().stringify(message, ComponentParser.ParseType.ANSI); }

    public static String stringifyDiscordAnsi (final Component message) { return new ComponentParser().stringify(message, ComponentParser.ParseType.DISCORD_ANSI); }

    public static String deserializeFromDiscordAnsi (final String original) { return new ComponentParser().deserializeFromDiscordAnsi(original); }

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

        public static final Map<NamedTextColor, String> NAMED_TEXT_COLOR_MAP = new HashMap<>();

        static {
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.BLACK, "0");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.DARK_BLUE, "1");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.DARK_GREEN, "2");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.DARK_AQUA, "3");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.DARK_RED, "4");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.DARK_PURPLE, "5");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.GOLD, "6");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.GRAY, "7");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.DARK_GRAY, "8");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.BLUE, "9");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.GREEN, "a");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.AQUA, "b");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.RED, "c");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.LIGHT_PURPLE, "d");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.YELLOW, "e");
            NAMED_TEXT_COLOR_MAP.put(NamedTextColor.WHITE, "f");
        }

        // we only focus on the discord ones that we made
        private static final Pattern DISCORD_ANSI_PATTERN = Pattern.compile("(\\u001b\\[\\d+m)");

        private ParseType type;

        private long parseStartTime = System.currentTimeMillis();

        private String lastColor = "";
        private String lastStyle = "";

        private boolean isSubParsing = false;

        public String deserializeFromDiscordAnsi (final String original) {
            final Matcher matcher = DISCORD_ANSI_PATTERN.matcher(original);
            final StringBuilder builder = new StringBuilder();

            while (matcher.find()) {
                final String match = matcher.group();

                boolean replaced = false;

                for (final Map.Entry<String, String> entry : DISCORD_ANSI_MAP.entrySet()) {
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

        private String stringify (final Component message, final ParseType type) {
            this.type = type;

            if (System.currentTimeMillis() > parseStartTime + MAX_TIME) return "";

            try {
                final StringBuilder builder = new StringBuilder();

                final String color = getColor(message.color());
                final String style = getStyle(message.style());

                final String output = stringifyPartially(message, color, style);

                builder.append(output);

                for (final Component child : message.children()) {
                    final ComponentParser parser = new ComponentParser();
                    parser.lastColor = lastColor + color;
                    parser.lastStyle = lastStyle + style;
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
            } catch (final Exception e) {
                LoggerUtilities.error(e);
                return "";
            }
        }

        public String stringifyPartially (final Component message, final String color, final String style) {
            return switch (message) {
                case final TextComponent t_component -> stringifyPartially(t_component, color, style);
                case final TranslatableComponent t_component -> stringifyPartially(t_component, color, style);
                case final SelectorComponent t_component -> stringifyPartially(t_component, color, style);
                case final KeybindComponent t_component -> stringifyPartially(t_component, color, style);
                default -> String.format("[Component type %s not implemented!]", message.getClass().getSimpleName());
            };
        }

        public String getStyle (final Style textStyle) {
            if (textStyle == null) return "";

            final StringBuilder style = new StringBuilder();

            for (final Map.Entry<TextDecoration, TextDecoration.State> decorationEntry : textStyle.decorations().entrySet()) {
                final TextDecoration decoration = decorationEntry.getKey();
                final TextDecoration.State state = decorationEntry.getValue();

                if (state == TextDecoration.State.NOT_SET) continue;

                if (type != ParseType.PLAIN) {
                    final Map<String, String> styleMap =
                            type == ParseType.ANSI ? ANSI_MAP : DISCORD_ANSI_MAP;

                    final String key = switch (decoration) {
                        case BOLD -> "l";
                        case ITALIC -> "o";
                        case UNDERLINED -> "n";
                        case STRIKETHROUGH -> "m";
                        case OBFUSCATED -> "k";
                    };

                    final String code = type == ParseType.SECTION_SIGNS ?
                            "§" + key :
                            styleMap.getOrDefault(key, "");

                    if (state == TextDecoration.State.TRUE) {
                        style.append(code);
                    } else {
                        // state is FALSE, meaning that the component HAS SPECIFIED the style to be empty

                        if (!lastStyle.isEmpty()) lastStyle = lastStyle.replace(code, "");

                        if (type == ParseType.SECTION_SIGNS) {
                            style
                                    .append(getResetCode())
                                    .append(lastColor)
                                    .append(lastStyle);
                        } else {
                            style
                                    .append(getResetCode())
                                    .append(lastStyle)
                                    .append(lastColor);
                        }
                    }
                }
            }

            return style.toString();
        }

        public String getColor (final TextColor color) {
            if (color == null) return "";

            // map totallynotskidded™ from https://github.com/PrismarineJS/prismarine-chat/blob/master/index.js#L299
            final String code;
            if (color instanceof final NamedTextColor named) {
                code = NAMED_TEXT_COLOR_MAP.getOrDefault(named, "");
            } else {
                code = color.asHexString();
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
            }

            return "";
        }

        private String getResetCode () {
            return switch (type) {
                case SECTION_SIGNS -> "§r";
                case ANSI, DISCORD_ANSI -> ANSI_MAP.get("r");
                default -> "";
            };
        }

        private String getPartialResult (final String originalResult, final String color, final String style, final boolean setLastStyles) {
            if (type == ParseType.PLAIN) return originalResult;

            final String orderedStyling = type == ParseType.SECTION_SIGNS
                    ? lastColor + lastStyle + color + style
                    : lastStyle + lastColor + style + color;

            final String result =
                    orderedStyling +
                            originalResult +
                            (
                                    !color.isEmpty() || !style.isEmpty()
                                            ? getResetCode()
                                            : ""
                            );

            if (setLastStyles) {
                lastColor = color;
                lastStyle = style;
            }

            return result;
        }

        private String stringifyPartially (final String message, final String color, final String style) {
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
                } catch (final Exception ignored) { }
            }

            return getPartialResult(replacedContent, color, style, true);
        }

        private String stringifyPartially (final TextComponent message, final String color, final String style) {
            return stringifyPartially(message.content(), color, style);
        }

        private String stringifyPartially (final TranslatableComponent message, final String color, final String style) {
            final String format = getOrReturnFallback(message);

            // totallynotskidded™️from HBot (and changed a bit)
            final Matcher matcher = ARG_PATTERN.matcher(format);
            final StringBuilder sb = new StringBuilder();

            // not checking if arguments length equals input format length
            // is INTENTIONAL and is a FEATURE
            int i = 0;
            while (matcher.find()) {
                if (matcher.group().equals("%%")) {
                    matcher.appendReplacement(sb, "%");
                } else {
                    final String idxStr = matcher.group(1);

                    try {
                        final int idx = idxStr == null ? i++ : (Integer.parseInt(idxStr) - 1);

                        if (idx >= 0 && idx < message.arguments().size()) {
                            final ComponentParser parser = new ComponentParser();

                            parser.lastColor = lastColor + color;
                            parser.lastStyle = lastStyle + style;
                            parser.parseStartTime = parseStartTime;
                            parser.isSubParsing = true;

                            final String orderedStyling = type == ParseType.SECTION_SIGNS
                                    ? lastColor + lastStyle + color + style
                                    : lastStyle + lastColor + style + color;

                            matcher.appendReplacement(
                                    sb,
                                    Matcher.quoteReplacement(
                                            getPartialResult(
                                                    parser.stringify(
                                                            message.arguments()
                                                                    .get(idx)
                                                                    .asComponent(),
                                                            type
                                                    ),
                                                    color,
                                                    style,
                                                    false
                                            ) + orderedStyling
                                    )
                            );
                        } else {
                            matcher.appendReplacement(sb, "");
                        }
                    } catch (final NumberFormatException ignored) { } // is this a good idea?
                }
            }

            matcher.appendTail(sb);

            return getPartialResult(sb.toString(), color, style, true);
        }

        // on the client side, this acts just like TextComponent
        // and does NOT process any players stuff
        private String stringifyPartially (final SelectorComponent message, final String color, final String style) {
            return stringifyPartially(message.pattern(), style, color);
        }

        public String stringifyPartially (final KeybindComponent message, final String color, final String style) {
            final String keybind = message.keybind();

            // this is indeed the correct way to process keybindings
            // inside the minecraft code, silly past me was wrong :D
            final Component component = Component.translatable(KEYBINDINGS.getOrDefault(keybind, keybind));

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
