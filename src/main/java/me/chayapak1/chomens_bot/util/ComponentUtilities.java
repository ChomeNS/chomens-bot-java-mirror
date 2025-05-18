package me.chayapak1.chomens_bot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.ComponentEncoder;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.ansi.ColorLevel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComponentUtilities {
    // component parsing
    // rewritten from chipmunkbot, a lot of stuff has changed, and also ANSI and section signs support, etc...
    public static final Map<String, String> LANGUAGE = new Object2ObjectOpenHashMap<>();

    private static final List<String> LANGUAGES = List.of("minecraftLanguage.json", "voiceChatLanguage.json");

    public static final Map<String, String> KEYBINDINGS = loadJsonStringMap("keybinds.json");

    static {
        for (final String language : LANGUAGES) {
            LANGUAGE.putAll(loadJsonStringMap(language));
        }
    }

    private static final Pattern ARG_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");
    private static final Pattern DISCORD_ANSI_PATTERN = Pattern.compile("(\\u001b\\[\\d+m)");
    private static final Pattern SECTION_SIGN_PATTERN = Pattern.compile("(§.)");

    private static final ThreadLocal<Integer> TOTAL_DEPTH = ThreadLocal.withInitial(() -> 0);

    private static final int MAX_DEPTH = 512; // same as adventure

    private static Map<String, String> loadJsonStringMap (final String name) {
        final Map<String, String> map = new Object2ObjectOpenHashMap<>();

        final InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
        assert is != null;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
            map.put(entry.getKey(), json.get(entry.getKey()).getAsString());
        }

        return map;
    }

    private static ComponentFlattener getFlattener (final boolean shouldReplaceSectionSignsWithANSI, final boolean isDiscord) {
        return ComponentFlattener.builder()
                .mapper(TextComponent.class, component -> mapText(component, shouldReplaceSectionSignsWithANSI, isDiscord))
                .complexMapper(KeybindComponent.class, ComponentUtilities::mapKeybind)
                .mapper(SelectorComponent.class, SelectorComponent::pattern)
                .complexMapper(TranslatableComponent.class, ComponentUtilities::mapTranslatable)
                .unknownMapper(component -> "<Unhandled component type: " + component.getClass().getSimpleName() + ">")
                .build();
    }

    private static final ANSIComponentSerializer TRUE_COLOR_ANSI_SERIALIZER = ANSIComponentSerializer.builder()
            .flattener(getFlattener(true, false))
            .colorLevel(ColorLevel.TRUE_COLOR)
            .build();

    private static final ANSIComponentSerializer DISCORD_ANSI_SERIALIZER = ANSIComponentSerializer.builder()
            .flattener(getFlattener(true, true))
            .colorLevel(ColorLevel.INDEXED_8)
            .build();

    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
            .flattener(getFlattener(false, false))
            .build();

    private static final PlainTextComponentSerializer PLAIN_TEXT_COMPONENT_SERIALIZER = PlainTextComponentSerializer.builder()
            .flattener(getFlattener(false, false))
            .build();

    public static String getOrReturnFallback (final TranslatableComponent component) {
        final String key = component.key();
        final String fallback = component.fallback();

        return LANGUAGE.getOrDefault(
                key,
                fallback != null ? fallback : key
        );
    }

    private static String guardedStringify (final ComponentEncoder<Component, String> serializer, final Component message) {
        try {
            return serializer.serialize(message);
        } catch (final Exception e) {
            return guardedStringify(
                    serializer,
                    Component.translatable(
                            "<Failed to parse component: %s>",
                            NamedTextColor.RED,
                            Component.text(e.toString())
                    )
            );
        } finally {
            TOTAL_DEPTH.set(0);
        }
    }

    public static String stringify (final Component message) {
        return guardedStringify(PLAIN_TEXT_COMPONENT_SERIALIZER, message);
    }

    public static String stringifyLegacy (final Component message) {
        return guardedStringify(LEGACY_COMPONENT_SERIALIZER, message);
    }

    public static String stringifyAnsi (final Component message) {
        return guardedStringify(TRUE_COLOR_ANSI_SERIALIZER, message);
    }

    public static String stringifyDiscordAnsi (final Component message) {
        return guardedStringify(DISCORD_ANSI_SERIALIZER, message)
                .replace("9", "3"); // we have to downscale because discord's ANSI doesn't have bright colors
    }

    public static String deserializeFromDiscordAnsi (final String original) {
        final Matcher matcher = DISCORD_ANSI_PATTERN.matcher(original);
        final StringBuilder builder = new StringBuilder();

        while (matcher.find()) {
            final String match = matcher.group();

            boolean replaced = false;

            final ANSIStyle[] values = ANSIStyle.values();

            for (final ANSIStyle value : values) {
                if (!value.discordAnsiCode.equals(match)) continue;
                matcher.appendReplacement(builder, "§" + value.legacyCode);
                replaced = true;
                break;
            }

            if (!replaced) matcher.appendReplacement(builder, match);
        }

        matcher.appendTail(builder);

        return builder.toString();
    }

    private static String mapText (
            final TextComponent component,
            final boolean shouldReplaceSectionSignsWithANSI,
            final boolean isDiscord
    ) {
        final String content = component.content();

        if (!shouldReplaceSectionSignsWithANSI || !content.contains("§")) {
            return component.content();
        } else {
            try {
                return SECTION_SIGN_PATTERN
                        .matcher(component.content())
                        .replaceAll(match -> {
                            final String code = match.group(0).substring(1);

                            final ANSIStyle[] values = ANSIStyle.values();

                            for (final ANSIStyle value : values) {
                                if (!code.equals(value.legacyCode)) continue;

                                return isDiscord
                                        ? value.discordAnsiCode
                                        : value.ansiCode;
                            }

                            return match.group(0);
                        });
            } catch (final Exception e) {
                return component.content();
            }
        }
    }

    private static void mapKeybind (final KeybindComponent component, final Consumer<Component> consumer) {
        consumer.accept(Component.translatable(KEYBINDINGS.getOrDefault(component.keybind(), component.keybind())));
    }

    private static void mapTranslatable (final TranslatableComponent component, final Consumer<Component> consumer) {
        final String format = getOrReturnFallback(component);

        final Matcher matcher = ARG_PATTERN.matcher(format);

        final List<Component> result = new ArrayList<>();

        // RIPPED straight from minecraft source code !!! :D
        try {
            int i = 0;

            int lastIndex = 0;

            while (matcher.find(lastIndex)) {
                final int start = matcher.start();
                final int end = matcher.end();

                if (start > lastIndex) {
                    final String formatSegment = format.substring(lastIndex, start);

                    if (formatSegment.indexOf('%') != -1) {
                        throw new IllegalArgumentException();
                    }

                    result.add(Component.text(formatSegment));
                }

                final String full = format.substring(start, end);

                if (matcher.group().equals("%") && full.equals("%%")) {
                    result.add(Component.text('%'));
                } else if (matcher.group(2).equals("s")) {
                    final String idxStr = matcher.group(1);

                    final int idx = idxStr == null ? i++ : (Integer.parseInt(idxStr) - 1);

                    if (idx < 0 || idx > component.arguments().size())
                        throw new IllegalArgumentException();

                    final int currentTotalDepth = TOTAL_DEPTH.get();

                    if (currentTotalDepth > MAX_DEPTH) return;

                    TOTAL_DEPTH.set(currentTotalDepth + 1);

                    result.add(
                            component.arguments()
                                    .get(idx)
                                    .asComponent()
                    );
                } else {
                    throw new IllegalArgumentException();
                }

                lastIndex = end;
            }

            if (lastIndex < format.length()) {
                final String remaining = format.substring(lastIndex);

                if (remaining.indexOf('%') != -1) {
                    throw new IllegalArgumentException();
                }

                result.add(Component.text(remaining));
            }
        } catch (final Exception e) {
            result.clear();
            result.add(Component.text(format));
        }

        consumer.accept(Component.join(JoinConfiguration.noSeparators(), result));
    }

    public enum ANSIStyle {
        GREEN("a", "\u001b[38;2;85;255;85m", "\u001b[32m"),
        AQUA("b", "\u001b[38;2;85;255;255m", "\u001b[36m"),
        RED("c", "\u001b[38;2;255;85;85m", "\u001b[31m"),
        LIGHT_PURPLE("d", "\u001b[38;2;255;85;255m", "\u001b[35m"),
        YELLOW("e", "\u001b[38;2;255;255;85m", "\u001b[33m"),
        WHITE("f", "\u001b[38;2;255;255;255m", "\u001b[37m"),
        BLACK("0", "\u001b[38;2;0;0;0m", "\u001b[30m"),
        DARK_RED("1", "\u001b[38;2;0;0;170m", "\u001b[34m"),
        DARK_GREEN("2", "\u001b[38;2;0;170;0m", "\u001b[32m"),
        GOLD("3", "\u001b[38;2;0;170;170m", "\u001b[36m"),
        DARK_BLUE("4", "\u001b[38;2;170;0;0m", "\u001b[31m"),
        DARK_PURPLE("5", "\u001b[38;2;170;0;170m", "\u001b[35m"),
        DARK_AQUA("6", "\u001b[38;2;255;170;0m", "\u001b[33m"),
        GRAY("7", "\u001b[38;2;170;170;170m", "\u001b[37m"),
        DARK_GRAY("8", "\u001b[38;2;85;85;85m", "\u001b[30m"),
        BLUE("9", "\u001b[38;2;85;85;255m", "\u001b[34m"),
        BOLD("l", "\u001b[1m", "\u001b[1m"),
        ITALIC("o", "\u001b[3m", "\u001b[3m"),
        UNDERLINED("n", "\u001b[4m", "\u001b[4m"),
        STRIKETHROUGH("m", "\u001b[9m", "\u001b[9m"),
        OBFUSCATED("k", "\u001b[6m", "\u001b[6m"),
        RESET("r", "\u001b[0m", "\u001b[0m");

        private final String legacyCode;
        private final String ansiCode;
        private final String discordAnsiCode;

        ANSIStyle (
                final String legacyCode,
                final String ansiCode,
                final String discordAnsiCode
        ) {
            this.legacyCode = legacyCode;
            this.ansiCode = ansiCode;
            this.discordAnsiCode = discordAnsiCode;
        }
    }
}
