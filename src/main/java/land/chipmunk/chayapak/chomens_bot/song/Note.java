package land.chipmunk.chayapak.chomens_bot.song;

import lombok.AllArgsConstructor;

// Author: hhhzzzsss
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
    return Long.compare(time, other.time);
  }
}
