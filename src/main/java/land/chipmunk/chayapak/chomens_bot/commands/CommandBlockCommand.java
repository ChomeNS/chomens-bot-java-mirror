package land.chipmunk.chayapak.chomens_bot.commands;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CommandBlockCommand implements Command {
    public String name() { return "cb"; }

    public String description() {
        return "Executes a command in the command core and return it's output";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{command}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("cmd");
        aliases.add("commandblock");
        aliases.add("run");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) throws ExecutionException, InterruptedException {
        final Bot bot = context.bot();

        final CompletableFuture<CompoundTag> future = bot.core().runTracked(String.join(" ", args));

        future.thenApply(tags -> {
            if (!tags.contains("LastOutput") || !(tags.get("LastOutput") instanceof StringTag)) return tags;

            final StringTag lastOutput = tags.get("LastOutput");

            final Component output = GsonComponentSerializer.gson().deserialize(lastOutput.getValue());

            final List<Component> children = output.children();

            context.sendOutput(Component.join(JoinConfiguration.separator(Component.space()), children));

            return tags;
        });

        return null;
    }
}
