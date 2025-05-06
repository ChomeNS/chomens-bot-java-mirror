package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.util.FileLoggerUtilities;
import me.chayapak1.chomens_bot.util.StringUtilities;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class GrepLogPlugin {
    private final Bot bot;

    public Pattern pattern;

    private int count = 0;

    public boolean running = false;

    public GrepLogPlugin (final Bot bot) {
        this.bot = bot;
    }

    public void search (final CommandContext context, final String input, final boolean ignoreCase, final boolean regex) throws CommandException {
        running = true;

        try (final Stream<Path> files = Files.list(FileLoggerUtilities.logDirectory)) {
            final Path[] fileList = files.toArray(Path[]::new);

            Arrays.sort(fileList, Comparator.comparing(a -> a.getFileName().toString()));

            final StringBuilder result = new StringBuilder();

            for (final Path filePath : fileList) {
                if (!running) {
                    pattern = null;
                    return;
                }

                if (count > 1_000_000) break;

                final String fileName = filePath.getFileName().normalize().toString();
                final String absolutePath = filePath.toAbsolutePath().normalize().toString();

                if (fileName.endsWith(".txt.gz")) {
                    try (
                            final GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(absolutePath));
                            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8))
                    ) {
                        result.append(process(bufferedReader, input, ignoreCase, regex));
                    }
                } else if (fileName.endsWith(".txt")) {
                    try (
                            final FileInputStream fileInputStream = new FileInputStream(absolutePath);
                            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8))
                    ) {
                        result.append(process(bufferedReader, input, ignoreCase, regex));
                    }
                }
            }

            // ? should this be here? (see `process` function below)
            pattern = null;

            count = 0;

            final String stringifiedResult = result.toString();

            final long matches = stringifiedResult.lines().count();

            if (matches == 0) throw new CommandException(Component.translatable("commands.greplog.error.no_matches_found"));

            final String channelId = Main.discord.servers.get(bot.getServerString(true));
            final TextChannel logChannel = Main.discord.jda.getTextChannelById(channelId);

            if (logChannel == null) return;

            logChannel
                    .sendMessage("Greplog result:")
                    .addFiles(
                            FileUpload.fromData(
                                    // as of the time writing this, discord has an 8 MB file size limit for bots
                                    StringUtilities.truncateToFitUtf8ByteLength(stringifiedResult, 8 * 1000 * 1000).getBytes(StandardCharsets.UTF_8),
                                    String.format("result-%d.txt", System.currentTimeMillis() / 1000)
                            )
                    )
                    .queue(message -> {
                        final String url = message.getAttachments().getFirst().getUrl();

                        final DecimalFormat formatter = new DecimalFormat("#,###");

                        final Component component = Component.translatable("commands.greplog.found")
                                .color(bot.colorPalette.defaultColor)
                                .arguments(
                                        Component.text(formatter.format(matches)).color(bot.colorPalette.number),
                                        Component.text(input).color(bot.colorPalette.string),
                                        Component
                                                .translatable("commands.greplog.here")
                                                .color(NamedTextColor.GREEN)
                                                .clickEvent(ClickEvent.openUrl(url))
                                );

                        context.sendOutput(component);
                    });
        } catch (final CommandException e) {
            running = false;
            throw e;
        } catch (final FileNotFoundException e) {
            running = false;
            throw new CommandException(Component.text("File not found"));
        } catch (final NotDirectoryException e) {
            running = false;
            throw new CommandException(Component.text("Logger directory is not a directory"));
        } catch (final PatternSyntaxException e) {
            running = false;
            throw new CommandException(Component.text("Pattern is invalid"));
        } catch (final IOException e) {
            running = false;
            throw new CommandException(Component.text("An I/O error has occurred"));
        } catch (final Exception e) {
            bot.logger.error(e);
        }

        running = false;
    }

    private StringBuilder process (final BufferedReader bufferedReader, final String input, final boolean ignoreCase, final boolean regex) throws IOException, PatternSyntaxException {
        if (regex && pattern == null) {
            if (ignoreCase)
                pattern = Pattern.compile(input, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
            else
                pattern = Pattern.compile(input, Pattern.UNICODE_CHARACTER_CLASS);
        }

        final StringBuilder result = new StringBuilder();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (
                    (regex && pattern.matcher(line).find()) || // *greplog -regex text OR *greplog -ignorecase -regex text
                            (!ignoreCase && !regex && line.contains(input)) || // *greplog text
                            (ignoreCase && StringUtilities.containsIgnoreCase(line, input)) // *greplog -ignorecase
            ) {
                result.append(line).append("\n");

                count++;
            }
        }

        return result;
    }
}
