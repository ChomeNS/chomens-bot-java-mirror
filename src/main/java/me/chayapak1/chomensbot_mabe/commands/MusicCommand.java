package me.chayapak1.chomensbot_mabe.commands;

import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.command.Command;
import me.chayapak1.chomensbot_mabe.command.CommandContext;
import me.chayapak1.chomensbot_mabe.plugins.MusicPlayerPlugin;
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
        }

        return Component.text("success");
    }

    public Component play (CommandContext context, String[] args) {
        final Bot bot = context.bot();
        final MusicPlayerPlugin player = context.bot().music();

        final String _path = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        final Path path = Path.of(root.toString(), _path);

        bot.logger().log(path.toString());

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

        int loop;
        switch (args[1]) {
            case "off" -> loop = 0;
            case "current" -> loop = 1;
            case "all" -> loop = 2;
            default -> {
                return Component.text("Invalid argument");
            }
        }

        bot.music().currentSong().looping = loop;

        return Component.text("success");
    }

    public Component list (CommandContext context, String[] args) {
        final String prefix = context.bot().chatCommandHandler().prefix();

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

        music.stopPlaying();

        return Component.text("success");
    }
}
