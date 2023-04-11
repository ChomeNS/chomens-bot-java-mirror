package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.plugins.MusicPlayerPlugin;
import land.chipmunk.chayapak.chomens_bot.song.Loop;
import land.chipmunk.chayapak.chomens_bot.song.Song;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MusicCommand implements Command {
    private Path root;

    public String name() { return "music"; }

    public String description() {
        return "Plays music";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("play <{song|URL}>");
        usages.add("stop");
        usages.add("loop <current|all|off>");
        usages.add("list [{directory}]");
        usages.add("skip");
        usages.add("nowplaying");
        usages.add("queue");
        usages.add("goto <timestamp>");
        usages.add("pitch <pitch>");
        usages.add("pause");
        usages.add("resume");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("song");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        root = Path.of(context.bot().music().SONG_DIR.getPath());
        switch (args[0]) {
            case "play" -> {
                return play(context, args);
            }
            case "stop" -> stop(context);
            case "loop" -> {
                return loop(context, args);
            }
            case "list" -> {
                return list(context, args);
            }
            case "skip" -> {
                return skip(context);
            }
            case "nowplaying" -> {
                return nowplaying(context);
            }
            case "queue" -> queue(context);
            case "goto" -> {
                return goTo(context, args);
            }
            case "pitch" -> {
                return pitch(context, args);
            }
            case "pause", "resume" -> {
                return pause(context);
            }
            default -> {
                return Component.text("Invalid argument").color(NamedTextColor.RED);
            }
        }

        return Component.text("success");
    }

    public Component play (CommandContext context, String[] args) {
        final MusicPlayerPlugin player = context.bot().music();

        String _path;
        Path path;
        try {
            _path = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            path = Path.of(root.toString(), _path);

            if (path.toString().contains("http")) player.loadSong(new URL(_path));
            else {
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

        return Component.text("success");
    }

    public void stop (CommandContext context) {
        final Bot bot = context.bot();
        bot.music().stopPlaying();
        bot.music().songQueue().clear();

        context.sendOutput(Component.text("Cleared the song queue"));
    }

    public Component loop (CommandContext context, String[] args) {
        final Bot bot = context.bot();

        if (args[1] == null) return Component.text("Invalid argument").color(NamedTextColor.RED);

        Loop loop;
        switch (args[1]) {
            case "off" -> {
                loop = Loop.OFF;
                context.sendOutput(
                        Component.empty()
                                .append(Component.text("Looping is now "))
                                .append(Component.text("disabled").color(NamedTextColor.RED))
                );
            }
            case "current" -> {
                loop = Loop.CURRENT;
                context.sendOutput(
                        Component.empty()
                                .append(Component.text("Now looping "))
                                .append(bot.music().currentSong().name.color(NamedTextColor.GOLD))
                );
            }
            case "all" -> {
                loop = Loop.ALL;
                context.sendOutput(Component.text("Now looping every song"));
            }
            default -> {
                return Component.text("Invalid argument").color(NamedTextColor.RED);
            }
        }

        bot.music().loop(loop);

        return Component.text("success");
    }

    public Component list (CommandContext context, String[] args) {
        final String prefix = context.prefix();

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
                            .text(filename, (i++ & 1) == 0 ? NamedTextColor.YELLOW : NamedTextColor.GOLD)
                            .clickEvent(
                                    ClickEvent.suggestCommand(
                                            prefix +
                                                    name() +
                                                    (file.isFile() ? " play " : " list ") +
                                                    joinedPath.replace("'", "\\'")
                                    )
                            )
            );
        }

        final int eachSize = 100;

        int index = 0;

        while (index < fullList.size()) {
            List<Component> list = fullList.subList(index, Math.min(index + eachSize, fullList.size()));

            final Component component = Component.join(JoinConfiguration.separator(Component.space()), list);
            context.sendOutput(component);

            index += eachSize;
            list.clear();
        }

        return Component.text("success");
    }

    public Component skip (CommandContext context) {
        final MusicPlayerPlugin music = context.bot().music();
        if (music.currentSong() == null) return Component.text("No song is currently playing").color(NamedTextColor.RED);

        context.sendOutput(
                Component.empty()
                        .append(Component.text("Skipping "))
                        .append(music.currentSong().name.color(NamedTextColor.GOLD))
        );

        music.skip();

        return Component.text("success");
    }

    public Component nowplaying (CommandContext context) {
        final Bot bot = context.bot();
        final Song song = bot.music().currentSong();
        if (song == null) return Component.text("No song is currently playing").color(NamedTextColor.RED);
        context.sendOutput(
                Component.empty()
                        .append(Component.text("Now playing "))
                        .append(song.name.color(NamedTextColor.GOLD))
        );

        return Component.text("success");
    }

    public void queue (CommandContext context) {
        final Bot bot = context.bot();
        final LinkedList<Song> queue = bot.music().songQueue();

        final List<Component> queueWithNames = new ArrayList<>();
        for (Song song : queue) queueWithNames.add(song.name);

        context.sendOutput(
                Component.empty()
                        .append(Component.text("Queue: ").color(NamedTextColor.GREEN))
                        .append(Component.join(JoinConfiguration.separator(Component.text(", ")), queueWithNames).color(NamedTextColor.AQUA))
        );
    }

    // lazy fix for java using "goto" as keyword real
    public Component goTo (CommandContext context, String[] args) {
        final Bot bot = context.bot();
        final Song currentSong = bot.music().currentSong();

        final long milliseconds;
        try {
            milliseconds = Long.parseLong(args[1]) * 1000;
        } catch (NumberFormatException e) {
            return Component.text("Invalid timestamp").color(NamedTextColor.RED);
        }

        if (currentSong == null) return Component.text("No song is currently playing").color(NamedTextColor.RED);
        if (milliseconds < 0 || milliseconds > currentSong.length) return Component.text("Invalid timestamp").color(NamedTextColor.RED);

        currentSong.setTime(milliseconds);

        return Component.text("success");
    }

    public Component pitch (CommandContext context, String[] args) {
        final Bot bot = context.bot();

        float pitch;
        try {
            pitch = Float.parseFloat(args[1]);
        } catch (IllegalArgumentException ignored) {
            return Component.text("Invalid pitch").color(NamedTextColor.RED);
        }

        bot.music().pitch(pitch);

        context.sendOutput(
                Component.empty()
                        .append(Component.text("Set the pitch to "))
                        .append(Component.text(pitch).color(NamedTextColor.GOLD))
        );

        return Component.text("success");
    }

    public Component pause (CommandContext context) {
        final Bot bot = context.bot();
        final Song currentSong = bot.music().currentSong();

        if (currentSong == null) return Component.text("No song is currently playing").color(NamedTextColor.RED);

        if (currentSong.paused) {
            currentSong.play();
            context.sendOutput(Component.text("Resumed the current song"));
        } else {
            currentSong.pause();
            context.sendOutput(Component.text("Paused the current song"));
        }

        return Component.text("success");
    }
}
