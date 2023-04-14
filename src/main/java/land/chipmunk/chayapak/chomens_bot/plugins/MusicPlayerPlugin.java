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
    @Getter @Setter private float speed = 1;

    private int ticksUntilPausedBossbar = 20;

    private final String bossbarName = "music";

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
        final Runnable task = () -> {
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

                addBossBar();

                currentSong = songQueue.get(0); // songQueue.poll();
                bot.chat().tellraw(Component.translatable("Now playing %s", Component.empty().append(currentSong.name).color(NamedTextColor.GOLD)));
                currentSong.play();
            }

            if (currentSong.paused && ticksUntilPausedBossbar-- < 0) return;
            else ticksUntilPausedBossbar = 20;

            BossBar bossBar = bot.bossbar().get(bossbarName);

            if (bossBar == null) bossBar = addBossBar();

            bossBar.players(SELECTOR);
            bossBar.name(generateBossbar());
            bossBar.color(pitch > 0 ? BossBarColor.PURPLE : BossBarColor.YELLOW);
            bossBar.visible(true);
            bossBar.style(BossBarStyle.PROGRESS);
            bossBar.value((int) Math.floor(currentSong.time * speed));
            bossBar.max(currentSong.length);

            if (currentSong.paused) return;

            handlePlaying();

            if (currentSong.finished()) {
                bot.chat().tellraw(Component.translatable("Finished playing %s", Component.empty().append(currentSong.name).color(NamedTextColor.GOLD)));

                if (loop == Loop.CURRENT) {
                    currentSong.setTime(0);
                    return;
                }
                if (loop == Loop.ALL) {
                    skip();
                    return;
                }

                songQueue.remove();

                if (songQueue.size() == 0) {
                    stopPlaying();
                    removeBossBar();
                    bot.chat().tellraw(Component.text("Finished playing every song in the queue"));
                    return;
                }
                if (currentSong.size() > 0) {
                    currentSong = songQueue.get(0);
                    currentSong.setTime(0);
                    currentSong.play();
                }
            }
        };

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

    public BossBar addBossBar () {
        final BossBar bossBar = new BossBar(
                Component.empty(),
                BossBarColor.WHITE,
                0,
                "",
                BossBarStyle.PROGRESS,
                0,
                false
        );

        bot.bossbar().add(bossbarName, bossBar);

        return bossBar;
    }

    public void removeBossBar() {
        bot.bossbar().remove(bossbarName);
    }

    public Component generateBossbar () {
        final DecimalFormat formatter = new DecimalFormat("#,###");

        Component component = Component.empty()
                .append(Component.empty().append(currentSong.name).color(pitch > 0 ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GREEN))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(Component.translatable("%s / %s", formatTime((long) (currentSong.time * speed)).color(NamedTextColor.GRAY), formatTime(currentSong.length).color(NamedTextColor.GRAY)).color(NamedTextColor.DARK_GRAY))
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
        removeBossBar();
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

            float key = note.pitch;

            if (key < 33) key -= 9;
            else if (key > 57) key -= 57;
            else key -= 33;

            final double floatingPitch = Math.pow(
                    2,
                    (key + (pitch / 10)) / 12
            );

            // if the thing is still out of range just ignore and don't play it!1!1
            if (floatingPitch < -1 || floatingPitch > 3) continue;

            bot.core().run(
                    "minecraft:execute as " +
                            SELECTOR +
                            " at @s run playsound " +
                            note.instrument.sound +
                            " record @s ~ ~ ~ " +
                            note.volume +
                            " " +
                            NumberUtilities.clamp(floatingPitch, 0, 2)
            );
        }
    }
}
