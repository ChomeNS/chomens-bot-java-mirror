package me.chayapak1.chomens_bot.song;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.plugins.MusicPlayerPlugin;
import me.chayapak1.chomens_bot.util.DownloadUtilities;
import net.kyori.adventure.text.Component;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SongLoaderThread extends Thread {
  private String location;
  private File songPath;
  private URL songUrl;
  public SongLoaderException exception;
  public Song song;

  private Bot bot;

  private boolean isUrl = false;

  public SongLoaderThread (URL location, Bot bot) throws SongLoaderException {
    this.bot = bot;
    isUrl = true;
    songUrl = location;
  }

  public SongLoaderThread (Path location, Bot bot) throws SongLoaderException {
    this.bot = bot;
    isUrl = false;
    songPath = location.toFile();
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
      song = MidiConverter.getSongFromBytes(bytes, name, bot);
    } catch (Exception e) {
    }

    if (song == null) {
      try {
        song = NBSConverter.getSongFromBytes(bytes, name, bot);
      } catch (Exception e) {
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