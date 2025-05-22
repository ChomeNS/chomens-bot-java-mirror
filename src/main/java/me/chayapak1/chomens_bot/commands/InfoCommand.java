package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.util.StringUtilities;
import me.chayapak1.chomens_bot.util.TimeUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class InfoCommand extends Command {
    public static final String ORIGINAL_REPOSITORY_URL = "https://code.chipmunk.land/ChomeNS/chomens-bot-java";

    public static final Properties BUILD_INFO = new Properties();

    static {
        try (final InputStream input = ClassLoader.getSystemClassLoader().getResourceAsStream("application.properties")) {
            BUILD_INFO.load(input);
        } catch (final IOException ignored) { }
    }

    public InfoCommand () {
        super(
                "info",
                new String[] {
                        "",
                        "creator",
                        "discord",
                        "server",
                        "botuser",
                        "botlogintime",
                        "uptime"
                },
                new String[] { "creator", "discord", "botuser", "botlogintime", "uptime" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;

        final String action = !context.userInputCommandName.equalsIgnoreCase(this.name) // if the input command is not `info`
                ? context.userInputCommandName.toLowerCase() // use that as the action (e.g. "discord", "creator")
                : context.getString(false, false, true); // else just take the argument of `info`

        switch (action) {
            case "creator" -> {
                return Component.translatable(
                        "commands.info.creator.output",
                        bot.colorPalette.defaultColor,
                        Component.text("ChomeNS Bot", bot.colorPalette.primary),
                        Component.text("chayapak", bot.colorPalette.ownerName)
                );
            }
            case "discord" -> {
                final String link = bot.config.discord.inviteLink;

                return Component.translatable(
                        "commands.info.discord.output",
                        bot.colorPalette.defaultColor,
                        Component
                                .text(link)
                                .clickEvent(ClickEvent.openUrl(link))
                                .color(NamedTextColor.BLUE)
                );
            }
            case "server" -> {
                // totallynotskidded™ from extras' serverinfo
                final Component component;

                final MemoryUsage heapUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

                final StringBuilder builder = new StringBuilder();

                try {
                    final RandomAccessFile file = new RandomAccessFile("/proc/cpuinfo", "r");
                    final FileChannel channel = file.getChannel();

                    final ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024); // 1 MB buffer
                    long bytesRead = channel.read(buffer);

                    while (bytesRead != -1) {
                        buffer.flip();

                        while (buffer.hasRemaining()) {
                            builder.append((char) buffer.get());
                        }

                        buffer.clear();
                        bytesRead = channel.read(buffer);
                    }

                    channel.close();
                    file.close();
                } catch (final IOException ignored) { }

                final TextColor color = bot.colorPalette.string;

                final String[] lines = builder.toString().split("\n");
                final Optional<String> modelName = Arrays.stream(lines)
                        .filter(line -> line.startsWith("model name"))
                        .findFirst();
                final Component cpuModel = modelName
                        .map(s -> Component.text(s.split("\t: ")[1]).color(color))
                        .orElseGet(() -> Component.text("N/A").color(color));

                InetAddress localHost = null;

                try {
                    localHost = InetAddress.getLocalHost();
                } catch (final UnknownHostException ignored) { }

                component = Component.translatable(
                        "commands.info.server.output",
                        bot.colorPalette.secondary,
                        Component.text(localHost == null ? "N/A" : localHost.getHostName()).color(color),
                        Component.text(System.getProperty("user.dir")).color(color),
                        Component.text(os.getArch()).color(color),
                        Component.text(os.getVersion()).color(color),
                        Component.text(os.getName()).color(color),
                        Component.text(String.valueOf(Runtime.getRuntime().availableProcessors())).color(color),
                        cpuModel,
                        Component.text(String.valueOf(Thread.activeCount())).color(color),
                        Component
                                .translatable(
                                        "%s MB / %s MB",
                                        Component.text(heapUsage.getUsed() / 1024L / 1024L),
                                        Component.text(heapUsage.getMax() / 1024L / 1024L)
                                ).color(color),
                        Component
                                .translatable(
                                        "%s MB / %s MB",
                                        Component.text((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L / 1024L),
                                        Component.text(Runtime.getRuntime().totalMemory() / 1024L / 1024L)
                                ).color(color)
                );

                return component;
            }
            case "botuser" -> {
                final String username = bot.username;
                final String uuid = bot.profile.getIdAsString();

                return Component.translatable(
                        "commands.info.botuser.output",
                        bot.colorPalette.defaultColor,
                        Component
                                .text(username, bot.colorPalette.username)
                                .hoverEvent(
                                        HoverEvent.showText(
                                                Component.translatable(
                                                        "commands.generic.click_to_copy_username",
                                                        NamedTextColor.GREEN
                                                )
                                        )
                                )
                                .clickEvent(ClickEvent.copyToClipboard(username)),
                        Component
                                .text(uuid, bot.colorPalette.uuid)
                                .hoverEvent(
                                        HoverEvent.showText(
                                                Component.translatable(
                                                        "commands.generic.click_to_copy_uuid",
                                                        NamedTextColor.GREEN
                                                )
                                        )
                                )
                                .clickEvent(ClickEvent.copyToClipboard(uuid))
                );
            }
            case "botlogintime" -> {
                final long loginTime = bot.loginTime;

                final ZoneId zoneId = ZoneId.of("UTC");

                final String formattedLoginTime = TimeUtilities.formatTime(
                        loginTime,
                        "MMMM d, yyyy, hh:mm:ss a Z",
                        zoneId
                );

                final DateFormat timeSinceFormatter = new SimpleDateFormat("HH 'hours' mm 'minutes' ss 'seconds'");
                timeSinceFormatter.setTimeZone(TimeZone.getTimeZone(zoneId)); // very important
                final String formattedTimeSince = timeSinceFormatter.format(new Date(System.currentTimeMillis() - loginTime));

                return Component.translatable(
                        "commands.info.botlogintime.output",
                        bot.colorPalette.defaultColor,
                        Component.text(formattedLoginTime, bot.colorPalette.string),
                        Component.text(formattedTimeSince, bot.colorPalette.string)
                );
            }
            case "uptime" -> {
                final long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

                final long days = TimeUnit.SECONDS.toDays(uptime);
                final long hours = TimeUnit.SECONDS.toHours(uptime) - (days * 24L);
                final long minutes = TimeUnit.SECONDS.toMinutes(uptime) - (TimeUnit.SECONDS.toHours(uptime) * 60);
                final long seconds = TimeUnit.SECONDS.toSeconds(uptime) - (TimeUnit.SECONDS.toMinutes(uptime) * 60);

                return Component.translatable(
                        "commands.info.uptime.output",
                        bot.colorPalette.defaultColor,
                        Component.translatable(
                                "%s %s, %s %s, %s %s, %s %s",
                                NamedTextColor.GREEN,
                                Component.text(days),
                                Component.text(StringUtilities.addPlural(days, "day")),
                                Component.text(hours),
                                Component.text(StringUtilities.addPlural(hours, "hour")),
                                Component.text(minutes),
                                Component.text(StringUtilities.addPlural(minutes, "minute")),
                                Component.text(seconds),
                                Component.text(StringUtilities.addPlural(seconds, "second"))
                        )
                );
            }
            default -> {
                return Component
                        .translatable(
                                "commands.info.default.main_output",
                                bot.colorPalette.defaultColor,
                                Component.text("ChomeNS Bot").color(NamedTextColor.YELLOW),
                                Component
                                        .text("Kaboom")
                                        .style(
                                                Style.style()
                                                        .color(NamedTextColor.GRAY)
                                                        .decorate(TextDecoration.BOLD)
                                        )
                        )
                        .append(Component.newline())
                        .append(
                                Component
                                        .translatable(
                                                "commands.info.default.original_repository",
                                                bot.colorPalette.defaultColor,
                                                Component
                                                        .text(ORIGINAL_REPOSITORY_URL)
                                                        .color(bot.colorPalette.string)
                                                        .clickEvent(
                                                                ClickEvent.openUrl(ORIGINAL_REPOSITORY_URL)
                                                        )
                                        )
                        )
                        .append(Component.newline())
                        .append(
                                Component
                                        .translatable(
                                                "commands.info.default.compiled_at",
                                                bot.colorPalette.defaultColor,
                                                Component
                                                        .text(BUILD_INFO.getProperty("build.date", "unknown"))
                                                        .color(bot.colorPalette.string)
                                        )
                        )
                        .append(
                                Component
                                        .translatable(
                                                "commands.info.default.git_commit",
                                                bot.colorPalette.defaultColor,
                                                Component
                                                        .text(BUILD_INFO.getProperty("build.git.commit.hash", "unknown"))
                                                        .color(bot.colorPalette.string),
                                                Component
                                                        .text(BUILD_INFO.getProperty("build.git.commit.count", "unknown"))
                                                        .color(bot.colorPalette.number)
                                        )
                        )
                        .append(Component.newline())
                        .append(
                                Component
                                        .translatable(
                                                "commands.info.default.build",
                                                bot.colorPalette.defaultColor,
                                                Component
                                                        .text(BUILD_INFO.getProperty("build.number", "unknown"))
                                                        .color(bot.colorPalette.number)
                                        )
                        );
            }
        }
    }
}
