package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.command.contexts.ConsoleCommandContext;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.plugins.MusicPlayerPlugin;
import me.chayapak1.chomens_bot.song.Instrument;
import me.chayapak1.chomens_bot.song.Loop;
import me.chayapak1.chomens_bot.song.Note;
import me.chayapak1.chomens_bot.song.Song;
import me.chayapak1.chomens_bot.util.Ascii85;
import me.chayapak1.chomens_bot.util.PathUtilities;
import me.chayapak1.chomens_bot.util.TimestampUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static me.chayapak1.chomens_bot.util.StringUtilities.isNotNullAndNotBlank;

public class MusicCommand extends Command {
    private static final Path ROOT = MusicPlayerPlugin.SONG_DIR;

    public MusicCommand () {
        super(
                "music",
                "Plays music",
                new String[] {
                        "play <song|URL>",
                        "playitem",
                        "stop",
                        "loop <current|all|off>",
                        "list [directory]",
                        "skip",
                        "nowplaying",
                        "queue",
                        "goto <timestamp>",
                        "pitch <pitch>",
                        "speed <speed>",
                        "volume <volume modifier>",
                        "amplify <amplification>",
                        "noteinstrument <instrument>",
                        "pause",
                        "resume",
                        "info"
                },
                new String[] { "song" },
                TrustLevel.PUBLIC,
                false,
                new ChatPacketType[] { ChatPacketType.DISGUISED }
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        if (context.bot.music.locked && !(context instanceof ConsoleCommandContext))
            throw new CommandException(Component.text("Managing music is currently locked"));

        final String action = context.getAction();

        return switch (action) {
            case "play", "playurl", "playnbs", "playnbsurl" -> play(context);
            case "playfromitem", "playitem", "playsongplayer" -> playFromItem(context);
            case "stop" -> stop(context);
            case "loop" -> loop(context);
            case "list" -> list(context);
            case "skip" -> skip(context);
            case "nowplaying" -> nowPlaying(context);
            case "queue" -> queue(context);
            case "goto" -> goTo(context);
            case "pitch" -> pitch(context);
            case "speed" -> speed(context);
            case "volume" -> volume(context);
            case "amplify" -> amplify(context);
            case "noteinstrument" -> noteInstrument(context);
            case "pause", "resume" -> pause(context);
            case "info" -> info(context);
            case "testsong" -> testSong(context);
            default -> Component.text("Invalid action").color(NamedTextColor.RED);
        };
    }

    public Component play (final CommandContext context) throws CommandException {
        final MusicPlayerPlugin player = context.bot.music;

        if (player.loaderThread != null) throw new CommandException(Component.text("Already loading a song"));

        final String stringPath = context.getString(true, true);

        final Path path;

        try {
            path = Path.of(ROOT.toString(), stringPath);

            if (path.toString().contains("http")) player.loadSong(new URI(stringPath).toURL(), context.sender);
            else {
                // among us protection!!!11
                if (!path.normalize().startsWith(ROOT.toString())) throw new CommandException(Component.text("no"));

                // ignore my ohio code for autocomplete
                final String separator = FileSystems.getDefault().getSeparator();

                if (stringPath.contains(separator) && !stringPath.isEmpty()) {
                    final String[] splitPath = stringPath.split(separator);

                    final List<String> splitPathClone = new ArrayList<>(Arrays.stream(splitPath).toList());
                    splitPathClone.removeLast();

                    final Path realPath = Path.of(ROOT.toString(), String.join(separator, splitPathClone));

                    try (final DirectoryStream<Path> stream = Files.newDirectoryStream(realPath)) {
                        final List<Path> songsPaths = new ArrayList<>();
                        for (final Path eachPath : stream) songsPaths.add(eachPath);

                        PathUtilities.sort(songsPaths);

                        final List<String> songs = new ArrayList<>();
                        for (final Path eachPath : songsPaths) songs.add(eachPath.getFileName().toString());

                        final String lowerCaseFile = splitPath[splitPath.length - 1].toLowerCase();

                        final String[] matchedArray = songs.stream()
                                .filter(song -> song.equalsIgnoreCase(lowerCaseFile) || song.toLowerCase().contains(lowerCaseFile))
                                .toArray(String[]::new);

                        if (matchedArray.length == 0) throw new CommandException(Component.text("Song not found"));

                        final String file = matchedArray[0];

                        player.loadSong(Path.of(realPath.toString(), file), context.sender);
                    } catch (final NoSuchFileException e) {
                        throw new CommandException(Component.text("Directory does not exist"));
                    }
                } else {
                    try (final DirectoryStream<Path> stream = Files.newDirectoryStream(ROOT)) {
                        final List<Path> songsPaths = new ArrayList<>();
                        for (final Path eachPath : stream) songsPaths.add(eachPath);

                        PathUtilities.sort(songsPaths);

                        final List<String> songs = new ArrayList<>();
                        for (final Path eachPath : songsPaths) songs.add(eachPath.getFileName().toString());

                        final String[] matchedArray = songs.stream()
                                .filter(song -> song.equalsIgnoreCase(stringPath) || song.toLowerCase().contains(stringPath.toLowerCase()))
                                .toArray(String[]::new);

                        if (matchedArray.length == 0) throw new CommandException(Component.text("Song not found"));

                        final String file = matchedArray[0];

                        player.loadSong(Path.of(ROOT.toString(), file), context.sender);
                    } catch (final NoSuchFileException e) {
                        throw new CommandException(Component.text("this will never happen ok??"));
                    }
                }
            }
        } catch (final MalformedURLException e) {
            throw new CommandException(Component.text("Invalid URL"));
        } catch (final IndexOutOfBoundsException e) {
            throw new CommandException(Component.text("Song not found"));
        } catch (final CommandException e) {
            throw e;
        } catch (final Exception e) {
            throw new CommandException(Component.text(e.toString()));
        }

        return null;
    }

    public Component playFromItem (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;

        final CompletableFuture<String> future = bot.query.entity(
                context.sender.profile.getIdAsString(),
                "SelectedItem.components.minecraft:custom_data.SongItemData.SongData"
        );

        future.thenApply(output -> {
            if (output.isEmpty()) {
                context.sendOutput(Component.text("Player has no `SongItemData.SongData` NBT tag in their selected item's minecraft:custom_data").color(NamedTextColor.RED));
                return null;
            }

            try {
                bot.music.loadSong(
                        Base64.getDecoder().decode(output),
                        context.sender
                );
            } catch (final IllegalArgumentException e) {
                try {
                    bot.music.loadSong(
                            Ascii85.decode(output),
                            context.sender
                    );
                } catch (final IllegalArgumentException e2) {
                    context.sendOutput(Component.text("Invalid Base64 or Ascii85 in the selected item").color(NamedTextColor.RED));
                }
            }

            return output;
        });

        return null;
    }

    public Component stop (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;
        bot.music.stopPlaying();
        bot.music.songQueue.clear();
        bot.music.loaderThread = null;

        return Component.text("Cleared the song queue").color(bot.colorPalette.defaultColor);
    }

    public Component loop (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(2);

        final Bot bot = context.bot;

        final Loop loop = context.getEnum(Loop.class);

        bot.music.loop = loop;

        switch (loop) {
            case OFF -> {
                return Component.empty()
                        .append(Component.text("Looping is now "))
                        .append(Component.text("disabled").color(NamedTextColor.RED))
                        .color(bot.colorPalette.defaultColor);
            }
            case CURRENT -> {
                if (bot.music.currentSong != null) {
                    return Component.empty()
                            .append(Component.text("Now looping "))
                            .append(Component.text(bot.music.currentSong.name).color(bot.colorPalette.secondary))
                            .color(bot.colorPalette.defaultColor);
                } else {
                    return Component.empty()
                            .append(Component.text("Will now loop the next song"))
                            .color(bot.colorPalette.defaultColor);
                }
            }
            case ALL -> {
                return Component.text("Now looping every song").color(bot.colorPalette.defaultColor);
            }
            default -> throw new CommandException(Component.text("Invalid action"));
        }
    }

    public Component list (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String prefix = context.prefix;

        final String stringPathIfExists = context.getString(true, false);

        final Path path = Path.of(
                ROOT.toString(),
                stringPathIfExists
        );

        if (!path.normalize().startsWith(ROOT.toString())) throw new CommandException(Component.text("no"));

        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            final List<Path> paths = new ArrayList<>();
            for (final Path eachPath : stream) paths.add(eachPath);

            PathUtilities.sort(paths);

            final List<Component> fullList = new ArrayList<>();
            int i = 0;
            for (final Path eachPath : paths) {
                final boolean isDirectory = Files.isDirectory(eachPath);

                Path location;
                try {
                    location = path;
                } catch (final IllegalArgumentException e) {
                    location = Paths.get(""); // wtf mabe
                }

                final String joinedPath = location.equals(ROOT) ?
                        eachPath.getFileName().toString() :
                        Paths.get(
                                location.getFileName().toString(),
                                eachPath.getFileName().toString()
                        ).toString();

                fullList.add(
                        Component
                                .text(eachPath.getFileName().toString(), (i++ & 1) == 0 ? bot.colorPalette.primary : bot.colorPalette.secondary)
                                .clickEvent(
                                        ClickEvent.suggestCommand(
                                                prefix +
                                                        name +
                                                        (isDirectory ? " list " : " play ") +
                                                        joinedPath
                                        )
                                )
                );
            }

            final int eachSize = 100;

            int index = 0;

            while (index <= fullList.size()) {
                // we MUST make a new copy of the list else everything will fard..,.
                final List<Component> list = new ArrayList<>(fullList).subList(index, Math.min(index + eachSize, fullList.size()));

                final Component component = Component.join(JoinConfiguration.separator(Component.space()), list);
                context.sendOutput(component);

                index += eachSize;
                list.clear();
            }
        } catch (final IOException e) {
            throw new CommandException(Component.text("Directory doesn't exist"));
        }

        return null;
    }

