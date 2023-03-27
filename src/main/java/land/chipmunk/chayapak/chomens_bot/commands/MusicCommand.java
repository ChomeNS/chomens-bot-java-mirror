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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
        usages.add("goto");
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

        final String _path = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        final Path path = Path.of(root.toString(), _path);

        try {
            if (!path.toString().contains("http")) player.loadSong(path);
            else player.loadSong(new URL(_path));
        } catch (MalformedURLException e) {
            return Component.text("Invalid URL").color(NamedTextColor.RED);
        }

        return Component.text("success");
    }

    public void stop (CommandContext context) {
        final Bot bot = context.bot();
        bot.music().stopPlaying();
        bot.music().songQueue().clear();
    }

    public Component loop (CommandContext context, String[] args) {
        final Bot bot = context.bot();

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

        final Path _path = Path.of(root.toString(), String.join(" ", args));
        final Path path = (args.length < 2) ? root : _path;

        final String[] filenames = path.toFile().list();
        if (filenames == null) return Component.text("Directory doesn't exist").color(NamedTextColor.RED);

        final List<Component> list = new ArrayList<>();
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
            list.add(
                    Component
                            .text(filename, (i++ & 1) == 0 ? NamedTextColor.YELLOW : NamedTextColor.GOLD)
                            .clickEvent(
                                    ClickEvent.suggestCommand(
                                            prefix +
                                                    "music" + // ? How do I make this dynamic?
                                                    (file.isFile() ? " play " : " list ") +
                                                    joinedPath.replace("'", "\\'")
                                    )
                            )
            );
        }

        final Component component = Component.join(JoinConfiguration.separator(Component.space()), list);
        context.sendOutput(component);

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
            milliseconds = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            return Component.text("Invalid timestamp").color(NamedTextColor.RED);
        }

        if (currentSong == null) return Component.text("No song is currently playing").color(NamedTextColor.RED);
        if (milliseconds < 0 || milliseconds > currentSong.length) return Component.text("Invalid timestamp").color(NamedTextColor.RED);

        currentSong.setTime(milliseconds);

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
