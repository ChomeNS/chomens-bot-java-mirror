package me.chayapak1.chomens_bot.song;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import me.chayapak1.chomens_bot.util.DownloadUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// Author: _ChipMC_ & hhhzzzsss but modified
public class SongLoaderThread extends Thread {
    // should the converters be here?
    public static final List<Converter> converters = new ArrayList<>();

    static {
        converters.add(new MidiConverter());
        converters.add(new NBSConverter());
        converters.add(new TextFileConverter());
        converters.add(new SongPlayerConverter());
    }

    public final String fileName;

    private Path songPath;
    private URL songUrl;
    public SongLoaderException exception;
    public Song song;

    private final Bot bot;

    private final String requester;

    private final boolean isUrl;

    private byte[] data;

    private boolean isItem = false;

    private boolean isFolder = false;

    public SongLoaderThread(URL location, Bot bot, String requester) {
        this.bot = bot;
        this.requester = requester;
        isUrl = true;
        songUrl = location;

        fileName = location.getFile();
    }

    public SongLoaderThread(Path location, Bot bot, String requester) {
        this.bot = bot;
        this.requester = requester;
        isUrl = false;
        songPath = location;

        isFolder = Files.isDirectory(songPath);

        fileName = location.getFileName().toString();
    }

    public SongLoaderThread (byte[] data, Bot bot, String requester) {
        this.bot = bot;
        this.requester = requester;
        this.data = data;
        this.isItem = true;
        this.isUrl = false;

        fileName = requester + "'s song item";
    }

    @Override
    public void run () {
        if (isFolder && !isUrl && !isItem) {
            try (Stream<Path> files = Files.list(songPath)) {
                files.forEach((file) -> {
                    songPath = file;
                    processFile();
                });

                showAddedToQueue();
            } catch (IOException e) {
                bot.logger.error(e);
            }
        } else processFile();
    }

    private void processFile () {
        if (bot.music.songQueue.size() > 100) return;

        byte[] bytes;
        String name;
        try {
            if (isUrl) {
                bytes = DownloadUtilities.DownloadToByteArray(songUrl, 5 * 1024 * 1024);
                final Path fileName = Paths.get(songUrl.toURI().getPath()).getFileName();

                name = fileName == null ? "(root)" : fileName.toString();
            } else if (isItem) {
                bytes = data;
                name = requester + "'s song item";
            } else {
                bytes = Files.readAllBytes(songPath);
                name = !isFolder ? fileName : songPath.getFileName().toString();
            }
        } catch (Exception e) {
            exception = new SongLoaderException(Component.text(e.getMessage()));

            failed();

            return;
        }

        for (Converter converter : converters) {
            if (song != null && !isFolder) break;

            try {
                song = converter.getSongFromBytes(bytes, name, bot);
            } catch (Exception ignored) {}
        }

        if (song == null) {
            exception = new SongLoaderException(Component.translatable("Invalid format"));

            failed();
        } else {
            song.requester = requester;

            bot.music.songQueue.add(song);

            if (!isFolder) showAddedToQueue();
        }

        bot.music.loaderThread = null;
    }

    private void showAddedToQueue () {
        if (isFolder) {
            bot.chat.tellraw(
                    Component.text(
                            "Added folder to the song queue"
                    ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
            );
        } else {
            bot.chat.tellraw(
                    Component.translatable(
                            "Added %s to the song queue",
                            Component.empty().append(Component.text(song.name)).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                    ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
            );
        }
    }

    private void failed () {
        bot.chat.tellraw(Component.translatable("Failed to load song: %s", exception.message).color(NamedTextColor.RED));
        bot.music.loaderThread = null;
    }
}