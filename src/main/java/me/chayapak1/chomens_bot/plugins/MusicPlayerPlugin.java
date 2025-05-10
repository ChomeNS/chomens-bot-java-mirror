package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.data.bossbar.BotBossBar;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.song.Loop;
import me.chayapak1.chomens_bot.song.Note;
import me.chayapak1.chomens_bot.song.Song;
import me.chayapak1.chomens_bot.song.SongLoaderThread;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import me.chayapak1.chomens_bot.util.MathUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarDivision;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Author: _ChipMC_ & hhhzzzsss
public class MusicPlayerPlugin implements Listener {
    public static final String SELECTOR = "@a[tag=!nomusic,tag=!chomens_bot_nomusic,tag=!custompitch]";
    public static final String CUSTOM_PITCH_SELECTOR = "@a[tag=!nomusic,tag=!chomens_bot_nomusic,tag=custompitch]";
    public static final String BOTH_SELECTOR = "@a[tag=!nomusic,tag=!chomens_bot_nomusic]";

    public static final Path SONG_DIR = Path.of("songs");

    private static final String BOSS_BAR_NAME = "music";

    private static final DecimalFormat FORMATTER = new DecimalFormat("#,###");

    static {
        try {
            if (!Files.exists(SONG_DIR)) Files.createDirectory(SONG_DIR);
        } catch (final IOException e) {
            LoggerUtilities.error(e);
        }
    }

    private final Bot bot;

    public Song currentSong;
    public final List<Song> songQueue = Collections.synchronizedList(new ObjectArrayList<>());
    public SongLoaderThread loaderThread = null;
    public Loop loop = Loop.OFF;

    // sus nightcore stuff,..,.,.
    public double pitch = 0;
    public double speed = 1;

    public float volume = 0;
    public int amplify = 1;

    public String instrument = "off";

    public boolean rainbow = false; // nbs easter egg
    private float rainbowHue = 0F;

    public BossBarColor bossBarColor = BossBarColor.YELLOW;

    private int urlLimit = 0;

    public boolean locked = false; // this can be set through servereval

    private boolean isStopping = false;

    public String currentLyrics = "";

