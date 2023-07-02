package land.chipmunk.chayapak.chomens_bot.song;

// Author: hhhzzzsss
public class Note implements Comparable<Note> {
  public Instrument instrument;
  public int pitch;
  public float volume;
  public long time;
  public int panning;
  public int stereo;

  public Note (
          Instrument instrument,
          int pitch,
          float volume,
          long time,
          int panning,
          int stereo
  ) {
    this.instrument = instrument;
    this.pitch = pitch;
    this.volume = volume;
    this.time = time;
    this.panning = panning;
    this.stereo = stereo;
  }

  @Override
  public int compareTo(Note other) {
    return Long.compare(time, other.time);
  }
}
