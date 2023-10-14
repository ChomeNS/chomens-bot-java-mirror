package land.chipmunk.chayapak.chomens_bot.commands;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
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
import land.chipmunk.chayapak.chomens_bot.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
                        "amplify <amplification>",
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

        final String action = context.getString(false, true, true);

        root = MusicPlayerPlugin.SONG_DIR;
        return switch (action) {
            case "play", "playurl", "playnbs", "playnbsurl" -> play(context);
            case "playfromitem", "playitem" -> playFromItem(context);
            case "playsongplayer" -> playSongPlayer(context);
            case "stop" -> stop(context);
            case "loop" -> loop(context);
            case "list" -> list(context);
            case "skip" -> skip(context);
            case "nowplaying" -> nowplaying(context);
            case "queue" -> queue(context);
            case "goto" -> goTo(context);
            case "pitch" -> pitch(context);
            case "speed" -> speed(context);
            case "amplify" -> amplify(context);
            case "noteinstrument" -> noteInstrument(context);
            case "pause", "resume" -> pause(context);
            case "info" -> info(context);
            case "testsong" -> testSong(context);
            default -> Component.text("Invalid action").color(NamedTextColor.RED);
        };
    }

    public Component play (CommandContext context) throws CommandException {
        final MusicPlayerPlugin player = context.bot.music;

        if (player.loaderThread != null) throw new CommandException(Component.text("Already loading a song"));

        String _path;
        Path path;
        try {
            _path = context.getString(true, true);

//            if (_path.isBlank()) throw new CommandException(Component.text("No song specified"));

            path = Path.of(root.toString(), _path);

            if (path.toString().contains("http")) player.loadSong(new URI(_path).toURL(), context.sender);
            else {
                // among us protection!!!11
                if (!path.normalize().startsWith(root.toString())) throw new CommandException(Component.text("no"));

                // ignore my ohio code for autocomplete
                final String separator = FileSystems.getDefault().getSeparator();

                if (_path.contains(separator) && !_path.isEmpty()) {
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

                        player.loadSong(Path.of(realPath.toString(), file), context.sender);
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

                        player.loadSong(Path.of(root.toString(), file), context.sender);
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

    public Component playFromItem (CommandContext context) throws CommandException {
        // mail command lol

        final Bot bot = context.bot;

        final CompletableFuture<CompoundTag> future = bot.core.runTracked(
                "minecraft:data get entity " +
                        UUIDUtilities.selector(context.sender.profile.getId()) +
                        " SelectedItem.tag.data"
        );

        if (future == null) {
            throw new CommandException(Component.text("There was an error while getting your data"));
        }

        future.thenApply(tags -> {
            if (!tags.contains("LastOutput") || !(tags.get("LastOutput") instanceof StringTag)) return tags;

            final StringTag lastOutput = tags.get("LastOutput");

            final Component output = GsonComponentSerializer.gson().deserialize(lastOutput.getValue());

            final List<Component> children = output.children();

            if (
                    !children.isEmpty() &&
                            !children.get(0).children().isEmpty() &&
                            ((TranslatableComponent) children.get(0).children().get(0))
                                    .key()
                                    .equals("arguments.nbtpath.nothing_found")
            ) {
                context.sendOutput(Component.text("Player has no `data` NBT tag in the selected item").color(NamedTextColor.RED));
                return tags;
            }

            final String value = ComponentUtilities.stringify(((TranslatableComponent) children.get(0)).args().get(1));

            if (!value.startsWith("\"") && !value.endsWith("\"") && !value.startsWith("'") && !value.endsWith("'")) {
                context.sendOutput(Component.text("`data` NBT is not a string").color(NamedTextColor.RED));
                return tags;
            }

            try {
                bot.music.loadSong(
                        Base64.getDecoder().decode(
                                value
                                        .substring(1)
                                        .substring(0, value.length() - 2)
                        ),
                        context.sender
                );
            } catch (IllegalArgumentException e) {
                context.sendOutput(Component.text("Invalid base64 in the selected item").color(NamedTextColor.RED));
            }

            return tags;
        });

        return null;
    }

    public Component playSongPlayer (CommandContext context) throws CommandException {
        // dupe codes ??

        final Bot bot = context.bot;

        final CompletableFuture<CompoundTag> future = bot.core.runTracked(
                "minecraft:data get entity " +
                        UUIDUtilities.selector(context.sender.profile.getId()) +
                        " SelectedItem.tag.SongItemData.SongData"
        );

        if (future == null) {
            throw new CommandException(Component.text("There was an error while getting your data"));
        }

        future.thenApply(tags -> {
            if (!tags.contains("LastOutput") || !(tags.get("LastOutput") instanceof StringTag)) return tags;

            final StringTag lastOutput = tags.get("LastOutput");

            final Component output = GsonComponentSerializer.gson().deserialize(lastOutput.getValue());

            final List<Component> children = output.children();

            if (
                    !children.isEmpty() &&
                            !children.get(0).children().isEmpty() &&
                            ((TranslatableComponent) children.get(0).children().get(0))
                                    .key()
                                    .equals("arguments.nbtpath.nothing_found")
            ) {
                context.sendOutput(Component.text("Player has no SongItemData -> SongData NBT tag in the selected item").color(NamedTextColor.RED));
                return tags;
            }

            final String value = ComponentUtilities.stringify(((TranslatableComponent) children.get(0)).args().get(1));

            if (!value.startsWith("\"") && !value.endsWith("\"") && !value.startsWith("'") && !value.endsWith("'")) {
                context.sendOutput(Component.text("NBT is not a string").color(NamedTextColor.RED));
                return tags;
            }

            try {
                bot.music.loadSong(
                        Base64.getDecoder().decode(
                                value
                                        .substring(1)
                                        .substring(0, value.length() - 2)
                        ),
                        context.sender
                );
            } catch (IllegalArgumentException e) {
                context.sendOutput(Component.text("Invalid song data in the selected item").color(NamedTextColor.RED));
            }

            return tags;
        });

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

    public Component amplify(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final int amplify = context.getInteger(true);

        if (amplify > 8) throw new CommandException(Component.text("Too big value"));

        bot.music.amplify = amplify;

        return Component.empty()
                .append(Component.text("Set the amplification to "))
                .append(Component.text(amplify).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)))
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
            return Component.text("Resumed the current song").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
        } else {
            currentSong.pause();
            return Component.text("Paused the current song").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
        }
    }

    public Component info (CommandContext context) throws CommandException {
        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        if (currentSong == null) throw new CommandException(Component.text("No song is currently playing"));

        final List<Component> components = new ArrayList<>();

        final TextColor keyColor = ColorUtilities.getColorByString(bot.config.colorPalette.secondary);
        final TextColor valueColor = ColorUtilities.getColorByString(bot.config.colorPalette.string);

        if (currentSong.name != null) components.add(Component.translatable("Title/Filename: %s", Component.text(currentSong.name).color(valueColor)).color(keyColor));
        if (currentSong.requester != null) components.add(Component.translatable("Requester: %s", Component.text(currentSong.requester).color(valueColor)).color(keyColor));
        if (currentSong.songAuthor != null && !currentSong.songAuthor.isBlank()) components.add(Component.translatable("Author: %s", Component.text(currentSong.songAuthor).color(valueColor)).color(keyColor));
        if (currentSong.songOriginalAuthor != null && !currentSong.songOriginalAuthor.isBlank()) components.add(Component.translatable("Original author: %s", Component.text(currentSong.songOriginalAuthor).color(valueColor)).color(keyColor));
        if (currentSong.songDescription != null && !currentSong.songDescription.isBlank()) components.add(Component.translatable("Description: %s", Component.text(currentSong.songDescription).color(valueColor)).color(keyColor));

        return Component.join(JoinConfiguration.newlines(), components);
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
