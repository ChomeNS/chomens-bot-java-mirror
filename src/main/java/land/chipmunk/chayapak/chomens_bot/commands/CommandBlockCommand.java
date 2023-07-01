package land.chipmunk.chayapak.chomens_bot.commands;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandBlockCommand extends Command {
    public CommandBlockCommand () {
        super(
                "cb",
                "Executes a command in the command core and return its output",
                new String[] { "<command>" },
                new String[] { "cmd", "commandblock", "run" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        final CompletableFuture<CompoundTag> future = bot.core().runTracked(String.join(" ", args));

        if (future == null) return null;

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
