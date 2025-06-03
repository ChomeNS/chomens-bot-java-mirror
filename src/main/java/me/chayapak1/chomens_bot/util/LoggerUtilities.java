package me.chayapak1.chomens_bot.util;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.logging.LogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtilities {
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static Component getPrefix (final Bot bot, final Component prefix, final Component message) {
        final LocalDateTime dateTime = LocalDateTime.now();

        final Component component;
        if (bot != null) {
            component = Component.translatable(
                    "[%s %s] [%s] [%s] %s",
                    NamedTextColor.DARK_GRAY,
                    Component.text(dateTime.format(dateTimeFormatter), NamedTextColor.GRAY),
                    prefix,
                    Component.text(Thread.currentThread().getName(), NamedTextColor.GRAY),
                    Component.text(bot.options.serverName, NamedTextColor.GRAY),
                    Component.empty().append(message.colorIfAbsent(NamedTextColor.WHITE))
            );
        } else {
            component = Component.translatable(
                    "[%s %s] [%s] %s",
                    NamedTextColor.DARK_GRAY,
                    Component.text(dateTime.format(dateTimeFormatter), NamedTextColor.GRAY),
                    prefix,
                    Component.text(Thread.currentThread().getName(), NamedTextColor.GRAY),
                    Component.empty().append(message.colorIfAbsent(NamedTextColor.WHITE))
            );
        }

        return component;
    }

    public static void log (final String message) { log(LogType.INFO, null, Component.text(message), true, true); }

    public static void log (final Component message) { log(LogType.INFO, null, message, true, true); }

    public static void log (final LogType type, final String message) { log(type, null, Component.text(message), true, true); }

    public static void log (final LogType type, final Component message) { log(type, null, message, true, true); }

    public static void log (final Bot bot, final Component message) { log(LogType.INFO, bot, message, true, true); }

    public static void log (final Bot bot, final String message) { log(LogType.INFO, bot, Component.text(message), true, true); }

    public static void log (final LogType type, final Bot bot, final Component message, final boolean logToFile, final boolean logToConsole) {
        final Component component = getPrefix(bot, type.component, message);

        if (logToConsole) print(ComponentUtilities.stringifyAnsi(component));

        if (logToFile) {
            final String formattedMessage = bot == null ? ComponentUtilities.stringify(message) :
                    String.format(
                            "[%s] %s",
                            bot.getServerString(true),
                            ComponentUtilities.stringify(message)
                    );

            FileLoggerUtilities.log(ComponentUtilities.stringify(type.component), formattedMessage);
        }
    }

    public static void error (final String message) { log(LogType.ERROR, message); }

    public static void error (final Component message) { log(LogType.ERROR, message); }

    public static void error (final Throwable throwable) { log(LogType.ERROR, ExceptionUtilities.getStacktrace(throwable)); }

    private static void print (final String message) {
        if (Main.console == null) System.out.println(message);
        else Main.console.reader.printAbove(message);
    }
}
