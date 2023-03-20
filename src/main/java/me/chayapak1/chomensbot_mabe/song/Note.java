package me.chayapak1.chomensbot_mabe.song;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Note implements Comparable<Note> {
  public Instrument instrument;
  public int pitch;
  public float volume;
  public long time;

  @Override
  public int compareTo(Note other) {
    if (time < other.time) {
      return -1;
    }
    else if (time > other.time) {
      return 1;
    }
    else {
      return 0;
    }
  }

  public int noteId () {
    return pitch + instrument.id * 25;
  }
}
