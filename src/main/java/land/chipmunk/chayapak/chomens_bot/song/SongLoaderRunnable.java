package land.chipmunk.chayapak.chomens_bot.song;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.DownloadUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;

// Author: _ChipMC_ or hhhzzzsss? also i modified it to use runnable
// because thread = bad !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
public class SongLoaderRunnable implements Runnable {
  public final String fileName;

  private File songPath;
  private URL songUrl;
  public SongLoaderException exception;
  public Song song;

  private final Bot bot;

  private final boolean isUrl;

  private boolean isFolder = false;

  public SongLoaderRunnable(URL location, Bot bot) {
    this.bot = bot;
    isUrl = true;
    songUrl = location;

    fileName = location.getFile();
  }

  public SongLoaderRunnable(Path location, Bot bot) {
    this.bot = bot;
    isUrl = false;
    songPath = location.toFile();

    isFolder = songPath.isDirectory();

    fileName = location.getFileName().toString();
  }

  public void run () {
    if (isFolder && !isUrl) {
      final File[] files = songPath.listFiles();

      if (files != null) {
        for (File file : files) {
          songPath = file;
          processFile();
        }

        showAddedToQueue();
      }
    } else processFile();
  }

  private void processFile () {
    byte[] bytes;
    String name;
    try {
      if (isUrl) {
        bytes = DownloadUtilities.DownloadToByteArray(songUrl, 10*1024*1024);
        name = Paths.get(songUrl.toURI().getPath()).getFileName().toString();
      } else {
        bytes = Files.readAllBytes(songPath.toPath());
        name = songPath.getName();
      }
    } catch (Exception e) {
      exception = new SongLoaderException(Component.text(e.getMessage()));

      failed();

      return;
    }

    try {
      song = MidiConverter.getSongFromBytes(bytes, name, bot);
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (song == null) {
      try {
        song = NBSConverter.getSongFromBytes(bytes, name, bot);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (song == null) {
      exception = new SongLoaderException(Component.translatable("Invalid format"));

      failed();
    } else {
      bot.music.songQueue.add(song);

      if (!isFolder) showAddedToQueue();
    }
  }

  private void showAddedToQueue () {
    bot.chat.tellraw(
            Component.translatable(
                    "Added %s to the song queue",
                    Component.empty().append(song.name).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
            ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
    );
  }

  private void failed() {
    exception.printStackTrace();
    bot.chat.tellraw(Component.translatable("Failed to load song: %s", exception.message).color(NamedTextColor.RED));
    bot.music.loaderThread = null;
  }
}