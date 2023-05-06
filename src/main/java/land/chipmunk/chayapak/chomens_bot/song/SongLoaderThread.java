package land.chipmunk.chayapak.chomens_bot.song;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.plugins.MusicPlayerPlugin;
import land.chipmunk.chayapak.chomens_bot.util.DownloadUtilities;
import net.kyori.adventure.text.Component;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;

// Author: _ChipMC_ or hhhzzzsss?
public class SongLoaderThread extends Thread {
  public String fileName;

  private File songPath;
  private URL songUrl;
  public SongLoaderException exception;
  public Song song;

  private final Bot bot;

  private final boolean isUrl;

  public SongLoaderThread (URL location, Bot bot) throws SongLoaderException {
    this.bot = bot;
    isUrl = true;
    songUrl = location;

    fileName = location.getFile();
  }

  public SongLoaderThread (Path location, Bot bot) throws SongLoaderException {
    this.bot = bot;
    isUrl = false;
    songPath = location.toFile();

    fileName = location.getFileName().toString();
  }

  public void run () {
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
      e.printStackTrace();
      exception = new SongLoaderException(Component.text(e.getMessage()), e);
      return;
    }

    try {
      if (name.endsWith(".mid") || name.endsWith(".midi")) {
        song = MidiConverter.getSongFromBytes(bytes, name, bot);
        return;
      }

      if (name.endsWith(".nbs")) {
        song = NBSConverter.getSongFromBytes(bytes, name, bot);
        return;
      }
    } catch (Exception e) {
      e.printStackTrace();

      exception = new SongLoaderException(Component.translatable("Invalid format"));
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
    }
  }

  private File getSongFile (String name) {
    return new File(MusicPlayerPlugin.SONG_DIR, name);
  }
}