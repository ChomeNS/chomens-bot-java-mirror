package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.bossbar.BotBossBar;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.song.Loop;
import me.chayapak1.chomens_bot.song.Note;
import me.chayapak1.chomens_bot.song.Song;
import me.chayapak1.chomens_bot.song.SongLoaderThread;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import me.chayapak1.chomens_bot.util.MathUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarDivision;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Author: _ChipMC_ & chayapak <3
public class MusicPlayerPlugin extends Bot.Listener {
    public static final String SELECTOR = "@a[tag=!nomusic,tag=!chomens_bot_nomusic,tag=!custompitch]";
    public static final String CUSTOM_PITCH_SELECTOR = "@a[tag=!nomusic,tag=!chomens_bot_nomusic,tag=custompitch]";
    public static final String BOTH_SELECTOR = "@a[tag=!nomusic,tag=!chomens_bot_nomusic]";

    private final Bot bot;

    public static final Path SONG_DIR = Path.of("songs");
    static {
        try {
            if (!Files.exists(SONG_DIR)) Files.createDirectory(SONG_DIR);
        } catch (IOException e) {
            LoggerUtilities.error(e);
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

    private int urlLimit = 0;

    public boolean locked = false; // this can be set through servereval

    private final String bossBarName = "music";
    public BossBarColor bossBarColor;

    public String currentLyrics = "";

    public MusicPlayerPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);

        bot.core.addListener(new CorePlugin.Listener() {
            @Override
            public void ready() {
                onCoreReady();
            }
        });

        bot.executor.scheduleAtFixedRate(this::onTick, 0, 50, TimeUnit.MILLISECONDS);
        bot.executor.scheduleAtFixedRate(() -> urlLimit = 0, 0, bot.config.music.urlRatelimit.seconds, TimeUnit.SECONDS);
    }

