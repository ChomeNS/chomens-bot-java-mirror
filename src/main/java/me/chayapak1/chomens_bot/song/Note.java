package me.chayapak1.chomens_bot.song;

// Author: hhhzzzsss
public class Note implements Comparable<Note> {
    public final Instrument instrument;
    public final Instrument shiftedInstrument;
    public final double pitch;
    public final double shiftedPitch;
    public final double originalPitch;
    public final float volume;
    public final long time;
    public final int panning;
    public final int stereo;
    public final boolean isRainbowToggle;

    public Note (
            Instrument instrument,
            double pitch,
            double originalPitch,
            float volume,
            long time,
            int panning,
            int stereo,
            boolean isRainbowToggle
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
            Instrument instrument,
            Instrument shiftedInstrument,
            double pitch,
            double shiftedPitch,
            double originalPitch,
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
        this.isRainbowToggle = false;
    }

    @Override
    public int compareTo(Note other) {
        return Long.compare(time, other.time);
    }
}
