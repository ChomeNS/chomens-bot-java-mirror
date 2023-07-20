package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.plugins.MusicPlayerPlugin;
import land.chipmunk.chayapak.chomens_bot.song.Loop;
import land.chipmunk.chayapak.chomens_bot.song.Song;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MusicCommand extends Command {
    private Path root;

    public MusicCommand () {
        super(
                "music",
                "Play musics",
                new String[] {
                        "play <{song|URL}>",
                        "stop",
                        "loop <current|all|off>",
                        "list [{directory}]",
                        "skip",
                        "nowplaying",
                        "queue",
                        "goto <timestamp>",
                        "pitch <pitch>",
                        "speed <speed>",
                        "pause",
                        "resume",
                        "info"
                },
                new String[] { "song" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        if (args.length < 1) return Component.text("Not enough arguments").color(NamedTextColor.RED);

        root = Path.of(MusicPlayerPlugin.SONG_DIR.getPath());
        return switch (args[0]) {
            case "play", "playurl", "playnbs", "playnbsurl" -> play(context, args);
            case "stop" -> stop(context);
            case "loop" -> loop(context, args);
            case "list" -> list(context, args);
            case "skip" -> skip(context);
            case "nowplaying" -> nowplaying(context);
            case "queue" -> queue(context);
            case "goto" -> goTo(context, args);
            case "pitch" -> pitch(context, args);
            case "speed" -> speed(context, args);
            case "pause", "resume" -> pause(context);
            case "info" -> info(context);
            default -> Component.text("Invalid action").color(NamedTextColor.RED);
        };
    }

    public Component play (CommandContext context, String[] args) {
        final MusicPlayerPlugin player = context.bot.music;

        String _path;
        Path path;
        try {
            _path = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            path = Path.of(root.toString(), _path);

            if (path.toString().contains("http")) player.loadSong(new URL(_path));
            else {
                // among us protection!!!11
                if (!path.normalize().startsWith(root.toString())) return Component.text("no").color(NamedTextColor.RED);

                // ignore my ohio code for autocomplete
                final String separator = File.separator;

                if (_path.contains(separator) && !_path.equals("")) {
                    final String[] pathSplitted = _path.split(separator);

                    final List<String> pathSplittedClone = new ArrayList<>(Arrays.stream(pathSplitted.clone()).toList());
                    pathSplittedClone.remove(pathSplittedClone.size() - 1);

                    final Path realPath = Path.of(root.toString(), String.join(separator, pathSplittedClone));

                    final String[] songs = realPath.toFile().list();

                    if (songs == null) return Component.text("Directory does not exist").color(NamedTextColor.RED);

                    final String lowerCaseFile = pathSplitted[pathSplitted.length - 1].toLowerCase();

                    final String file = Arrays.stream(songs)
                            .filter(song -> song.toLowerCase().contains(lowerCaseFile))
                            .toArray(String[]::new)[0];

                    player.loadSong(Path.of(realPath.toString(), file));
                } else {
                    final String[] songs = root.toFile().list();

                    if (songs == null) return null;

                    final String file = Arrays.stream(songs)
                            .filter(song -> song.toLowerCase().contains(_path.toLowerCase()))
                            .toArray(String[]::new)[0];

                    player.loadSong(Path.of(root.toString(), file));
                }
            }
        } catch (MalformedURLException e) {
            return Component.text("Invalid URL").color(NamedTextColor.RED);
        } catch (ArrayIndexOutOfBoundsException e) {
            return Component.text("Song not found").color(NamedTextColor.RED);
        } catch (Exception e) {
            return Component.text(e.toString()).color(NamedTextColor.RED);
        }

        return null;
    }

    public Component stop (CommandContext context) {
        final Bot bot = context.bot;
        bot.music.stopPlaying();
        bot.music.songQueue.clear();

        return Component.text("Cleared the song queue").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }

    public Component loop (CommandContext context, String[] args) {
        final Bot bot = context.bot;

        if (args.length < 2) return Component.text("Invalid loop").color(NamedTextColor.RED);

        Loop loop;
        switch (args[1]) {
            case "off" -> {
                loop = Loop.OFF;
                context.sendOutput(
                        Component.empty()
                                .append(Component.text("Looping is now "))
                                .append(Component.text("disabled").color(NamedTextColor.RED))
                                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
                );
            }
            case "current" -> {
                loop = Loop.CURRENT;
                context.sendOutput(
                        Component.empty()
                                .append(Component.text("Now looping "))
                                .append(bot.music.currentSong.name.color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary)))
                                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
                );
            }
            case "all" -> {
                loop = Loop.ALL;
                context.sendOutput(Component.text("Now looping every song").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)));
            }
            default -> {
                return Component.text("Invalid action").color(NamedTextColor.RED);
            }
        }

        bot.music.loop = loop;

        return null;
    }

    public Component list (CommandContext context, String[] args) {
        final Bot bot = context.bot;

        final String prefix = context.prefix;

        final Path _path = Path.of(root.toString(), String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        final Path path = (args.length < 2) ? root : _path;

        if (!path.normalize().startsWith(root.toString())) return Component.text("no").color(NamedTextColor.RED);

        final String[] filenames = path.toFile().list();
        if (filenames == null) return Component.text("Directory doesn't exist").color(NamedTextColor.RED);

        Arrays.sort(filenames, (s1, s2) -> {
            int result = s1.compareToIgnoreCase(s2);
            if (result == 0) {
                return s2.compareTo(s1);
            }
            return result;
        });

        final List<Component> fullList = new ArrayList<>();
        int i = 0;
        for (String filename : filenames) {
            final String pathString = path.toString();
            final File file = new File(Paths.get(pathString, filename).toUri());

            Path location;
            try {
                location = path;
            } catch (IllegalArgumentException e) {
                location = Paths.get(""); // wtf mabe
            }
            final String joinedPath = (args.length < 2) ? filename : Paths.get(location.getFileName().toString(), filename).toString();
            fullList.add(
                    Component
                            .text(filename, (i++ & 1) == 0 ? ColorUtilities.getColorByString(bot.config.colorPalette.primary) : ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                            .clickEvent(
                                    ClickEvent.suggestCommand(
                                            prefix +
                                                    name +
                                                    (file.isFile() ? " play " : " list ") +
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

        return null;
    }

    public Component skip (CommandContext context) {
        final Bot bot = context.bot;
        final MusicPlayerPlugin music = bot.music;
        if (music.currentSong == null) return Component.text("No song is currently playing").color(NamedTextColor.RED);

        context.sendOutput(
                Component.empty()
                    .append(Component.text("Skipping "))
                    .append(music.currentSong.name.color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary)))
                    .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
        );

        music.skip();

        return null;
    }

    public Component nowplaying (CommandContext context) {
        final Bot bot = context.bot;
        final Song song = bot.music.currentSong;
        if (song == null) return Component.text("No song is currently playing").color(NamedTextColor.RED);

        return Component.empty()
                .append(Component.text("Now playing "))
                .append(song.name.color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary)))
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }

    public Component queue (CommandContext context) {
        final Bot bot = context.bot;
        final List<Song> queue = bot.music.songQueue;

        final List<Component> queueWithNames = new ArrayList<>();
        int i = 0;
        for (Song song : queue) {
            queueWithNames.add(
                    song.name.color((i++ & 1) == 0 ? ColorUtilities.getColorByString(bot.config.colorPalette.primary) : ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
            );
        }

        return Component.empty()
                .append(Component.text("Queue: ").color(NamedTextColor.GREEN))
                .append(Component.join(JoinConfiguration.separator(Component.text(", ")), queueWithNames));
    }

    // lazy fix for java using "goto" as keyword real
    public Component goTo (CommandContext context, String[] args) {
        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        final long milliseconds;
        try {
            milliseconds = Long.parseLong(args[1]) * 1000;
        } catch (NumberFormatException e) {
            return Component.text("Invalid timestamp").color(NamedTextColor.RED);
        }

        if (currentSong == null) return Component.text("No song is currently playing").color(NamedTextColor.RED);
        if (milliseconds < 0 || milliseconds > currentSong.length) return Component.text("Invalid timestamp").color(NamedTextColor.RED);

        currentSong.setTime(milliseconds);

        return null;
    }

    public Component pitch (CommandContext context, String[] args) {
        final Bot bot = context.bot;

        float pitch;
        try {
            pitch = Float.parseFloat(args[1]);
        } catch (IllegalArgumentException ignored) {
            return Component.text("Invalid pitch").color(NamedTextColor.RED);
        }

        bot.music.pitch = pitch;

        return Component.empty()
                .append(Component.text("Set the pitch to "))
                .append(Component.text(pitch).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)))
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }

    public Component speed (CommandContext context, String[] args) {
        final Bot bot = context.bot;

        float speed;
        try {
            speed = Float.parseFloat(args[1]);
        } catch (IllegalArgumentException ignored) {
            return Component.text("Invalid speed").color(NamedTextColor.RED);
        }

        bot.music.speed = speed;

        return Component.empty()
                .append(Component.text("Set the speed to "))
                .append(Component.text(speed).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)))
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }

    public Component pause (CommandContext context) {
        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        if (currentSong == null) return Component.text("No song is currently playing").color(NamedTextColor.RED);

        if (currentSong.paused) {
            currentSong.play();
            return Component.text("Resumed the current song");
        } else {
            currentSong.pause();
            return Component.text("Paused the current song");
        }
    }

    public Component info (CommandContext context) {
        final Bot bot = context.bot;
        final Song currentSong = bot.music.currentSong;

        if (currentSong == null) return Component.text("No song is currently playing").color(NamedTextColor.RED);

        // ig very code yup
        final Component title = currentSong.name;
        final String songAuthor = currentSong.songAuthor == null || currentSong.songAuthor.equals("") ? "N/A" : currentSong.songAuthor;
        final String songOriginalAuthor = currentSong.songOriginalAuthor == null || currentSong.songOriginalAuthor.equals("") ? "N/A" : currentSong.songOriginalAuthor;
        final String songDescription = currentSong.songDescription == null || currentSong.songDescription.equals("") ? "N/A" : currentSong.songDescription;

        return Component.translatable(
                """
                        Title/Filename: %s
                        Author: %s
                        Original author: %s
                        Description: %s""",
                title.color(NamedTextColor.AQUA),
                Component.text(songAuthor).color(NamedTextColor.AQUA),
                Component.text(songOriginalAuthor).color(NamedTextColor.AQUA),
                Component.text(songDescription).color(NamedTextColor.AQUA)
        ).color(NamedTextColor.GOLD);
    }
}