    public MusicPlayerPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);

        bot.executor.scheduleAtFixedRate(this::onMusicTick, 0, 50, TimeUnit.MILLISECONDS);
        bot.executor.scheduleAtFixedRate(() -> urlLimit = 0, 0, bot.config.music.urlRatelimit.seconds, TimeUnit.SECONDS);
    }

    public void loadSong (final Path location, final CommandContext context) {
        startLoadingSong(
                location.getFileName().toString(),
                new SongLoaderThread(location, bot, context)
        );
    }

    public void loadSong (final byte[] data, final CommandContext context) {
        startLoadingSong(
                context.sender.profile.getName() + "'s song item",
                new SongLoaderThread(data, bot, context)
        );
    }

    public void loadSong (final URL location, final CommandContext context) {
        if (urlLimit >= bot.config.music.urlRatelimit.limit) {
            context.sendOutput(Component.translatable("commands.music.error.url_ratelimited", NamedTextColor.RED));
            return;
        }

        urlLimit++;

        startLoadingSong(
                location.toString(),
                new SongLoaderThread(location, bot, context)
        );
    }

    private void startLoadingSong (final String songName, final SongLoaderThread loaderThread) {
        if (songQueue.size() > 500) return;

        this.loaderThread = loaderThread;

        loaderThread.context.sendOutput(
                Component
                        .translatable(
                                "commands.music.loading",
                                bot.colorPalette.defaultColor,
                                Component.text(songName, bot.colorPalette.secondary)
                        )
        );

        this.loaderThread.start();
    }

    @Override
    public void onCoreReady () {
        if (currentSong != null) currentSong.play();
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        if (currentSong != null) currentSong.pause(); // nice.
        loaderThread = null;
    }

    // this needs a separate ticker because we need
    // the song to be playing without lag
    private void onMusicTick () {
        try {
            if (!bot.loggedIn) return;

            if (currentSong == null) {
                if (songQueue.isEmpty()) return; // this line

                addBossBar();

                currentSong = songQueue.getFirst(); // songQueue.poll();
                currentSong.context.sendOutput(
                        Component.translatable(
                                "commands.music.nowplaying",
                                bot.colorPalette.defaultColor,
                                Component.empty().append(Component.text(currentSong.name)).color(bot.colorPalette.secondary)
                        )
                );
                currentSong.play();
            }

            if (isStopping) {
                currentSong = null;
                isStopping = false;
            } else if (!currentSong.finished()) {
                handleLyrics();

                BotBossBar bossBar = bot.bossbar.get(BOSS_BAR_NAME);

                if (bossBar == null) bossBar = addBossBar();

                if (bossBar != null && bot.options.useCore) {
                    bossBar.setTitle(generateBossBar());
                    bossBar.setColor(bossBarColor);
                    bossBar.setValue((int) Math.floor(((currentSong.time / speed) / 1000)));
                    bossBar.setMax((long) (currentSong.length / speed) / 1000);
                }

                if (currentSong.paused || bot.core.isRateLimited()) return;

                handlePlaying();
            } else {
                currentLyrics = "";

                if (loop == Loop.CURRENT) {
                    currentSong.loop();
                    return;
                }

                currentSong.context.sendOutput(
                        Component.translatable(
                                "commands.music.finished",
                                bot.colorPalette.defaultColor,
                                Component.empty().append(Component.text(currentSong.name)).color(bot.colorPalette.secondary)
                        )
                );

                if (loop == Loop.ALL) {
                    skip();
                    return;
                }

                songQueue.removeFirst();

                if (songQueue.isEmpty()) {
                    stopPlaying();
                    return;
                }

                if (currentSong.size() > 0) {
                    currentSong = songQueue.getFirst();
                    currentSong.setTime(0);
                    currentSong.play();
                }
            }
        } catch (final Exception e) {
            bot.logger.error(e);
        }
    }

    public void skip () {
        if (loop == Loop.ALL) {
            songQueue.add(songQueue.removeFirst()); // bot.music.queue.push(bot.music.queue.shift()) in js
        } else {
            songQueue.removeFirst();
        }
        if (songQueue.isEmpty()) {
            stopPlaying();
            return;
        }
        currentSong = songQueue.getFirst();
        currentSong.setTime(0);
        currentSong.play();
    }

    public BotBossBar addBossBar () {
        if (currentSong == null) return null;

        rainbow = false;

        final BotBossBar bossBar = new BotBossBar(
                Component.empty(),
                BOTH_SELECTOR,
                bossBarColor,
                BossBarDivision.NONE,
                true,
                (int) currentSong.length / 1000,
                0,
                bot
        );

        bot.bossbar.add(BOSS_BAR_NAME, bossBar);

        return bossBar;
    }

    private void handleLyrics () {
        // please help, this is many attempts trying to get this working
        // midi lyrics are very weird
        // i need some karaoke players to see how this works

        //        final Map<Long, String> lyrics = currentSong.lyrics;
        //
        //        if (lyrics.isEmpty()) return;
        //
        //        final List<String> lyricsList = new ArrayList<>();
        //
        //        for (Map.Entry<Long, String> entry : lyrics.entrySet()) {
        //            final long time = entry.getKey();
        //            String _lyric = entry.getValue();
        //
        //            if (time > currentSong.time) continue;
        //
        ////            StringBuilder lyric = new StringBuilder();
        ////
        ////            for (char character : _lyric.toCharArray()) {
        ////                if ((character != '\n' && character != '\r' && character < ' ') || character == '�') continue;
        ////
        ////                lyric.append(character);
        ////            }
        ////
        ////            String stringLyric = lyric.toString();
        ////
        ////            if (stringLyric.startsWith("\\") || stringLyric.startsWith("/")) {
        ////                lyricsList.clear();
        ////
        ////                stringLyric = stringLyric.substring(1);
        ////            }
        //
        //            lyricsList.add(_lyric);
        //        }
        //
        //        final String joined = String.join("", lyricsList);
        //        currentLyrics = joined.substring(Math.max(0, joined.length() - 25));
    }

    public void removeBossBar () {
        final BotBossBar bossBar = bot.bossbar.get(BOSS_BAR_NAME);

        if (bossBar != null) bossBar.setTitle(Component.translatable("commands.music.error.not_playing"));

        bot.bossbar.remove(BOSS_BAR_NAME);
    }

    public Component generateBossBar () {
        final TextColor nameColor;

        if (rainbow) {
            final int increment = 360 / 20;
            nameColor = TextColor.color(HSVLike.hsvLike(rainbowHue / 360.0f, 1, 1));
            rainbowHue = (rainbowHue + increment) % 360;

            bossBarColor = BossBarColor.YELLOW;
        } else if (pitch > 0) {
            nameColor = NamedTextColor.LIGHT_PURPLE;
            bossBarColor = BossBarColor.PURPLE;
        } else if (pitch < 0) {
            nameColor = NamedTextColor.AQUA;
            bossBarColor = BossBarColor.CYAN;
        } else {
            nameColor = NamedTextColor.GREEN;
            bossBarColor = BossBarColor.YELLOW;
        }

        Component component = Component.empty()
                .append(Component.empty().append(Component.text(currentSong.name)).color(nameColor))
                .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                .append(
                        Component
                                .translatable("%s / %s",
                                              formatTime((long) (currentSong.time / speed)).color(NamedTextColor.GRAY),
                                              formatTime((long) (currentSong.length / speed)).color(NamedTextColor.GRAY)).color(NamedTextColor.DARK_GRAY)
                );

        if (!bot.core.hasRateLimit()) {
            component = component
                    .append(Component.text(" | ").color(NamedTextColor.DARK_GRAY))
                    .append(
                            Component.translatable(
                                    "%s / %s",
                                    Component.text(FORMATTER.format(currentSong.position), NamedTextColor.GRAY),
                                    Component.text(FORMATTER.format(currentSong.size()), NamedTextColor.GRAY)
                            ).color(NamedTextColor.DARK_GRAY)
                    );

            if (!currentLyrics.isBlank()) {
                component = component
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(currentLyrics).color(NamedTextColor.BLUE));
            }
        }

        if (currentSong.paused) {
            return component
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("⏸", NamedTextColor.LIGHT_PURPLE));
        }

        if (loop != Loop.OFF) {
            return component
                    .append(Component.translatable(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.translatable("Looping " + ((loop == Loop.CURRENT) ? "current" : "all"), NamedTextColor.LIGHT_PURPLE));
        }

        return component;
    }

    public Component formatTime (final long millis) {
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
        isStopping = true;
    }

    public void handlePlaying () {
        if (currentSong == null) return;

        currentSong.advanceTime();
        while (currentSong.reachedNextNote()) {
            final Note note = currentSong.getNextNote();

            try {
                if (note.isRainbowToggle) {
                    rainbow = !rainbow;
                    continue;
                }

                double key = note.shiftedPitch;

                final Vector3d blockPosition = note.position;

                final double notShiftedFloatingPitch = 0.5 * Math.pow(2, (note.pitch + (pitch / 10)) / 12);

                key += 33;

                final boolean isMoreOrLessOctave = key < 33 || key > 57;

                final boolean shouldCustomPitch = currentSong.nbs ?
                        isMoreOrLessOctave :
                        note.pitch != note.shiftedPitch ||
                                note.shiftedInstrument != note.instrument;

                final double volume = note.volume + this.volume;

                if (volume == 0.0) continue;

                if (shouldCustomPitch) {
                    bot.core.run(
                            "minecraft:execute as " +
                                    CUSTOM_PITCH_SELECTOR +
                                    " at @s run playsound " +
                                    (!instrument.equals("off") ? instrument : note.instrument.sound) + ".pitch." + notShiftedFloatingPitch +
                                    " record @s ^" + blockPosition.getX() + " ^" + blockPosition.getY() + " ^" + blockPosition.getZ() + " " +
                                    volume +
                                    " " +
                                    0
                    );
                }

                // these 2 lines are totallynotskidded from https://github.com/OpenNBS/OpenNoteBlockStudio/blob/master/scripts/selection_transpose/selection_transpose.gml
                // so huge thanks to them uwu
                while (key < 33) key += 12; // 1 octave has 12 notes, so we just keep moving octaves here
                while (key > 57) key -= 12;

                key -= 33;

                final double floatingPitch = 0.5 * Math.pow(2, (key + (pitch / 10)) / 12);

                for (int i = 0; i < amplify; i++) {
                    bot.core.run(
                            "minecraft:execute as " +
                                    (shouldCustomPitch ? SELECTOR : BOTH_SELECTOR) +
                                    " at @s run playsound " +
                                    (!instrument.equals("off") ? instrument : note.shiftedInstrument.sound) +
                                    " record @s ^" + blockPosition.getX() + " ^" + blockPosition.getY() + " ^" + blockPosition.getZ() + " " +
                                    volume +
                                    " " +
                                    MathUtilities.clamp(floatingPitch, 0, 2)
                    );
                }
            } catch (final Exception e) {
                bot.logger.error(e);
            }
        }
    }
}
