package me.chayapak1.chomensbot_mabe.song;

import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.plugins.MusicPlayerPlugin;
import me.chayapak1.chomensbot_mabe.util.DownloadUtilities;
import net.kyori.adventure.text.Component;
import java.io.File;
import java.io.IOException;
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
    System.out.println("its url");
    songUrl = location;
  }

  public SongLoaderThread (Path location, Bot bot) throws SongLoaderException {
    this.bot = bot;
    isUrl = false;
    System.out.println("its path");
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
      System.out.println("WHAT THE SUS EXCEPTION YUP ! !! %@%$");
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
      exception = new SongLoaderException(Component.translatable("Invalid song format"));
    }
  }

  private File getSongFile (String name) {
    return new File(MusicPlayerPlugin.SONG_DIR, name);
  }
}