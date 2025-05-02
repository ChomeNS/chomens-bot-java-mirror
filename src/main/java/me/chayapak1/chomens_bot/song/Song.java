package me.chayapak1.chomens_bot.song;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Bot;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static me.chayapak1.chomens_bot.util.StringUtilities.isNotNullAndNotBlank;

// Author: hhhzzzsss & _ChipMC_ but i changed most of the stuff
public class Song {
    public final List<Note> notes = new ObjectArrayList<>();
    public final String originalName;
    public String name;
    public String requester = "Unknown";
    public int position = 0; // Current note index
    public boolean paused = true;
    public double startTime = 0; // Start time in millis since unix epoch
    public double length = 0; // Milliseconds in the song
    public double time = 0; // Time since start of song
    public long loopPosition = 0; // Milliseconds into the song to start looping

    public final Map<Long, String> lyrics = new Object2ObjectOpenHashMap<>();

    public String songName;
    public String songAuthor;
    public String songOriginalAuthor;
    public String songDescription;

    public final String tracks;

    public final boolean nbs;

    //  public int loopCount = 0; // Number of times to loop
    //  public int currentLoop = 0; // Number of loops so far

    private final Bot bot;

    public Song (final String originalName, final Bot bot, final String songName, final String songAuthor, final String songOriginalAuthor, final String songDescription, final String tracks, final boolean nbs) {
        this.originalName = originalName;
        this.bot = bot;
        this.songName = songName;
        this.songAuthor = songAuthor;
        this.songOriginalAuthor = songOriginalAuthor;
        this.songDescription = songDescription;
        this.tracks = tracks;
        this.nbs = nbs;

        updateName();
    }

    public void updateName () {
        String authorPart = null;

        if (isNotNullAndNotBlank(songOriginalAuthor) && isNotNullAndNotBlank(songAuthor)) {
            authorPart = String.format("%s/%s", songOriginalAuthor, songAuthor);
        } else if (isNotNullAndNotBlank(songOriginalAuthor)) {
            authorPart = songOriginalAuthor;
        } else if (isNotNullAndNotBlank(songAuthor)) {
            authorPart = songAuthor;
        }

        final String namePart = isNotNullAndNotBlank(songName) ? songName : originalName;
        name = (authorPart != null) ? String.format("%s - %s", authorPart, namePart) : namePart;
    }

    public Note get (final int i) {
        return notes.get(i);
    }

    public void add (final Note e) {
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
            if (loopPosition != 0) bot.music.loop = Loop.CURRENT;
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

    public void setTime (final double t) {
        time = t;
        startTime = System.currentTimeMillis() - time;
        position = 0;
        while (position < notes.size() && notes.get(position).time / bot.music.speed <= t) {
            position++;
        }
    }

    public void advanceTime () {
        time = (System.currentTimeMillis() - startTime) * bot.music.speed;
    }

    public boolean reachedNextNote () {
        if (position < notes.size()) {
            return notes.get(position).time / bot.music.speed <= time;
        } else {
            if (finished() && bot.music.loop != Loop.OFF) {
                if (position < notes.size()) {
                    return notes.get(position).time / bot.music.speed <= time;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public void loop () {
        position = 0;
        startTime += length - loopPosition;
        time -= length - loopPosition;
        while (position < notes.size() && notes.get(position).time / bot.music.speed < loopPosition) {
            position++;
        }
    }

    public Note getNextNote () {
        if (position >= notes.size() && bot.music.loop == Loop.OFF) return null;
        return notes.get(position++);
    }

    public boolean finished () {
        return time > length || position >= size();
    }

    public int size () {
        return notes.size();
    }
}
