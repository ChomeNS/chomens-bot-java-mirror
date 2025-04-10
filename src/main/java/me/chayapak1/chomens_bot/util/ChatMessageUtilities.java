package me.chayapak1.chomens_bot.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Pattern;

public class ChatMessageUtilities {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer
            .legacySection()

            .toBuilder()

            .extractUrls(
                    Pattern
                            .compile("((https?://(ww(w|\\d)\\.)?|ww(w|\\d))[-a-zA-Z0-9@:%._+~#=]{1,256}"
                                             + "\\.[a-zA-Z0-9]{1,63}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*))"),
                    Style.style(
                            NamedTextColor.BLUE,
                            TextDecoration.UNDERLINED,
                            HoverEvent.showText(
                                    Component
                                            .text("Click here to open the URL")
                                            .color(NamedTextColor.BLUE)
                            )
                    )
            )

            // > `this.useTerriblyStupidHexFormat = true`
            // i know it is stupid indeed,
            // but i also want the compatibility to the kaboom chat
            .useUnusualXRepeatedCharacterHexFormat() // &x&1&2&3&4&5&6abc

            .build();

    public static Component applyChatMessageStyling (final String message) {
        return SERIALIZER.deserialize(message);
    }
}
