package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Main;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.plugins.MusicPlayerPlugin;
import land.chipmunk.chayapak.chomens_bot.song.Instrument;
import land.chipmunk.chayapak.chomens_bot.song.Loop;
import land.chipmunk.chayapak.chomens_bot.song.Note;
import land.chipmunk.chayapak.chomens_bot.song.Song;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.PathUtilities;
import land.chipmunk.chayapak.chomens_bot.util.TimestampUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MusicCommand extends Command {
    private Path root;

    private int ratelimit = 0;

    public MusicCommand () {
        super(
                "music",
                "Plays music",
                new String[] {
                        "play <song|URL>",
                        "stop",
                        "loop <current|all|off>",
                        "list [directory]",
                        "skip",
                        "nowplaying",
                        "queue",
                        "goto <timestamp>",
                        "pitch <pitch>",
                        "speed <speed>",
                        "noteinstrument <instrument>",
                        "pause",
                        "resume",
                        "info"
                },
                new String[] { "song" },
                TrustLevel.PUBLIC,
                false
        );

        Main.executor.scheduleAtFixedRate(() -> ratelimit = 0, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        ratelimit++;

        if (ratelimit > 10) return null;

        final String action = context.getString(false, true);

        root = MusicPlayerPlugin.SONG_DIR;
        return switch (action) {
            case "play", "playurl", "playnbs", "playnbsurl" -> play(context);
            case "stop" -> stop(context);
            case "loop" -> loop(context);
            case "list" -> list(context);
            case "skip" -> skip(context);
            case "nowplaying" -> nowplaying(context);
            case "queue" -> queue(context);
            case "goto" -> goTo(context);
            case "pitch" -> pitch(context);
            case "speed" -> speed(context);
            case "noteinstrument" -> noteInstrument(context);
            case "pause", "resume" -> pause(context);
            case "info" -> info(context);
            case "testsong" -> testSong(context);
            default -> Component.text("Invalid action").color(NamedTextColor.RED);
        };
    }

    public Component play (CommandContext context) throws CommandException {
        final MusicPlayerPlugin player = context.bot.music;

        String _path;
        Path path;
        try {
            _path = context.getString(true, true);

//            if (_path.isBlank()) throw new CommandException(Component.text("No song specified"));

            path = Path.of(root.toString(), _path);

            if (path.toString().contains("http")) player.loadSong(new URL(_path));
            else {
                // among us protection!!!11
                if (!path.normalize().startsWith(root.toString())) throw new CommandException(Component.text("no"));

                // ignore my ohio code for autocomplete
                final String separator = File.separator; // how do i do this with the new Files?

                if (_path.contains(separator) && !_path.equals("")) {
                    final String[] pathSplitted = _path.split(separator);

                    final List<String> pathSplittedClone = new ArrayList<>(Arrays.stream(pathSplitted.clone()).toList());
                    pathSplittedClone.remove(pathSplittedClone.size() - 1);

                    final Path realPath = Path.of(root.toString(), String.join(separator, pathSplittedClone));

                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(realPath)) {
                        final List<Path> songsPaths = new ArrayList<>();
                        for (Path eachPath : stream) songsPaths.add(eachPath);

                        PathUtilities.sort(songsPaths);

                        final List<String> songs = new ArrayList<>();
                        for (Path eachPath : songsPaths) songs.add(eachPath.getFileName().toString());

                        final String lowerCaseFile = pathSplitted[pathSplitted.length - 1].toLowerCase();

                        final String[] matchedArray = songs.stream()
                                .filter(song -> song.equalsIgnoreCase(lowerCaseFile) || song.toLowerCase().contains(lowerCaseFile))
                                .toArray(String[]::new);

                        if (matchedArray.length == 0) throw new CommandException(Component.text("Song not found"));

                        final String file = matchedArray[0];

                        player.loadSong(Path.of(realPath.toString(), file));
                    } catch (CommandException e) {
                        throw e;
                    } catch (NoSuchFileException e) {
                        throw new CommandException(Component.text("Directory does not exist"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
                        final List<Path> songsPaths = new ArrayList<>();
                        for (Path eachPath : stream) songsPaths.add(eachPath);

                        PathUtilities.sort(songsPaths);

                        final List<String> songs = new ArrayList<>();
                        for (Path eachPath : songsPaths) songs.add(eachPath.getFileName().toString());

                        final String[] matchedArray = songs.stream()
                                .filter(song -> song.equalsIgnoreCase(_path) || song.toLowerCase().contains(_path.toLowerCase()))
                                .toArray(String[]::new);

                        if (matchedArray.length == 0) throw new CommandException(Component.text("Song not found"));

                        final String file = matchedArray[0];

                        player.loadSong(Path.of(root.toString(), file));
                    } catch (CommandException e) {
                        throw e;
                    } catch (NoSuchFileException e) {
                        throw new CommandException(Component.text("this will never happen ok??"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (MalformedURLException e) {
            throw new CommandException(Component.text("Invalid URL"));
        } catch (IndexOutOfBoundsException e) {
            throw new CommandException(Component.text("Song not found"));
        } catch (CommandException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandException(Component.text(e.toString()));
        }

        return null;
    }

    public Component stop (CommandContext context) {
        final Bot bot = context.bot;
        bot.music.stopPlaying();
        bot.music.songQueue.clear();

        return Component.text("Cleared the song queue").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }

    public Component loop (CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final Loop loop = context.getEnum(Loop.class);

        bot.music.loop = loop;

        switch (loop) {
            case OFF -> {
                return Component.empty()
                            .append(Component.text("Looping is now "))
                            .append(Component.text("disabled").color(NamedTextColor.RED))
                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case CURRENT -> {
                return Component.empty()
                            .append(Component.text("Now looping "))
                            .append(Component.text(bot.music.currentSong.name).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary)))
                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case ALL -> {
                return Component.text("Now looping every song").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            default -> throw new CommandException(Component.text("Invalid action"));
        }
    }

    public Component list (CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String prefix = context.prefix;

        final String stringPathIfExists = context.getString(true, false);

        final Path path = (stringPathIfExists.isEmpty()) ?
                root :
                Path.of(
                        root.toString(),
                        stringPathIfExists
                );

        if (!path.normalize().startsWith(root.toString())) throw new CommandException(Component.text("no"));

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            final List<Path> paths = new ArrayList<>();
            for (Path eachPath : stream) paths.add(eachPath);

            PathUtilities.sort(paths);

            final List<Component> fullList = new ArrayList<>();
            int i = 0;
            for (Path eachPath : paths) {
                final boolean isDirectory = Files.isDirectory(eachPath);

                Path location;
                try {
                    location = path;
                } catch (IllegalArgumentException e) {
                    location = Paths.get(""); // wtf mabe
                }

                final String joinedPath = stringPathIfExists.isEmpty() ? eachPath.getFileName().toString() : Paths.get(location.getFileName().toString(), eachPath.getFileName().toString()).toString();

                fullList.add(
                        Component
                                .text(eachPath.getFileName().toString(), (i++ & 1) == 0 ? ColorUtilities.getColorByString(bot.config.colorPalette.primary) : ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
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
                List<Component> list = new ArrayList<>(fullList).subList(index, Math.min(index + eachSize, fullList.size()));

                final Component component = Component.join(JoinConfiguration.separator(Component.space()), list);
                context.sendOutput(component);

                index += eachSize;
                list.clear();
            }
        } catch (NoSuchFileException e) {
            throw new CommandException(Component.text("Directory doesn't exist"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Component skip (CommandContext context) throws CommandException {
        final Bot bot = context.bot;
        final MusicPlayerPlugin music = bot.music;
        if (music.currentSong == null) throw new CommandException(Component.text("No song is currently playing"));

        context.sendOutput(
                Component.empty()
                    .append(Component.text("Skipping "))
                    .append(Component.text(music.currentSong.name).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary)))
                    .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
        );

        music.skip();

        return null;
    }

    public Component nowplaying (CommandContext context) throws CommandException {
        final Bot bot = context.bot;
        final Song song = bot.music.currentSong;
        if (song == null) throw new CommandException(Component.text("No song is currently playing"));

        return Component.empty()
                .append(Component.text("Now playing "))
                .append(Component.text(song.name).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary)))
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }

    public Component queue (CommandContext context) {
        final Bot bot = context.bot;
        final List<Song> queue = bot.music.songQueue;

        final List<Component> queueWithNames = new ArrayList<>();
        int i = 0;
        for (Song song : queue) {
            queueWithNames.add(
                    Component.text(song.name).color((i++ & 1) == 0 ? ColorUtilities.getColorByString(bot.config.colorPalette.primary) : ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
            );
        }

        return Component.empty()
                .append(Component.text("Queue: ").color(NamedTextColor.GREEN))
                .append(Component.join(JoinConfiguration.separator(Component.text(", ")), queueWithNames));
    }

    // lazy fix for java using "goto" as keyword real
    public Component goTo (CommandContext context) throws CommandException {
        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        final String input = context.getString(true, true);

        final long timestamp = TimestampUtilities.parseTimestamp(input);

        if (currentSong == null) throw new CommandException(Component.text("No song is currently playing"));

        if (timestamp < 0 || timestamp > currentSong.length) throw new CommandException(Component.text("Invalid timestamp"));

        currentSong.setTime(timestamp);

        return Component
                .text("Set the time to ")
                .append(Component.text(input).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)))
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }

    public Component pitch (CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final float pitch = context.getFloat(true);

        bot.music.pitch = pitch;

        return Component.empty()
                .append(Component.text("Set the pitch to "))
                .append(Component.text(pitch).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)))
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }

    public Component speed (CommandContext context) throws CommandException {
        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        final float speed = context.getFloat(true);

        if (speed > 5) throw new CommandException(Component.text("Too fast!"));

        long oldTime = -1;

        if (currentSong != null) oldTime = currentSong.time;

        bot.music.speed = speed;

        if (currentSong != null) currentSong.setTime(oldTime);

        return Component.empty()
                .append(Component.text("Set the speed to "))
                .append(Component.text(speed).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)))
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }

    public Component noteInstrument (CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String instrument = context.getString(true, true);

        bot.music.instrument = instrument;

        if (!instrument.equals("off")) {
            return Component.empty()
                    .append(Component.text("Set the instrument for every note to "))
                    .append(Component.text(instrument).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary)))
                    .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
        } else {
            return Component.text("Every notes are now using its instrument").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
        }
    }

    public Component pause (CommandContext context) throws CommandException {
        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        if (currentSong == null) throw new CommandException(Component.text("No song is currently playing"));

        if (currentSong.paused) {
            currentSong.play();
            return Component.text("Resumed the current song");
        } else {
            currentSong.pause();
            return Component.text("Paused the current song");
        }
    }

    public Component info (CommandContext context) throws CommandException {
        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        if (currentSong == null) throw new CommandException(Component.text("No song is currently playing"));

        // ig very code yup
        final String title = currentSong.originalName;
        final String songAuthor = currentSong.songAuthor == null || currentSong.songAuthor.equals("") ? "N/A" : currentSong.songAuthor;
        final String songOriginalAuthor = currentSong.songOriginalAuthor == null || currentSong.songOriginalAuthor.equals("") ? "N/A" : currentSong.songOriginalAuthor;
        final String songDescription = currentSong.songDescription == null || currentSong.songDescription.equals("") ? "N/A" : currentSong.songDescription;

        return Component.translatable(
                """
                        Title/Filename: %s
                        Author: %s
                        Original author: %s
                        Description: %s""",
                Component.text(title).color(NamedTextColor.AQUA),
                Component.text(songAuthor).color(NamedTextColor.AQUA),
                Component.text(songOriginalAuthor).color(NamedTextColor.AQUA),
                Component.text(songDescription).color(NamedTextColor.AQUA)
        ).color(NamedTextColor.GOLD);
    }

    public Component testSong (CommandContext context) {
        final Bot bot = context.bot;

        final Song song = new Song(
                "test_song",
                bot,
                "Test Song",
                "chayapak",
                "hhhzzzsss",
                "SongPlayer's test song ported to ChomeNS Bot",
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
                            1,
                            i * 50,
                            -1,
                            100
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

        return Component.text("Test song has been added to the song queue").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }
}
