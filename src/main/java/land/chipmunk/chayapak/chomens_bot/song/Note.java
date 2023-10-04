package land.chipmunk.chayapak.chomens_bot.song;

// Author: hhhzzzsss
public class Note implements Comparable<Note> {
  public final Instrument instrument;
  public final Instrument shiftedInstrument;
  public final int pitch;
  public final int shiftedPitch;
  public final int originalPitch;
  public final float volume;
  public final long time;
  public final int panning;
  public final int stereo;

  public Note (
          Instrument instrument,
          int pitch,
          int originalPitch,
          float volume,
          long time,
          int panning,
          int stereo
  ) {
    this.instrument = instrument;
    this.shiftedInstrument = this.instrument;
    this.pitch = pitch;
    this.shiftedPitch = this.pitch;
    this.originalPitch = originalPitch;
    this.volume = volume;
    this.time = time;
    this.panning = panning;
    this.stereo = stereo;
  }

  public Note (
          Instrument instrument,
          Instrument shiftedInstrument,
          int pitch,
          int shiftedPitch,
          int originalPitch,
          float volume,
          long time,
          int panning,
          int stereo
  ) {
    this.instrument = instrument;
    this.shiftedInstrument = shiftedInstrument;
    this.pitch = pitch;
    this.shiftedPitch = shiftedPitch;
    this.originalPitch = originalPitch;
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
