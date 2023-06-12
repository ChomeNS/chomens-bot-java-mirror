package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.BossBar;
import land.chipmunk.chayapak.chomens_bot.data.BossBarColor;
import land.chipmunk.chayapak.chomens_bot.data.BossBarStyle;
import land.chipmunk.chayapak.chomens_bot.song.*;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.MathUtilities;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

// Author: _ChipMC_ & chayapak <3
public class MusicPlayerPlugin extends Bot.Listener {
    private final Bot bot;

    public static final String SELECTOR = "@a[tag=!nomusic,tag=!chomens_bot_nomusic]";
    public static File SONG_DIR = new File("songs");
    static {
        if (!SONG_DIR.exists()) {
            SONG_DIR.mkdir();
        }
    }

    @Getter @Setter private Song currentSong;
    @Getter @Setter private List<Song> songQueue = new ArrayList<>();
    @Getter @Setter private SongLoaderRunnable loaderThread;
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
        try {
            final SongLoaderRunnable runnable = new SongLoaderRunnable(location, bot);

            bot.chat().tellraw(
                    Component
                            .translatable(
                                    "Loading %s",
                                    Component.text(location.getFileName().toString(), ColorUtilities.getColorByString(bot.config().colorPalette().secondary()))
                            )
                            .color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()))
            );

            bot.executorService().submit(runnable);
        } catch (SongLoaderException e) {
            e.printStackTrace();
            bot.chat().tellraw(Component.translatable("Failed to load song: %s", e.message()).color(NamedTextColor.RED));
            loaderThread = null;
        }
    }

    public void loadSong (URL location) {
        try {
            final SongLoaderRunnable runnable = new SongLoaderRunnable(location, bot);

            bot.chat().tellraw(
                    Component
                            .translatable(
                                    "Loading %s",
                                    Component.text(location.toString(), ColorUtilities.getColorByString(bot.config().colorPalette().secondary()))
                            )
                            .color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()))
            );

            bot.executorService().submit(runnable);
        } catch (SongLoaderException e) {
            bot.chat().tellraw(Component.translatable("Failed to load song: %s", e.message()).color(NamedTextColor.RED));
            loaderThread = null;
        }
    }

    public void coreReady () {
        bot.tick().addListener(new TickPlugin.Listener() {
            @Override
            public void onTick() {
                if (currentSong == null) {
                    if (songQueue.size() == 0) return; // this line

                    addBossBar();

                    currentSong = songQueue.get(0); // songQueue.poll();
                    bot.chat().tellraw(
                            Component.translatable(
                                    "Now playing %s",
                                    Component.empty().append(currentSong.name).color(ColorUtilities.getColorByString(bot.config().colorPalette().secondary()))
                            ).color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()))
                    );
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
                    if (loop == Loop.CURRENT) {
                        currentSong.setTime(0);
                        return;
                    }

                    bot.chat().tellraw(
                            Component.translatable(
                                    "Finished playing %s",
                                    Component.empty().append(currentSong.name).color(ColorUtilities.getColorByString(bot.config().colorPalette().secondary()))
                            ).color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()))
                    );

                    if (loop == Loop.ALL) {
                        skip();
                        return;
                    }

                    songQueue.remove(0);

                    if (songQueue.size() == 0) {
                        stopPlaying();
                        removeBossBar();
                        bot.chat().tellraw(
                                Component
                                        .text("Finished playing every song in the queue")
                                        .color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()))
                        );
                        return;
                    }
                    if (currentSong.size() > 0) {
                        currentSong = songQueue.get(0);
                        currentSong.setTime(0);
                        currentSong.play();
                    }
                }
            }
        });

        if (currentSong != null) currentSong.play();
    }

    public void skip () {
        if (loop == Loop.ALL) {
            songQueue.add(songQueue.remove(0)); // bot.music.queue.push(bot.music.queue.shift()) in js
        } else {
            songQueue.remove(0);
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
        if (currentSong != null) currentSong.pause(); // nice.
    }

    public void handlePlaying () {
        currentSong.advanceTime();
        while (currentSong.reachedNextNote()) {
            final Note note = currentSong.getNextNote();

            float key = note.pitch;

            // totally didn't look at the minecraft code and found the note block pitch thingy so i totallydidnotskidded™ it
            final double floatingPitch = Math.pow(2.0, ((key + (pitch / 10)) - 12) / 12.0);
            // final double floatingPitch = 0.5 * (Math.pow(2, ((key + (pitch / 10)) / 12)));

            // totallynotskidded from opennbs
            int blockPosition = 0;

            if (currentSong.nbs) {
                final int s = (note.stereo + note.panning) / 2; // Stereo values to X coordinates, calc'd from the average of both note and layer pan.
                if (s > 100) blockPosition = (s - 100) / -100;
                if (s < 100) blockPosition = ((s - 100) * -1) / 100;
            }

            bot.core().run(
                    "minecraft:execute as " +
                            SELECTOR +
                            " at @s run playsound " +
                            note.instrument.sound +
                            " record @s ^" + blockPosition + " ^ ^ " +
                            note.volume +
                            " " +
                            MathUtilities.clamp(floatingPitch, 0, 2)
            );
        }
    }
}
