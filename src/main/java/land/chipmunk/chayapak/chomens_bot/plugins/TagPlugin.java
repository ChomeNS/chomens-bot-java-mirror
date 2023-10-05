package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

// TODO: self care
public class TagPlugin extends CorePlugin.Listener {
    private final Bot bot;

    public final String tag = "chomens_bot";

    public TagPlugin (Bot bot) {
        this.bot = bot;

        // tracked core is meant to be a standby core so ill use 30 seconds for this
        bot.executor.scheduleAtFixedRate(this::checkAndAdd, 5, 30, TimeUnit.SECONDS);
    }

    private void checkAndAdd () {
        final String botSelector = UUIDUtilities.selector(bot.profile.getId());

        final CompletableFuture<CompoundTag> future = bot.core.runTracked("minecraft:data get entity " + botSelector + " Tags");

        if (future == null) return;

        future.thenApply((tags) -> {
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
                runCommand();
                return tags;
            }

            final String value = ComponentUtilities.stringify(((TranslatableComponent) children.get(0)).args().get(1));

            if (!value.contains("\"" + tag + "\"")) runCommand();

            return tags;
        });
    }

    private void runCommand () {
        final String botSelector = UUIDUtilities.selector(bot.profile.getId());

        bot.core.run("minecraft:tag " + botSelector + " add " + tag);
    }
}
