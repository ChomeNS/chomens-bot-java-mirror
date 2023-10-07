package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class InfoCommand extends Command {
    public InfoCommand () {
        super(
                "info",
                "Shows an info about various things",
                new String[] {
                        "<creator>",
                        "<discord>",
                        "<server>",
                        "<botuser>",
                        "<uptime>"
                },
                new String[] {},
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getString(false, true, true);

        switch (action) {
            case "creator" -> {
                return Component.empty()
                        .append(Component.text("ChomeNS Bot ").color(ColorUtilities.getColorByString(bot.config.colorPalette.primary)))
                        .append(Component.text("is created by ").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)))
                        .append(Component.text("chayapak").color(ColorUtilities.getColorByString(bot.config.colorPalette.ownerName)));
            }
            case "discord" -> {
                final String link = bot.config.discord.inviteLink;
                return Component.empty()
                        .append(Component.text("The Discord invite is ").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)))
                        .append(
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
                    RandomAccessFile file = new RandomAccessFile("/proc/cpuinfo", "r");
                    FileChannel channel = file.getChannel();

                    ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024); // 1 MB buffer
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
                } catch (IOException ignored) {}

                final TextColor color = ColorUtilities.getColorByString(bot.config.colorPalette.string);

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
                } catch (UnknownHostException ignored) {}

                component = Component.translatable(
                        """
                                Hostname: %s
                                Working directory: %s
                                OS architecture: %s
                                OS version: %s
                                OS name: %s
                                CPU cores: %s
                                CPU model: %s
                                Threads: %s
                                Heap memory usage: %s""",
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
                                ).color(color)
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary));

                return component;
            }
            case "botuser" -> {
                final String username = bot.username;
                final String uuid = bot.profile.getIdAsString();

                return Component.translatable(
                        "The bot's username is: %s and the UUID is: %s",
                        Component
                                .text(username)
                                .hoverEvent(
                                        HoverEvent.showText(
                                                Component
                                                        .text("Click here to copy the username to your clipboard")
                                                        .color(NamedTextColor.GREEN)
                                        )
                                )
                                .clickEvent(
                                        ClickEvent.copyToClipboard(username)
                                )
                                .color(ColorUtilities.getColorByString(bot.config.colorPalette.username)),
                        Component
                                .text(uuid)
                                .hoverEvent(
                                        HoverEvent.showText(
                                                Component
                                                        .text("Click here to copy the UUID to your clipboard")
                                                        .color(NamedTextColor.GREEN)
                                        )
                                )
                                .clickEvent(
                                        ClickEvent.copyToClipboard(uuid)
                                )
                                .color(ColorUtilities.getColorByString(bot.config.colorPalette.uuid))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "uptime" -> {
                final long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

                final long days = TimeUnit.SECONDS.toDays(uptime);
                final long hours = TimeUnit.SECONDS.toHours(uptime) - (days * 24L);
                final long minutes = TimeUnit.SECONDS.toMinutes(uptime) - (TimeUnit.SECONDS.toHours(uptime) * 60);
                final long seconds = TimeUnit.SECONDS.toSeconds(uptime) - (TimeUnit.SECONDS.toMinutes(uptime) * 60);

                return Component.translatable(
                        "The bots uptime is: %s",
                        Component.translatable(
                                "%s days, %s hours, %s minutes, %s seconds",
                                Component.text(days),
                                Component.text(hours),
                                Component.text(minutes),
                                Component.text(seconds)
                        ).color(NamedTextColor.GREEN)
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            default -> {
                throw new CommandException(Component.text("Invalid action"));
            }
        }
    }
}
