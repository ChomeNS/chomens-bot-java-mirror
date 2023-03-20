package me.chayapak1.chomensbot_mabe.song;

import me.chayapak1.chomensbot_mabe.Bot;
import net.kyori.adventure.text.Component;
import java.util.ArrayList;
import java.util.Collections;

public class Song {
  public ArrayList<Note> notes = new ArrayList<>();
  public Component name;
  public int position = 0; // Current note index
  public int looping = 0; // 0 for no looping, 1 for current loop, 2 for all loops
  public boolean paused = true;
  public long startTime = 0; // Start time in millis since unix epoch
  public long length = 0; // Milliseconds in the song
  public long time = 0; // Time since start of song
  public long loopPosition = 0; // Milliseconds into the song to start looping
  public int loopCount = 0; // Number of times to loop
  public int currentLoop = 0; // Number of loops so far
  public int queueIndex = 0;

  private Bot bot;
  
  public Song (Component name, Bot bot) {
    this.bot = bot;
    this.name = name;
  }

  public Song (String name, Bot bot) {
    this(Component.text(name), bot);
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
//    System.out.println("time is " + time);
    startTime = System.currentTimeMillis() - time;
//    System.out.println("start time is " + startTime);
    position = 0;
    while (position < notes.size() && notes.get(position).time < t) {
      position++;
    }
//    System.out.println("position is " + position);
  }

  public void advanceTime () {
    time = System.currentTimeMillis() - startTime;
  }

  public boolean reachedNextNote () {
    if (position < notes.size()) {
      return notes.get(position).time <= time;
    } else {
      if (time > length && shouldLoop()) {
        loop();
        if (position < notes.size()) {
          return notes.get(position).time <= time;
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
      if (shouldLoop()) {
        loop();
      } else {
        return null;
      }
    }
    return notes.get(position++);
  }

  public boolean finished () {
    return time > length && !shouldLoop();
  }

  private void loop () {
//    position = 0;
//    startTime += length - loopPosition;
//    time -= length - loopPosition;
//    while (position < notes.size() && notes.get(position).time < loopPosition) {
//      position++;
//    }
//    currentLoop++;
//    if (looping == 1) {
      position = 0;
      startTime += length - loopPosition;
      time -= length - loopPosition;
      while (position < notes.size() && notes.get(position).time < loopPosition) {
        position++;
      }
//      System.out.println("looping is 1 we did all shits here and position is " + position);
//    } else if (looping == 2) {
//      position = 0;
//      setTime(0);
//      System.out.println("looping is 2 position " + position);
//      queueIndex = (queueIndex + 1) % bot.music().songQueue().size();
//      System.out.println("queue INDEX IS " + queueIndex);
//      System.out.println("BOT SONG QUEUEUE SIZE IS " + bot.music().songQueue().size());
//    }
//    System.out.println(currentLoop);
    currentLoop++;
  }

  private boolean shouldLoop () {
//    if (looping) {
//      if (loopCount == 0) {
//        return true;
//      } else {
//        return currentLoop < loopCount;
//      }
//    } else {
//      return false;
//    }
    if (looping == 1) {
      if (loopCount == 0) {
        return true;
      } else {
        return currentLoop < loopCount;
      }
    } else return looping == 2;
  }

  public int size () {
    return notes.size();
  }
}