    public Component skip (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;
        final MusicPlayerPlugin music = bot.music;
        if (music.currentSong == null) throw new CommandException(Component.text("No song is currently playing"));

        context.sendOutput(
                Component.empty()
                        .append(Component.text("Skipping "))
                        .append(Component.text(music.currentSong.name).color(bot.colorPalette.secondary))
                        .color(bot.colorPalette.defaultColor)
        );

        music.skip();

        return null;
    }

    public Component nowPlaying (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;
        final Song song = bot.music.currentSong;
        if (song == null) throw new CommandException(Component.text("No song is currently playing"));

        return Component.empty()
                .append(Component.text("Now playing "))
                .append(Component.text(song.name).color(bot.colorPalette.secondary))
                .color(bot.colorPalette.defaultColor);
    }

    public Component queue (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;
        final List<Song> queue = bot.music.songQueue;

        final List<Component> queueWithNames = new ArrayList<>();
        int i = 0;
        for (final Song song : queue) {
            queueWithNames.add(
                    Component.text(song.name).color((i++ & 1) == 0 ? bot.colorPalette.primary : bot.colorPalette.secondary)
            );
        }

        return Component.empty()
                .append(Component.text("Queue: ").color(NamedTextColor.GREEN))
                .append(Component.join(JoinConfiguration.separator(Component.text(", ")), queueWithNames));
    }

