package me.chayapak1.chomens_bot.commands;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.command.contexts.ConsoleCommandContext;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.plugins.MusicPlayerPlugin;
import me.chayapak1.chomens_bot.song.Instrument;
import me.chayapak1.chomens_bot.song.Loop;
import me.chayapak1.chomens_bot.song.Note;
import me.chayapak1.chomens_bot.song.Song;
import me.chayapak1.chomens_bot.util.Ascii85;
import me.chayapak1.chomens_bot.util.I18nUtilities;
import me.chayapak1.chomens_bot.util.PathUtilities;
import me.chayapak1.chomens_bot.util.TimestampUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.cloudburstmc.math.vector.Vector3d;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static me.chayapak1.chomens_bot.util.StringUtilities.isNotNullAndNotBlank;

public class MusicCommand extends Command implements Listener {
    private static final Path ROOT = MusicPlayerPlugin.SONG_DIR;

    private static final AtomicInteger commandsPerSecond = new AtomicInteger();

    public MusicCommand () {
        super(
                "music",
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

        Main.EXECUTOR.scheduleAtFixedRate(() -> commandsPerSecond.set(0), 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        // denis check
        if (commandsPerSecond.get() > 3) return null;
        else commandsPerSecond.getAndIncrement();

        if (context.bot.music.locked && !(context instanceof ConsoleCommandContext))
            throw new CommandException(Component.translatable("commands.music.error.locked"));

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
            default -> throw new CommandException(Component.translatable("commands.generic.error.invalid_action"));
        };
    }

    public Component play (final CommandContext context) throws CommandException {
        final MusicPlayerPlugin player = context.bot.music;

        if (player.loaderThread != null) throw new CommandException(Component.translatable("commands.music.play.error.already_loading"));

        final String stringPath = context.getString(true, true);

        final Path path;

        try {
            path = Path.of(ROOT.toString(), stringPath);

            if (path.toString().contains("http")) player.loadSong(new URI(stringPath).toURL(), context);
            else {
                // among us protection!!!11
                if (!path.normalize().startsWith(ROOT.toString())) throw new CommandException(Component.text("no"));

                // ignore my ohio code for autocomplete
                final String separator = FileSystems.getDefault().getSeparator();

                if (stringPath.contains(separator) && !stringPath.isEmpty()) {
                    final String[] splitPath = stringPath.split(separator);

                    final List<String> splitPathClone = new ObjectArrayList<>(Arrays.stream(splitPath).toList());
                    splitPathClone.removeLast();

                    final Path realPath = Path.of(ROOT.toString(), String.join(separator, splitPathClone));

                    try (final DirectoryStream<Path> stream = Files.newDirectoryStream(realPath)) {
                        final List<Path> songsPaths = new ObjectArrayList<>();
                        for (final Path eachPath : stream) songsPaths.add(eachPath);

                        PathUtilities.sort(songsPaths);

                        final List<String> songs = new ObjectArrayList<>();
                        for (final Path eachPath : songsPaths) songs.add(eachPath.getFileName().toString());

                        final String lowerCaseFile = splitPath[splitPath.length - 1].toLowerCase();

                        final String[] matchedArray = songs.stream()
                                .filter(song -> song.equalsIgnoreCase(lowerCaseFile) || song.toLowerCase().contains(lowerCaseFile))
                                .toArray(String[]::new);

                        if (matchedArray.length == 0) throw new CommandException(Component.translatable("commands.music.error.song_not_found"));

                        final String file = matchedArray[0];

                        player.loadSong(Path.of(realPath.toString(), file), context);
                    } catch (final NoSuchFileException e) {
                        throw new CommandException(Component.translatable("commands.music.error.no_directory"));
                    }
                } else {
                    try (final DirectoryStream<Path> stream = Files.newDirectoryStream(ROOT)) {
                        final List<Path> songsPaths = new ObjectArrayList<>();
                        for (final Path eachPath : stream) songsPaths.add(eachPath);

                        PathUtilities.sort(songsPaths);

                        final List<String> songs = new ObjectArrayList<>();
                        for (final Path eachPath : songsPaths) songs.add(eachPath.getFileName().toString());

                        final String[] matchedArray = songs.stream()
                                .filter(song -> song.equalsIgnoreCase(stringPath) || song.toLowerCase().contains(stringPath.toLowerCase()))
                                .toArray(String[]::new);

                        if (matchedArray.length == 0) throw new CommandException(Component.translatable("commands.music.error.song_not_found"));

                        final String file = matchedArray[0];

                        player.loadSong(Path.of(ROOT.toString(), file), context);
                    } catch (final NoSuchFileException e) {
                        throw new CommandException(Component.text("this will never happen ok??"));
                    }
                }
            }
        } catch (final MalformedURLException e) {
            throw new CommandException(Component.translatable("commands.music.error.invalid_url"));
        } catch (final IndexOutOfBoundsException e) {
            throw new CommandException(Component.translatable("commands.music.error.song_not_found"));
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
            if (output == null) {
                context.sendOutput(Component.translatable("commands.music.playitem.error.no_item_nbt", NamedTextColor.RED));
                return null;
            }

            try {
                bot.music.loadSong(
                        Base64.getDecoder().decode(output),
                        context
                );
            } catch (final IllegalArgumentException e) {
                try {
                    bot.music.loadSong(
                            Ascii85.decode(output),
                            context
                    );
                } catch (final IllegalArgumentException e2) {
                    context.sendOutput(Component.translatable("commands.music.playitem.invalid_data", NamedTextColor.RED));
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

        return Component.translatable("commands.music.stop", bot.colorPalette.defaultColor);
    }

    public Component loop (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(2);

        final Bot bot = context.bot;

        final Loop loop = context.getEnum(true, Loop.class);

        bot.music.loop = loop;

        switch (loop) {
            case OFF -> {
                return Component.translatable(
                        "commands.music.loop.off",
                        bot.colorPalette.defaultColor,
                        Component.translatable("commands.music.loop.off.disabled", NamedTextColor.RED)
                );
            }
            case CURRENT -> {
                if (bot.music.currentSong != null) {
                    return Component.translatable(
                            "commands.music.loop.current.with_song",
                            bot.colorPalette.defaultColor,
                            Component.text(bot.music.currentSong.name, bot.colorPalette.secondary)
                    );
                } else {
                    return Component.translatable(
                            "commands.music.loop.current.without_song",
                            bot.colorPalette.defaultColor
                    );
                }
            }
            case ALL -> {
                return Component.translatable("commands.music.loop.all", bot.colorPalette.defaultColor);
            }
        }

        return null;
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
            final List<Path> paths = new ObjectArrayList<>();
            for (final Path eachPath : stream) paths.add(eachPath);

            PathUtilities.sort(paths);

            final List<Component> fullList = new ObjectArrayList<>();
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
                final List<Component> list = new ObjectArrayList<>(fullList).subList(index, Math.min(index + eachSize, fullList.size()));

                final Component component = Component.join(JoinConfiguration.separator(Component.space()), list);
                context.sendOutput(component);

                index += eachSize;
                list.clear();
            }
        } catch (final IOException e) {
            throw new CommandException(Component.translatable("commands.music.error.no_directory"));
        }

        return null;
    }

    public Component skip (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;
        final MusicPlayerPlugin music = bot.music;
        if (music.currentSong == null) throw new CommandException(Component.translatable("commands.music.error.not_playing"));

        final String name = music.currentSong.name;

        music.skip();

        return Component.translatable(
                "commands.music.skip",
                bot.colorPalette.defaultColor,
                Component.text(name, bot.colorPalette.secondary)
        );
    }

    public Component nowPlaying (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;
        final Song song = bot.music.currentSong;
        if (song == null) throw new CommandException(Component.translatable("commands.music.error.not_playing"));

        return Component.translatable(
                "commands.music.nowplaying",
                bot.colorPalette.defaultColor,
                Component.text(song.name, bot.colorPalette.secondary)
        );
    }

    public Component queue (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;
        final List<Song> queue = bot.music.songQueue;

        final List<Component> queueWithNames = new ObjectArrayList<>();
        int i = 0;
        for (final Song song : queue) {
            queueWithNames.add(
                    Component.text(
                            song.name,
                            (i++ & 1) == 0
                                    ? bot.colorPalette.primary
                                    : bot.colorPalette.secondary
                    )
            );
        }

        return Component.translatable(
                "commands.music.queue",
                NamedTextColor.GREEN,
                Component.join(JoinConfiguration.commas(true), queueWithNames)
        );
    }

    // lazy fix for java using "goto" as keyword real
    public Component goTo (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        final String input = context.getString(true, true);

        final long timestamp = TimestampUtilities.parseTimestamp(input);

        if (currentSong == null) throw new CommandException(Component.translatable("commands.music.error.not_playing"));

        if (timestamp < 0 || timestamp > currentSong.length / bot.music.speed)
            throw new CommandException(Component.translatable("commands.music.goto.error.invalid_timestamp"));

        currentSong.setTime(timestamp / bot.music.speed);

        return Component.translatable(
                "commands.music.goto",
                bot.colorPalette.defaultColor,
                Component.text(input, bot.colorPalette.number)
        );
    }

    public Component pitch (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(2);

        final Bot bot = context.bot;

        final float pitch = context.getFloat(true, false);

        bot.music.pitch = pitch;

        return Component.translatable(
                "commands.music.pitch",
                bot.colorPalette.defaultColor,
                Component.text(pitch, bot.colorPalette.number)
        );
    }

    public Component speed (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(2);

        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        final double speed = context.getDouble(true, false);

        if (speed > 5) throw new CommandException(Component.translatable("commands.music.speed.error.too_fast"));
        else if (speed < 0) throw new CommandException(Component.translatable("commands.music.speed.error.negative"));

        double oldTime = -1;

        if (currentSong != null) oldTime = currentSong.time / speed;

        bot.music.speed = speed;

        if (currentSong != null) currentSong.setTime(oldTime);

        return Component.translatable(
                "commands.music.speed",
                bot.colorPalette.defaultColor,
                Component.text(speed, bot.colorPalette.number)
        );
    }

    public Component volume (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(2);

        final Bot bot = context.bot;

        final float volume = context.getFloat(true, false);

        bot.music.volume = volume;

        return Component.translatable(
                "commands.music.volume",
                bot.colorPalette.defaultColor,
                Component.text(volume, bot.colorPalette.number)
        );
    }

    public Component amplify (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(2);

        final Bot bot = context.bot;

        final int amplify = context.getInteger(true);

        if (amplify > 8) throw new CommandException(Component.translatable("commands.music.amplify.error.too_big_value"));
        else if (amplify < 0) throw new CommandException(Component.translatable("commands.music.amplify.error.negative"));

        bot.music.amplify = amplify;

        return Component.translatable(
                "commands.music.amplify",
                bot.colorPalette.defaultColor,
                Component.text(amplify, bot.colorPalette.number)
        );
    }

    public Component noteInstrument (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String instrument = context.getString(true, true);

        bot.music.instrument = instrument;

        if (instrument.equalsIgnoreCase("off")) {
            return Component.translatable("commands.music.noteinstrument.off", bot.colorPalette.defaultColor);
        } else {
            return Component.translatable(
                    "commands.music.noteinstrument.set",
                    bot.colorPalette.defaultColor,
                    Component.text(instrument)
            );
        }
    }

    public Component pause (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        if (currentSong == null) throw new CommandException(Component.translatable("commands.music.error.not_playing"));

        if (currentSong.paused) {
            currentSong.play();
            return Component.translatable("commands.music.resumed", bot.colorPalette.defaultColor);
        } else {
            currentSong.pause();
            return Component.translatable("commands.music.paused", bot.colorPalette.defaultColor);
        }
    }

    public Component info (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        if (currentSong == null) throw new CommandException(Component.translatable("commands.music.error.not_playing"));

        final List<Component> components = new ObjectArrayList<>();

        final TextColor keyColor = bot.colorPalette.secondary;
        final TextColor valueColor = bot.colorPalette.string;

        final DecimalFormat formatter = new DecimalFormat("#,###");

        final String formattedNotesCount = formatter.format(currentSong.size());

        if (isNotNullAndNotBlank(currentSong.name))
            components.add(Component.translatable("commands.music.info.title", keyColor, Component.text(currentSong.name, valueColor)));
        if (currentSong.context != null && isNotNullAndNotBlank(currentSong.context.sender.profile.getName()))
            components.add(Component.translatable("commands.music.info.requester", keyColor, Component.text(currentSong.context.sender.profile.getName(), valueColor)));
        if (isNotNullAndNotBlank(currentSong.songAuthor))
            components.add(Component.translatable("commands.music.info.author", keyColor, Component.text(currentSong.songAuthor, valueColor)));
        if (isNotNullAndNotBlank(currentSong.songOriginalAuthor))
            components.add(Component.translatable("commands.music.info.original_author", keyColor, Component.text(currentSong.songOriginalAuthor, valueColor)));
        if (isNotNullAndNotBlank(currentSong.tracks))
            components.add(Component.translatable("commands.music.info.tracks", keyColor, Component.text(currentSong.tracks, valueColor)));
        components.add(Component.translatable("commands.music.info.notes", keyColor, Component.text(formattedNotesCount, valueColor)));
        if (isNotNullAndNotBlank(currentSong.songDescription))
            components.add(Component.translatable("commands.music.info.description", keyColor, Component.text(currentSong.songDescription, valueColor)));

        return Component.join(JoinConfiguration.newlines(), components);
    }

    public Component testSong (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;

        final Song song = new Song(
                "test_song",
                bot,
                I18nUtilities.get("commands.music.testsong.title"),
                "chayapak",
                "hhhzzzsss",
                I18nUtilities.get("commands.music.testsong.description"),
                null,
                false
        );

        song.context = context;

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
                            Vector3d.ZERO,
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

        return Component.translatable("commands.music.testsong.output", bot.colorPalette.defaultColor);
    }
}
