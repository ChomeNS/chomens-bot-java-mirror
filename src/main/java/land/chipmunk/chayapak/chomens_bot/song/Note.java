package land.chipmunk.chayapak.chomens_bot.song;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Note implements Comparable<Note> {
  public Instrument instrument;
  public int pitch;
  public float volume;
  public long time;
  public int panning;
  public int stereo;

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