    // lazy fix for java using "goto" as keyword real
    public Component goTo (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        final String input = context.getString(true, true);

        final long timestamp = TimestampUtilities.parseTimestamp(input);

        if (currentSong == null) throw new CommandException(Component.text("No song is currently playing"));

        if (timestamp < 0 || timestamp > currentSong.length * bot.music.speed)
            throw new CommandException(Component.text("Invalid timestamp"));

        currentSong.setTime(timestamp);

        return Component
                .text("Set the time to ")
                .append(Component.text(input).color(bot.colorPalette.number))
                .color(bot.colorPalette.defaultColor);
    }

    public Component pitch (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(2);

        final Bot bot = context.bot;

        final float pitch = context.getFloat(true, false);

        bot.music.pitch = pitch;

        return Component.empty()
                .append(Component.text("Set the pitch to "))
                .append(Component.text(pitch).color(bot.colorPalette.number))
                .color(bot.colorPalette.defaultColor);
    }

    public Component speed (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(2);

        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        final double speed = context.getDouble(true, false);

        if (speed > 5) throw new CommandException(Component.text("Too fast!"));
        else if (speed < 0) throw new CommandException(Component.text("Invalid speed"));

        double oldTime = -1;

        if (currentSong != null) oldTime = currentSong.time / speed;

        bot.music.speed = speed;

        if (currentSong != null) currentSong.setTime(oldTime);

        return Component.empty()
                .append(Component.text("Set the speed to "))
                .append(Component.text(speed).color(bot.colorPalette.number))
                .color(bot.colorPalette.defaultColor);
    }

