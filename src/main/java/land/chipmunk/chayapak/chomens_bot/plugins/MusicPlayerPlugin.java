package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.BossBar;
import land.chipmunk.chayapak.chomens_bot.data.BossBarColor;
import land.chipmunk.chayapak.chomens_bot.data.BossBarStyle;
import land.chipmunk.chayapak.chomens_bot.song.*;
import land.chipmunk.chayapak.chomens_bot.util.NumberUtilities;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MusicPlayerPlugin extends SessionAdapter {
    private final Bot bot;

    private ScheduledFuture<?> playTask;

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
    @Getter @Setter private Loop loop = Loop.OFF;

    // sus nightcore stuff,..,.,.
    @Getter @Setter private float pitch = 0;

    private int ticksUntilPausedBossbar = 20;

    private final String bossbarName = "music";
    private Runnable runnable;

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
            bot.chat().tellraw(Component.translatable("Already loading a song", NamedTextColor.RED));
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
        final Runnable task = runnable;

        playTask = bot.executor().scheduleAtFixedRate(task, 50, 50, TimeUnit.MILLISECONDS);

        if (currentSong != null) currentSong.play();
    }

    public void skip () {
        if (loop == Loop.ALL) {
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
        bot.bossbar().remove(bossbarName);
    }

    public Component generateBossbar () {
        final DecimalFormat formatter = new DecimalFormat("#,###");

        Component component = Component.empty()
                .append(Component.empty().append(currentSong.name).color(pitch > 0 ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GREEN))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(Component.translatable("%s / %s", formatTime(currentSong.time).color(NamedTextColor.GRAY), formatTime(currentSong.length).color(NamedTextColor.GRAY)).color(NamedTextColor.DARK_GRAY))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(
                    Component.translatable(
                            "%s / %s",
                            Component.text(formatter.format(currentSong.position), NamedTextColor.GRAY),
                            Component.text(formatter.format(currentSong.size()), NamedTextColor.GRAY)
                    ).color(NamedTextColor.DARK_GRAY)
                );

        if (currentSong.paused) {
            return component
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Paused", NamedTextColor.LIGHT_PURPLE));
        }

        if (loop != Loop.OFF) {
            return component
                    .append(Component.translatable(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.translatable("Looping " + ((loop == Loop.CURRENT) ? "current" : "all"), NamedTextColor.LIGHT_PURPLE));
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
        playTask.cancel(true);

        if (currentSong != null) currentSong.pause(); // nice.
    }

    public void handlePlaying () {
        currentSong.advanceTime();
        while (currentSong.reachedNextNote()) {
            final Note note = currentSong.getNextNote();

            final double floatingPitch = NumberUtilities.clamp(Math.pow(2, ((note.pitch + (pitch / 10)) - 12) / 12.0), 0, 2);

            bot.core().run("minecraft:execute as " + SELECTOR + " at @s run playsound " + note.instrument.sound + " record @s ~ ~ ~ " + note.volume + " " + floatingPitch);
        }
    }
}
