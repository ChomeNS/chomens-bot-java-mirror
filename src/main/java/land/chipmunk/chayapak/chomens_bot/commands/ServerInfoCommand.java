package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ServerInfoCommand extends Command {
    public String name = "serverinfo";

    public String description = "Shows the info about the server that is hosting the bot";

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

        return aliases;
    }

    public int trustLevel = 0;

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
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

        final String[] lines = builder.toString().split("\n");
        final Optional<String> modelName = Arrays.stream(lines)
                .filter(line -> line.startsWith("model name"))
                .findFirst();
        final Component cpuModel = modelName
                .map(s -> Component.text(s.split("\t: ")[1]).color(NamedTextColor.AQUA))
                .orElseGet(() -> Component.text("N/A").color(NamedTextColor.AQUA));

        try {
            component = Component.translatable(
                    """
                            Hostname: %s
                            Working directory: %s
                            OS architecture: %s
                            OS version: %s
                            OS name: %s
                            CPU cores: %s
                            CPU model: %s
                            Heap memory usage: %s""",
                    Component.text(InetAddress.getLocalHost().getHostName()).color(NamedTextColor.AQUA),
                    Component.text(System.getProperty("user.dir")).color(NamedTextColor.AQUA),
                    Component.text(os.getArch()).color(NamedTextColor.AQUA),
                    Component.text(os.getVersion()).color(NamedTextColor.AQUA),
                    Component.text(os.getName()).color(NamedTextColor.AQUA),
                    Component.text(String.valueOf(Runtime.getRuntime().availableProcessors())).color(NamedTextColor.AQUA),
                    cpuModel,
                    Component
                            .translatable(
                                    "%s MB / %s MB",
                                    Component.text(heapUsage.getUsed() / 1024L / 1024L),
                                    Component.text(heapUsage.getMax() / 1024L / 1024L)
                            ).color(NamedTextColor.AQUA)
            ).color(NamedTextColor.GOLD);

            return component;
        } catch (UnknownHostException ignored) {}

        return null;
    }
}