    public Component volume (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(2);

        final Bot bot = context.bot;

        final float volume = context.getFloat(true, false);

        bot.music.volume = volume;

        return Component.empty()
                .append(Component.text("Set the volume modifier to "))
                .append(Component.text(volume).color(bot.colorPalette.number))
                .color(bot.colorPalette.defaultColor);
    }

    public Component amplify (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(2);

        final Bot bot = context.bot;

        final int amplify = context.getInteger(true);

        if (amplify > 8) throw new CommandException(Component.text("Too big value"));
        else if (amplify < 0) throw new CommandException(Component.text("Invalid amplification value"));

        bot.music.amplify = amplify;

        return Component.empty()
                .append(Component.text("Set the amplification to "))
                .append(Component.text(amplify).color(bot.colorPalette.number))
                .color(bot.colorPalette.defaultColor);
    }

    public Component noteInstrument (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String instrument = context.getString(true, true);

        bot.music.instrument = instrument;

        if (!instrument.equals("off")) {
            return Component.empty()
                    .append(Component.text("Set the instrument for every note to "))
                    .append(Component.text(instrument).color(bot.colorPalette.secondary))
                    .color(bot.colorPalette.defaultColor);
        } else {
            return Component.text("Every notes are now using its instrument").color(bot.colorPalette.defaultColor);
        }
    }

    public Component pause (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        if (currentSong == null) throw new CommandException(Component.text("No song is currently playing"));

        if (currentSong.paused) {
            currentSong.play();
            return Component.text("Resumed the current song").color(bot.colorPalette.defaultColor);
        } else {
            currentSong.pause();
            return Component.text("Paused the current song").color(bot.colorPalette.defaultColor);
        }
    }

    public Component info (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        if (currentSong == null) throw new CommandException(Component.text("No song is currently playing"));

        final List<Component> components = new ArrayList<>();

        final TextColor keyColor = bot.colorPalette.secondary;
        final TextColor valueColor = bot.colorPalette.string;

        final DecimalFormat formatter = new DecimalFormat("#,###");

        final String formattedNotesCount = formatter.format(currentSong.size());

        if (isNotNullAndNotBlank(currentSong.name))
            components.add(Component.translatable("Title/Filename: %s", keyColor, Component.text(currentSong.name, valueColor)));
        if (isNotNullAndNotBlank(currentSong.requester))
            components.add(Component.translatable("Requested by: %s", keyColor, Component.text(currentSong.requester, valueColor)));
        if (isNotNullAndNotBlank(currentSong.songAuthor))
            components.add(Component.translatable("Author: %s", keyColor, Component.text(currentSong.songAuthor, valueColor)));
        if (isNotNullAndNotBlank(currentSong.songOriginalAuthor))
            components.add(Component.translatable("Original author: %s", keyColor, Component.text(currentSong.songOriginalAuthor, valueColor)));
        if (isNotNullAndNotBlank(currentSong.tracks))
            components.add(Component.translatable("Tracks: %s", keyColor, Component.text(currentSong.tracks, valueColor)));
        components.add(Component.translatable("Notes: %s", keyColor, Component.text(formattedNotesCount, valueColor)));
        if (isNotNullAndNotBlank(currentSong.songDescription))
            components.add(Component.translatable("Description: %s", keyColor, Component.text(currentSong.songDescription, valueColor)));

        return Component.join(JoinConfiguration.newlines(), components);
    }

    public Component testSong (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;

        final Song song = new Song(
                "test_song",
                bot,
                "Test Song",
                "chayapak",
                "hhhzzzsss",
                "SongPlayer's test song ported to ChomeNS Bot",
                null,
                false
        );

        int instrumentId = 0;
        int j = 0;
        for (int i = 0; i < 400; i++) {
            j++;

            song.add(
                    new Note(
                            Instrument.fromId(instrumentId),
                            j,
                            j,
                            1,
                            i * 50,
                            -1,
                            100,
                            false
                    )
            );

            if (j > 15) {
                instrumentId++;
                if (instrumentId > 15) instrumentId = 0;
                j = 0;
            }
        }

        song.length = 400 * 50;

        bot.music.songQueue.add(song);

        return Component.text("Test song has been added to the song queue").color(bot.colorPalette.defaultColor);
    }
}
