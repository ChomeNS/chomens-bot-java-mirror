package land.chipmunk.chayapak.chomens_bot.song;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.DownloadUtilities;
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

// Author: _ChipMC_ or hhhzzzsss? also i modified it to use runnable
// because thread = bad !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
public class SongLoaderThread extends Thread {
  // should the converters be here?
  public static final List<Converter> converters = new ArrayList<>();

  static {
    converters.add(new MidiConverter());
    converters.add(new NBSConverter());
    converters.add(new TextFileConverter());
  }

  public final String fileName;

  private Path songPath;
  private URL songUrl;
  public SongLoaderException exception;
  public Song song;

  private final Bot bot;

  private final String requester;

  private final boolean isUrl;

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

  @Override
  public void run () {
    if (isFolder && !isUrl) {
      try (Stream<Path> files = Files.list(songPath)) {
        if (files != null) {
          files.forEach((file) -> {
            songPath = file;
            processFile();
          });

          showAddedToQueue();
        }
      } catch (IOException e) {
        e.printStackTrace();
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
      } catch (Exception e) {
        e.printStackTrace();
      }
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

  private void failed() {
    exception.printStackTrace();
    bot.chat.tellraw(Component.translatable("Failed to load song: %s", exception.message).color(NamedTextColor.RED));
    bot.music.loaderThread = null;
  }
}