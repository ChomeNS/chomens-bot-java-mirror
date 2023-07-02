package land.chipmunk.chayapak.chomens_bot.song;

import land.chipmunk.chayapak.chomens_bot.Bot;
import net.kyori.adventure.text.Component;
import java.util.ArrayList;
import java.util.Collections;

// Author: hhhzzzsss & _ChipMC_ but i changed most of the stuff
public class Song {
  public final ArrayList<Note> notes = new ArrayList<>();
  public final Component name;
  public int position = 0; // Current note index
  public boolean paused = true;
  public long startTime = 0; // Start time in millis since unix epoch
  public long length = 0; // Milliseconds in the song
  public long time = 0; // Time since start of song
  public long loopPosition = 200; // Milliseconds into the song to start looping

  public final String songName;
  public final String songAuthor;
  public final String songOriginalAuthor;
  public final String songDescription;

  public final boolean nbs;

//  public int loopCount = 0; // Number of times to loop
//  public int currentLoop = 0; // Number of loops so far

  private Bot bot;

  public Song (Component name, Bot bot, String songName, String songAuthor, String songOriginalAuthor, String songDescription, boolean nbs) {
    this.bot = bot;
    this.name = name;
    this.songName = songName;
    this.songAuthor = songAuthor;
    this.songOriginalAuthor = songOriginalAuthor;
    this.songDescription = songDescription;
    this.nbs = nbs;
  }

  public Song (String name, Bot bot, String songName, String songAuthor, String songOriginalAuthor, String songDescription, boolean nbs) {
    this(Component.text(name), bot, songName, songAuthor, songOriginalAuthor, songDescription, nbs);
    this.bot = bot;
  }

  public Note get (int i) {
    return notes.get(i);
  }

  public void add (Note e) {
    notes.add(e);
  }

  public void sort () {
    Collections.sort(notes);
  }

  /**
   * Starts playing song (does nothing if already playing)
   */
  public void play () {
    if (paused) {
      paused = false;
      startTime = System.currentTimeMillis() - time;
    }
  }

  /**
   * Pauses song (does nothing if already paused)
   */
  public void pause () {
    if (!paused) {
      paused = true;
      // Recalculates time so that the song will continue playing after the exact point it was paused
      advanceTime();
    }
  }

  public void setTime (long t) {
    time = t;
    startTime = System.currentTimeMillis() - time;
    position = 0;
    while (position < notes.size() && notes.get(position).time < t) {
      position++;
    }
  }

  public void advanceTime () {
    time = (long) ((System.currentTimeMillis() - startTime) * bot.music.speed);
  }

  public boolean reachedNextNote () {
    if (position < notes.size()) {
      return notes.get(position).time <= time * bot.music.speed;
    } else {
      if (finished() && bot.music.loop != Loop.OFF) {
        if (position < notes.size()) {
          return notes.get(position).time <= time * bot.music.speed;
        } else {
          return false;
        }
      } else {
        return false;
      }
    }
  }

  public Note getNextNote () {
    if (position >= notes.size()) {
      if (bot.music.loop == Loop.OFF) return null;
    }
    return notes.get(position++);
  }

  public boolean finished () {
    return time > length || position >= size();
  }

  public int size () {
    return notes.size();
  }
}
