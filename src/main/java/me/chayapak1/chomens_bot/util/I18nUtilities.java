package me.chayapak1.chomens_bot.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class I18nUtilities {
    private static final Path USER_LANGUAGE_PATH = Path.of("language.json");

    private static final Map<String, String> LANGUAGE;

    static {
        if (Files.exists(USER_LANGUAGE_PATH)) {
            try {
                LANGUAGE = loadLanguageFromFile();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            LANGUAGE = loadLanguage();
        }
    }

    private static Map<String, String> loadLanguage () {
        final Map<String, String> map = new Object2ObjectOpenHashMap<>();

        final InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("language-en_us.json");
        assert is != null;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        final JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
            map.put(entry.getKey(), json.get(entry.getKey()).getAsString());
        }

        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> loadLanguageFromFile () throws IOException {
        final Map<String, String> map = new Object2ObjectOpenHashMap<>();

        final InputStream output = Files.newInputStream(USER_LANGUAGE_PATH);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(output));

        final JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

        for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
            map.put(entry.getKey(), json.get(entry.getKey()).getAsString());
        }

        return Collections.unmodifiableMap(map);
    }

    public static String get (final String key) {
        return LANGUAGE.get(key);
    }

    public static Component render (final Component original) {
        final List<Component> renderedChildren = new ArrayList<>();
        final List<TranslationArgument> renderedArguments = new ArrayList<>();

        for (final Component child : original.children()) {
            final Component rendered = render(child);
            renderedChildren.add(rendered);
        }

        if (original instanceof final TranslatableComponent translatableComponent) {
            for (final TranslationArgument argument : translatableComponent.arguments()) {
                final Component rendered = render(argument.asComponent());
                renderedArguments.add(TranslationArgument.component(rendered));
            }
        }

        final HoverEvent<?> hoverEvent = original.hoverEvent();
        final HoverEventSource<Component> hoverEventSource;
        if (
                hoverEvent != null
                        && hoverEvent.action() == HoverEvent.Action.SHOW_TEXT
                        && hoverEvent.value() instanceof final Component hoverComponent
        ) {
            final Component rendered = render(hoverComponent);
            hoverEventSource = HoverEvent.showText(rendered);
        } else {
            hoverEventSource = null;
        }

        if (original instanceof final TranslatableComponent translatableComponent) {
            final String translated = get(translatableComponent.key());

            return translatableComponent
                    .key(
                            (translated == null ? "" : "chomens_bot.") + // sometimes we can collide with minecraft's
                                    translatableComponent.key()
                    )
                    .fallback(translated)
                    .children(renderedChildren)
                    .arguments(renderedArguments)
                    .hoverEvent(hoverEventSource);
        } else {
            return original
                    .children(renderedChildren)
                    .hoverEvent(hoverEventSource);
        }
    }
}
