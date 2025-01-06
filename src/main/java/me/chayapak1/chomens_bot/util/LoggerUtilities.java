package me.chayapak1.chomens_bot.util;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtilities {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // function ported from chomens bot js (well, modified)
    private static String prefix (Bot bot, Component prefix, String _message) {
        final LocalDateTime dateTime = LocalDateTime.now();

        Component message;
        if (bot != null) {
            message = Component.translatable(
                    "[%s %s] [%s] [%s] %s",
                    Component.text(dateTime.format(dateTimeFormatter)).color(NamedTextColor.GRAY),
                    prefix,
                    Component.text(Thread.currentThread().getName()).color(NamedTextColor.GRAY),
                    Component.text(bot.options.serverName).color(NamedTextColor.GRAY),
                    Component.text(_message).color(NamedTextColor.WHITE)
            ).color(NamedTextColor.DARK_GRAY);
        } else {
            message = Component.translatable(
                    "[%s %s] [%s] %s",
                    Component.text(dateTime.format(dateTimeFormatter)).color(NamedTextColor.GRAY),
                    prefix,
                    Component.text(Thread.currentThread().getName()).color(NamedTextColor.GRAY),
                    Component.text(_message).color(NamedTextColor.WHITE)
            ).color(NamedTextColor.DARK_GRAY);
        }

        return ComponentUtilities.stringifyAnsi(message);
    }

    public static void log (String message) { log(null, message, true, true); }
    public static void log (Component message) { log(null, message); }
    public static void log (Bot bot, Component message) { log(bot, ComponentUtilities.stringifyAnsi(message)); }
    public static void log (Bot bot, String message) { log(bot, message, true, true); }
    public static void log (Bot bot, String message, boolean logToFile, boolean logToConsole) {
        final String component = prefix(bot, Component.text("Log").color(NamedTextColor.GOLD), message);

        if (logToConsole && bot != null) bot.console.reader.printAbove(component);
        else if (logToConsole) System.out.println(component);

        if (logToFile) {
            final String formattedMessage = bot == null ? "" :
                    String.format(
                        "[%s] %s",
                        bot.host + ":" + bot.port,
                        message
                    );

            FileLoggerUtilities.log(formattedMessage);
        }
    }

    public static void info (String message) { info(null, message); }
    public static void info (Component message) { info(null, message); }
    public static void info (Bot bot, Component message) { info(bot, ComponentUtilities.stringifyAnsi(message)); }
    public static void info (Bot bot, String message) {
        final String component = prefix(bot, Component.text("Info").color(NamedTextColor.GREEN), message);

        Main.console.reader.printAbove(component);
    }

    public static void error (String message) { error(null, message); }
    public static void error (Component message) { error(null, message); }
    public static void error (Throwable throwable) { error(null, throwable); }
    public static void error (Bot bot, Component message) { error(bot, ComponentUtilities.stringifyAnsi(message)); }
    public static void error (Bot bot, Throwable throwable) { error(bot, ExceptionUtilities.getStacktrace(throwable)); }
    public static void error (Bot bot, String message) {
        final String component = prefix(bot, Component.text("Error").color(NamedTextColor.RED), message);

        Main.console.reader.printAbove(component);
    }

    public static void custom (Component prefix, Component message) { custom(null, prefix, message); }
    public static void custom (Bot bot, Component prefix, Component _message) {
        final String message = prefix(bot, prefix, ComponentUtilities.stringifyAnsi(_message));

        Main.console.reader.printAbove(message);
    }
}
