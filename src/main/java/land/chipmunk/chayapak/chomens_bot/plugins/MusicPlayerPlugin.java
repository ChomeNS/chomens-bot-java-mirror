package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.data.game.BossBarColor;
import com.github.steveice10.mc.protocol.data.game.BossBarDivision;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.BotBossBar;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.song.Loop;
import land.chipmunk.chayapak.chomens_bot.song.Note;
import land.chipmunk.chayapak.chomens_bot.song.Song;
import land.chipmunk.chayapak.chomens_bot.song.SongLoaderThread;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.MathUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Author: _ChipMC_ & chayapak <3
public class MusicPlayerPlugin extends Bot.Listener {
    private final Bot bot;

    public static final String SELECTOR = "@a[tag=!nomusic,tag=!chomens_bot_nomusic,tag=!custompitch]";
    public static final String CUSTOM_PITCH_SELECTOR = "@a[tag=!nomusic,tag=!chomens_bot_nomusic,tag=custompitch]";
    public static final String BOTH_SELECTOR = "@a[tag=!nomusic,tag=!chomens_bot_nomusic]";

    public static final Path SONG_DIR = Path.of("songs");
    static {
        try {
            if (!Files.exists(SONG_DIR)) Files.createDirectory(SONG_DIR);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Song currentSong;
    public final List<Song> songQueue = new ArrayList<>();
    public SongLoaderThread loaderThread = null;
    public Loop loop = Loop.OFF;

    // sus nightcore stuff,..,.,.
    public float pitch = 0;
    public float speed = 1;
    public int amplify = 1;

    public String instrument = "off";

    private int notesPerSecond = 0;

    private int limit = 0;

    private int stereoNoteIndex = 0;
    private float stereoPosition = -0.2F;

    private final String bossbarName = "music";

    public BossBarColor bossBarColor;

    public MusicPlayerPlugin (Bot bot) {
        this.bot = bot;
        bot.addListener(this);
        bot.core.addListener(new CorePlugin.Listener() {
            public void ready () { coreReady(); }
        });
        bot.executor.scheduleAtFixedRate(() -> notesPerSecond = 0, 0, 1, TimeUnit.SECONDS);
        bot.executor.scheduleAtFixedRate(() -> limit = 0, 0, bot.config.music.urlRatelimit.seconds, TimeUnit.SECONDS);
    }

    public void loadSong (Path location, PlayerEntry sender) {
        if (songQueue.size() > 100) return;

        loaderThread = new SongLoaderThread(location, bot, sender.profile.getName());

        bot.chat.tellraw(
                Component
                        .translatable(
                                "Loading %s",
                                Component.text(location.getFileName().toString(), ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                        )
                        .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
        );

        loaderThread.start();
    }

    public void loadSong (URL location, PlayerEntry sender) {
        if (songQueue.size() > 100) return;

        limit++;

        if (limit > bot.config.music.urlRatelimit.limit) {
            bot.chat.tellraw(Component.text("ohio").color(NamedTextColor.RED));
            return;
        }

        loaderThread = new SongLoaderThread(location, bot, sender.profile.getName());

        bot.chat.tellraw(
                Component
                        .translatable(
                                "Loading %s",
                                Component.text(location.toString(), ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                        )
                        .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
        );

        loaderThread.start();
    }

    public void coreReady () {
        bot.tick.addListener(new TickPlugin.Listener() {
            @Override
            public void onTick() {
                try {
                    if (currentSong == null) {
                        if (songQueue.isEmpty()) return; // this line

                        addBossBar();

                        currentSong = songQueue.get(0); // songQueue.poll();
                        bot.chat.tellraw(
                                Component.translatable(
                                        "Now playing %s",
                                        Component.empty().append(Component.text(currentSong.name)).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
                        );
                        currentSong.play();
                    }

                    if (currentSong.paused) return;

                    if (!currentSong.finished()) {
                        BotBossBar bossBar = bot.bossbar.get(bossbarName);

                        if (bossBar == null && bot.bossbar.enabled) bossBar = addBossBar();

                        if (bot.bossbar.enabled && bot.options.useCore) {
                            bossBar.setTitle(generateBossbar());
                            bossBar.setColor(bossBarColor);
                            bossBar.setValue((int) Math.floor(((double) (currentSong.time * speed) / 1000)));
                            bossBar.setMax((long) (currentSong.length * speed) / 1000);
                        }

                        if (currentSong.paused || bot.core.isRateLimited()) return;

                        handlePlaying();
                    } else {
                        if (loop == Loop.CURRENT) {
                            currentSong.loop();
                            return;
                        }

                        bot.chat.tellraw(
                                Component.translatable(
                                        "Finished playing %s",
                                        Component.empty().append(Component.text(currentSong.name)).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
                        );

                        if (loop == Loop.ALL) {
                            skip();
                            return;
                        }

                        songQueue.remove(0);

                        if (songQueue.isEmpty()) {
                            stopPlaying();
                            removeBossBar();
                            bot.chat.tellraw(
                                    Component
                                            .text("Finished playing every song in the queue")
                                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
                            );
                            return;
                        }
                        if (currentSong.size() > 0) {
                            currentSong = songQueue.get(0);
                            currentSong.setTime(0);
                            currentSong.play();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
        if (songQueue.isEmpty()) return;
        currentSong = songQueue.get(0);
        currentSong.setTime(0);
        currentSong.play();
    }

    public BotBossBar addBossBar () {
        if (currentSong == null) return null;

        final BotBossBar bossBar = new BotBossBar(
                Component.empty(),
                BOTH_SELECTOR,
                BossBarColor.LIME,
                BossBarDivision.NONE,
                true,
                (int) currentSong.length / 1000,
                0,
                bot
        );

        bot.bossbar.add(bossbarName, bossBar);

        return bossBar;
    }

    public void removeBossBar() {
        bot.bossbar.remove(bossbarName);
    }

    public Component generateBossbar () {
        NamedTextColor namedTextColor;
        if (pitch > 0) {
            namedTextColor = NamedTextColor.LIGHT_PURPLE;
            bossBarColor = BossBarColor.PURPLE;
        } else if (pitch < 0) {
            namedTextColor = NamedTextColor.AQUA;
            bossBarColor = BossBarColor.CYAN;
        } else {
            namedTextColor = NamedTextColor.GREEN;
            bossBarColor = BossBarColor.YELLOW;
        }

        Component component = Component.empty()
                .append(Component.empty().append(Component.text(currentSong.name)).color(namedTextColor))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(
                        Component
                                .translatable("%s / %s",
                                        formatTime((long) (currentSong.time * speed)).color(NamedTextColor.GRAY),
                                        formatTime((long) (currentSong.length * speed)).color(NamedTextColor.GRAY)).color(NamedTextColor.DARK_GRAY)
                );

        if (!bot.core.hasRateLimit()) {
            final DecimalFormat formatter = new DecimalFormat("#,###");

            component = component
                    .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                    .append(
                            Component.translatable(
                                    "%s / %s",
                                    Component.text(formatter.format(currentSong.position), NamedTextColor.GRAY),
                                    Component.text(formatter.format(currentSong.size()), NamedTextColor.GRAY)
                            ).color(NamedTextColor.DARK_GRAY)
                    );
        }

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
        removeBossBar();
        currentSong = null;
        notesPerSecond = 0;
    }

    @Override
    public void disconnected (DisconnectedEvent event) {
        if (currentSong != null) currentSong.pause(); // nice.
    }

    public void handlePlaying () {
        try {
            currentSong.advanceTime();
            while (currentSong.reachedNextNote()) {
                final Note note = currentSong.getNextNote();

                final int totalCoreBlocks = (bot.config.core.end.x * bot.config.core.end.z) * MathUtilities.clamp(bot.config.core.end.y, 1, bot.world.maxY);

                if (note.volume == 0 || notesPerSecond > totalCoreBlocks * (50 * 20)) continue;

                float key = note.shiftedPitch;

                float blockPosition = 0;

                // code idea stolen from OpenNBS
                if (currentSong.nbs) {
                    final int average = (note.stereo + note.panning) / 2;

                    // The stereo position of the note block, from 0-200. 0 is 2 blocks right, 100 is center, 200 is 2 blocks left.
                    // and
                    // How much this layer is panned to the left/right. 0 is 2 blocks right, 100 is center, 200 is 2 blocks left.
                    // (taken from https://opennbs.org/nbs)
                    if (average > 100) blockPosition = -(float) average / 100; // left
                    else if (average < 100) blockPosition = (float) average / 100; // right
                } else {
                    // is this better than using the pitches??

                    if (stereoNoteIndex > 4) {
                        // is there already a function for this?
                        if (stereoPosition == -0.2F) stereoPosition = 0.2F;
                        else if (stereoPosition == 0.2F) stereoPosition = -0.2F;

                        stereoNoteIndex = 0;
                    }

                    blockPosition = stereoPosition;

                    stereoNoteIndex++;
                }

                final double notShiftedFloatingPitch = Math.pow(2.0, ((note.pitch + (pitch / 10)) - 12) / 12.0);

                key += 33;

                final boolean isMoreOrLessOctave = key < 33 || key > 57;

                final boolean shouldCustomPitch = currentSong.nbs ?
                        isMoreOrLessOctave :
                        note.pitch != note.shiftedPitch ||
                                note.shiftedInstrument != note.instrument;

                if (shouldCustomPitch) {
                    bot.core.run(
                            "minecraft:execute as " +
                                    CUSTOM_PITCH_SELECTOR +
                                    " at @s run playsound " +
                                    (!instrument.equals("off") ? instrument : note.instrument.sound) + ".pitch." + notShiftedFloatingPitch +
                                    " record @s ^" + blockPosition + " ^1 ^ " +
                                    note.volume +
                                    " " +
                                    0
                    );
                }

                // these 2 lines are totallynotskidded from https://github.com/OpenNBS/OpenNoteBlockStudio/blob/master/scripts/selection_transpose/selection_transpose.gml
                // so huge thanks to them uwu
                while (key < 33) key += 12; // 1 octave has 12 notes so we just keep moving octaves here
                while (key > 57) key -= 12;

                key -= 33;

                double floatingPitch = Math.pow(2.0, ((key + (pitch / 10)) - 12) / 12.0);

                for (int i = 0; i < amplify; i++) {
                    bot.core.run(
                            "minecraft:execute as " +
                                    (shouldCustomPitch ? SELECTOR : BOTH_SELECTOR) +
                                    " at @s run playsound " +
                                    (!instrument.equals("off") ? instrument : note.shiftedInstrument.sound) +
                                    " record @s ^" + blockPosition + " ^1 ^ " +
                                    note.volume +
                                    " " +
                                    MathUtilities.clamp(floatingPitch, 0, 2)
                    );
                }

                notesPerSecond++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