    public void loadSong (Path location, PlayerEntry sender) {
        if (songQueue.size() > 500) return;

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
        if (songQueue.size() > 500) return;

        urlLimit++;

        if (urlLimit > bot.config.music.urlRatelimit.limit) {
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

    public void loadSong (byte[] data, PlayerEntry sender) {
        if (songQueue.size() > 500) return;

        loaderThread = new SongLoaderThread(data, bot, sender.profile.getName());

        bot.chat.tellraw(
                Component
                        .translatable(
                                "Loading %s",
                                Component.text(sender.profile.getName() + "'s song item", ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                        )
                        .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
        );

        loaderThread.start();
    }

    private void onCoreReady () {
        if (currentSong != null) currentSong.play();
    }

    // this needs a separate ticker because we need
    // the song to be playing without lag
    private void onTick () {
        try {
            if (!bot.loggedIn) return;

            if (currentSong == null) {
                if (songQueue.isEmpty()) return; // this line

                addBossBar();

                currentSong = songQueue.getFirst(); // songQueue.poll();
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
                handleLyrics();

                BotBossBar bossBar = bot.bossbar.get(bossBarName);

                if (bossBar == null && bot.bossbar.enabled) bossBar = addBossBar();

                if (bot.bossbar.enabled && bot.options.useCore) {
                    bossBar.setTitle(generateBossBar());
                    bossBar.setColor(bossBarColor);
                    bossBar.setValue((int) Math.floor(((double) (currentSong.time * speed) / 1000)));
                    bossBar.setMax((long) (currentSong.length * speed) / 1000);
                }

                if (currentSong.paused || bot.core.isRateLimited()) return;

                handlePlaying();
            } else {
                currentLyrics = "";

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
        } catch (Exception e) {
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

        bot.bossbar.add(bossBarName, bossBar);

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

    public void removeBossBar() {
        final BotBossBar bossBar = bot.bossbar.get(bossBarName);

        if (bossBar != null) bossBar.setTitle(Component.text("No song is currently playing"));

        bot.bossbar.remove(bossBarName);
    }

    public Component generateBossBar () {
        NamedTextColor nameColor;
        if (pitch > 0) {
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
                                        formatTime((long) (currentSong.time * speed)).color(NamedTextColor.GRAY),
                                        formatTime((long) (currentSong.length * speed)).color(NamedTextColor.GRAY)).color(NamedTextColor.DARK_GRAY)
                );

        if (!bot.core.hasRateLimit() && !currentLyrics.isEmpty()) {
            component = component
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(currentLyrics).color(NamedTextColor.BLUE));
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
    }

    @Override
    public void disconnected (DisconnectedEvent event) {
        if (currentSong != null) currentSong.pause(); // nice.
    }

    public void handlePlaying () {
        if (currentSong == null) return;

        try {
            currentSong.advanceTime();
            while (currentSong.reachedNextNote()) {
                final Note note = currentSong.getNextNote();

                if (note.volume == 0) continue;

                float key = note.shiftedPitch;

                final Vector3f blockPosition = getBlockPosition(note);

                final float notShiftedFloatingPitch = (float) Math.pow(2.0, ((note.pitch + (pitch / 10)) - 12) / 12.0);

                key += 33;

                final boolean isMoreOrLessOctave = key < 33 || key > 57;

                final boolean shouldCustomPitch = currentSong.nbs ?
                        isMoreOrLessOctave :
                        note.pitch != note.shiftedPitch ||
                                note.shiftedInstrument != note.instrument;

                final double volume = note.volume;

                if (shouldCustomPitch) {
                    bot.core.run(
                            "minecraft:execute as " +
                                    CUSTOM_PITCH_SELECTOR +
                                    " at @s run playsound " +
                                    (!instrument.equals("off") ? instrument : note.instrument.sound) + ".pitch." + notShiftedFloatingPitch +
                                    " record @s ^" + blockPosition.getX() + " ^" + blockPosition.getY() + " ^" + blockPosition.getZ() + " " +
                                    volume +
                                    " " +
                                    0 +
                                    " 1"
                    );
                }

                // these 2 lines are totallynotskidded from https://github.com/OpenNBS/OpenNoteBlockStudio/blob/master/scripts/selection_transpose/selection_transpose.gml
                // so huge thanks to them uwu
                while (key < 33) key += 12; // 1 octave has 12 notes, so we just keep moving octaves here
                while (key > 57) key -= 12;

                key -= 33;

                float floatingPitch = (float) (0.5 * Math.pow(2, (key + (pitch / 10)) / 12));

                for (int i = 0; i < amplify; i++) {
                    bot.core.run(
                            "minecraft:execute as " +
                                    (shouldCustomPitch ? SELECTOR : BOTH_SELECTOR) +
                                    " at @s run playsound " +
                                    (!instrument.equals("off") ? instrument : note.shiftedInstrument.sound) +
                                    " record @s ^" + blockPosition.getX() + " ^" + blockPosition.getY() + " ^" + blockPosition.getZ() + " " +
                                    volume +
                                    " " +
                                    MathUtilities.clamp(floatingPitch, 0, 2) +
                                    " 1"
                    );
                }
            }
        } catch (Exception e) {
            bot.logger.error(e);
        }
    }

    private Vector3f getBlockPosition (Note note) {
        Vector3f blockPosition;

        if (currentSong.nbs) {
            // https://github.com/OpenNBS/OpenNoteBlockStudio/blob/45f35ea193268fb541c1297d0b656f4964339d97/scripts/dat_generate/dat_generate.gml#L22C18-L22C31
            final int average = (note.stereo + note.panning) / 2;

            float pos;

            if (average > 100) pos = (float) (average - 100) / -100;
            else if (average == 100) pos = 0;
            else pos = (float) ((average - 100) * -1) / 100;

            blockPosition = Vector3f.from(pos * 2, 0, 0);
        } else {
            // i wrote this part

            final int originalPitch = note.originalPitch;

            float xPos = -(float) originalPitch / 768;
            if (originalPitch > 25) xPos = Math.abs(xPos);

            float yPos = -(float) originalPitch / 35;
            if (originalPitch < 75) yPos = -yPos;

            float zPos = -(float) originalPitch / 40;
            if (originalPitch < 75) zPos = -zPos;

            blockPosition = Vector3f.from(xPos, yPos, zPos);
        }

        return blockPosition;
    }
}
