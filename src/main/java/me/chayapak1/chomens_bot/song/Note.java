package me.chayapak1.chomens_bot.song;

// Author: hhhzzzsss
public class Note implements Comparable<Note> {
    public final Instrument instrument;
    public final Instrument shiftedInstrument;
    public final double pitch;
    public final double shiftedPitch;
    public final double originalPitch;
    public final float volume;
    public final double time;
    public final int panning;
    public final int stereo;
    public final boolean isRainbowToggle;

    public Note (
            final Instrument instrument,
            final double pitch,
            final double originalPitch,
            final float volume,
            final double time,
            final int panning,
            final int stereo,
            final boolean isRainbowToggle
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
        this.isRainbowToggle = isRainbowToggle;
    }

    public Note (
            final Instrument instrument,
            final Instrument shiftedInstrument,
            final double pitch,
            final double shiftedPitch,
            final double originalPitch,
            final float volume,
            final double time,
            final int panning,
            final int stereo
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
        this.isRainbowToggle = false;
    }

    @Override
    public int compareTo (final Note other) {
        return Double.compare(time, other.time);
    }
}
