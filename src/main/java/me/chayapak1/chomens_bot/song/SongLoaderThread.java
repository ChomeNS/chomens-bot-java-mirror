package me.chayapak1.chomens_bot.song;

import it.unimi.dsi.fastutil.objects.ObjectList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.util.DownloadUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

// Author: _ChipMC_ & hhhzzzsss but modified
public class SongLoaderThread extends Thread {
    // should the converters be here?
    public static final List<Converter> converters = ObjectList.of(
            new MidiConverter(),
            new NBSConverter(),
            new TextFileConverter(),
            new SongPlayerConverter()
    );

    public final String fileName;

    private Path songPath;
    private URL songUrl;
    public SongLoaderException exception;
    public Song song;

    private final Bot bot;

    public final CommandContext context;

    private final boolean isUrl;

    private byte[] data;

    private boolean isItem = false;

    private boolean isFolder = false;

    public SongLoaderThread (final URL location, final Bot bot, final CommandContext context) {
        this.bot = bot;
        this.context = context;
        isUrl = true;
        songUrl = location;

        fileName = location.getFile();

        updateName();
    }

    public SongLoaderThread (final Path location, final Bot bot, final CommandContext context) {
        this.bot = bot;
        this.context = context;
        isUrl = false;
        songPath = location;

        isFolder = Files.isDirectory(songPath);

        fileName = location.getFileName().toString();

        updateName();
    }

    public SongLoaderThread (final byte[] data, final Bot bot, final CommandContext context) {
        this.bot = bot;
        this.context = context;
        this.data = data;
        this.isItem = true;
        this.isUrl = false;

        fileName = context.sender.profile.getName() + "'s song item";

        updateName();
    }

    private void updateName () {
        setName("SongLoaderThread for " + fileName);
    }

    @Override
    public void run () {
        if (isFolder && !isUrl && !isItem) {
            try (final Stream<Path> files = Files.list(songPath)) {
                files.forEach((file) -> {
                    songPath = file;
                    processFile();
                });

                showAddedToQueue();
            } catch (final IOException e) {
                bot.logger.error(e);
            }
        } else processFile();
    }

    private void processFile () {
        if (bot.music.songQueue.size() > 100) return;

        final byte[] bytes;
        final String name;
        try {
            if (isUrl) {
                bytes = DownloadUtilities.DownloadToByteArray(songUrl, 10 * 1024 * 1024);
                final Path fileName = Paths.get(songUrl.toURI().getPath()).getFileName();

                name = fileName == null ? "(root)" : fileName.toString();
            } else if (isItem) {
                bytes = data;
                name = context.sender.profile.getName() + "'s song item";
            } else {
                bytes = Files.readAllBytes(songPath);
                name = !isFolder ? fileName : songPath.getFileName().toString();
            }
        } catch (final Exception e) {
            exception = new SongLoaderException(Component.text(e.getMessage()));

            failed();

            return;
        }

        for (final Converter converter : converters) {
            if (song != null && !isFolder) break;

            try {
                song = converter.getSongFromBytes(bytes, name, bot);
            } catch (final Exception ignored) { }
        }

        if (song == null) {
            exception = new SongLoaderException(Component.translatable("commands.music.error.invalid_format"));

            failed();
        } else {
            song.context = context;

            bot.music.songQueue.add(song);

            if (!isFolder) showAddedToQueue();
        }

        bot.music.loaderThread = null;
    }

    private void showAddedToQueue () {
        if (isFolder) {
            bot.music.sendOutput(
                    context,
                    Component.translatable(
                            "commands.music.loading.added_folder_to_queue",
                            bot.colorPalette.defaultColor
                    )
            );
        } else {
            bot.music.sendOutput(
                    context,
                    Component.translatable(
                            "commands.music.loading.added_song_to_queue",
                            bot.colorPalette.defaultColor,
                            Component.empty()
                                    .append(Component.text(song.name, bot.colorPalette.secondary))
                    )
            );
        }
    }

    private void failed () {
        bot.music.sendOutput(
                context,
                Component.translatable(
                        "commands.music.error.loading_failed",
                        NamedTextColor.RED,
                        exception.message
                )
        );
        bot.music.loaderThread = null;
    }
}