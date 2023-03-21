package me.chayapak1.chomensbot_mabe.plugins;

import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import lombok.Getter;
import lombok.Setter;
import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.song.Note;
import me.chayapak1.chomensbot_mabe.song.Song;
import me.chayapak1.chomensbot_mabe.song.SongLoaderException;
import me.chayapak1.chomensbot_mabe.song.SongLoaderThread;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MusicPlayerPlugin extends SessionAdapter {
    private final Bot bot;

    private ScheduledFuture<?> futurePlayTask;

    public static final String SELECTOR  = "@a[tag=!nomusic,tag=!chomens_bot_nomusic]";
    public static File SONG_DIR = new File("songs");
    static {
        if (!SONG_DIR.exists()) {
            SONG_DIR.mkdir();
        }
    }

    @Getter @Setter private Song currentSong;
    @Getter @Setter private LinkedList<Song> songQueue = new LinkedList<>();
    @Getter @Setter private SongLoaderThread loaderThread;
    @Getter @Setter private int loop = 0;
    private int ticksUntilPausedBossbar = 20;
    private final String bossbarName = "chomens_bot:music"; // maybe make this in the config?

    public MusicPlayerPlugin (Bot bot) {
        this.bot = bot;
        bot.addListener(this);
        bot.core().addListener(new CorePlugin.Listener() {
            public void ready () { coreReady(); }
        });
    }

    public void loadSong (Path location) {
        if (loaderThread != null) {
            bot.chat().tellraw(Component.translatable("Already loading a song, can't load another", NamedTextColor.RED));
            return;
        }

        try {
            final SongLoaderThread _loaderThread = new SongLoaderThread(location, bot);
            bot.chat().tellraw(Component.translatable("Loading %s", Component.text(location.getFileName().toString(), NamedTextColor.GOLD)));
            _loaderThread.start();
            loaderThread = _loaderThread;
        } catch (SongLoaderException e) {
            e.printStackTrace();
            bot.chat().tellraw(Component.translatable("Failed to load song: %s", e.message()).color(NamedTextColor.RED));
            loaderThread = null;
        }
    }

    public void loadSong (URL location) {
        if (loaderThread != null) {
            bot.chat().tellraw(Component.translatable("Already loading a song, can't load another", NamedTextColor.RED));
            return;
        }

        try {
            final SongLoaderThread _loaderThread = new SongLoaderThread(location, bot);
            bot.chat().tellraw(Component.translatable("Loading %s", Component.text(location.toString(), NamedTextColor.GOLD)));
            _loaderThread.start();
            loaderThread = _loaderThread;
        } catch (SongLoaderException e) {
            bot.chat().tellraw(Component.translatable("Failed to load song: %s", e.message()).color(NamedTextColor.RED));
            loaderThread = null;
        }
    }

    public void coreReady () {
        final Runnable playTask = () -> {
            if (loaderThread != null && !loaderThread.isAlive()) {
                if (loaderThread.exception != null) {
                    bot.chat().tellraw(Component.translatable("Failed to load song: %s", loaderThread.exception.message()).color(NamedTextColor.RED));
                } else {
                    songQueue.add(loaderThread.song);
                    bot.chat().tellraw(Component.translatable("Added %s to the song queue", Component.empty().append(loaderThread.song.name).color(NamedTextColor.GOLD)));
                }
                loaderThread = null;
            }

            if (currentSong == null) {
                if (songQueue.size() == 0) return;

                currentSong = songQueue.get(0); // songQueue.poll();
                bot.chat().tellraw(Component.translatable("Now playing %s", Component.empty().append(currentSong.name).color(NamedTextColor.GOLD)));
                currentSong.play();
            }

            if (currentSong.paused && ticksUntilPausedBossbar-- < 0) return;
            else ticksUntilPausedBossbar = 20;

            bot.core().run("minecraft:bossbar add " + bossbarName + " \"\"");
            bot.core().run("minecraft:bossbar set " + bossbarName + " players " + SELECTOR);
            bot.core().run("minecraft:bossbar set " + bossbarName + " name " + GsonComponentSerializer.gson().serialize(generateBossbar()));
            bot.core().run("minecraft:bossbar set " + bossbarName + " color yellow");
            bot.core().run("minecraft:bossbar set " + bossbarName + " visible true");
            bot.core().run("minecraft:bossbar set " + bossbarName + " value " + (int) Math.floor(currentSong.time));
            bot.core().run("minecraft:bossbar set " + bossbarName + " max " + currentSong.length);

            if (currentSong.paused) return;

            handlePlaying();

            if (currentSong.finished()) {
                removeBossbar();
                bot.chat().tellraw(Component.translatable("Finished playing %s", Component.empty().append(currentSong.name).color(NamedTextColor.GOLD)));

                if (loop == 1) {
                    currentSong.setTime(0);
                    return;
                }
                if (loop == 2) {
                    skip();
                    return;
                }

                songQueue.remove();

                if (songQueue.size() == 0) {
                    stopPlaying();
                    bot.chat().tellraw(Component.text("Finished playing every sone in the queue"));
                    return;
                }
                if (currentSong.size() > 0) {
                    currentSong = songQueue.get(0);
                    currentSong.setTime(0);
                    currentSong.play();
                }
            }
        };

        futurePlayTask = bot.executor().scheduleAtFixedRate(playTask, 50, 50, TimeUnit.MILLISECONDS);

        if (currentSong != null) currentSong.play();
    }

    public void skip () {
        if (loop == 2) {
            songQueue.add(songQueue.remove()); // bot.music.queue.push(bot.music.queue.shift()) in js
        } else {
            songQueue.remove();
            stopPlaying();
        }
        if (songQueue.size() == 0) return;
        currentSong = songQueue.get(0);
        currentSong.setTime(0);
        currentSong.play();
    }

    public void removeBossbar () {
        bot.core().run("minecraft:bossbar remove " + bossbarName);
    }

    public Component generateBossbar () {
        Component component = Component.empty()
                .append(Component.empty().append(currentSong.name).color(NamedTextColor.GREEN))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(Component.translatable("%s / %s", formatTime(currentSong.time).color(NamedTextColor.GRAY), formatTime(currentSong.length).color(NamedTextColor.GRAY)).color(NamedTextColor.DARK_GRAY))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(Component.translatable("%s / %s", Component.text(currentSong.position, NamedTextColor.GRAY), Component.text(currentSong.size(), NamedTextColor.GRAY)).color(NamedTextColor.DARK_GRAY));

        if (currentSong.paused) {
            return component
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Paused", NamedTextColor.LIGHT_PURPLE));
        }

        if (loop > 0) {
            return component
                    .append(Component.translatable(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.translatable("Looping " + ((loop == 1) ? "current" : "all"), NamedTextColor.LIGHT_PURPLE));
        }

        return component;
    }

    public Component formatTime (long millis) {
        final int seconds = (int) millis / 1000;

        final String minutePart = String.valueOf(seconds / 60);
        final String unpaddedSecondPart = String.valueOf(seconds % 60);

        return Component.translatable(
                "%s:%s",
                Component.text(minutePart),
                Component.text(unpaddedSecondPart.length() < 2 ? "0" + unpaddedSecondPart : unpaddedSecondPart)
        );
    }

    public void stopPlaying () {
        currentSong = null;
        removeBossbar();
    }

    @Override
    public void disconnected (DisconnectedEvent event) {
        futurePlayTask.cancel(false);

        if (currentSong != null) currentSong.pause(); // nice.
    }

    public void handlePlaying () {
        currentSong.advanceTime();
        while (currentSong.reachedNextNote()) {
            final Note note = currentSong.getNextNote();

            final double floatingPitch = Math.pow(2, (note.pitch - 12) / 12.0);

            bot.core().run("minecraft:execute as " + SELECTOR + " at @s run playsound " + note.instrument.sound + " record @s ~ ~ ~ " + note.volume + " " + floatingPitch);
        }
    }
}
