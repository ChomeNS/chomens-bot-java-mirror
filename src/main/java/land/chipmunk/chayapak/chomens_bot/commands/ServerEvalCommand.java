package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.ArrayList;
import java.util.List;

public class ServerEvalCommand implements Command {
    public String name() { return "servereval"; }

    public String description() {
        return "Evaluate codes using LuaJ";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{code}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

        return aliases;
    }

    public int trustLevel() {
        return 2;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        try {
            final Bot bot = context.bot();

            bot.eval().globals().set("context", CoerceJavaToLua.coerce(context));

            final LuaValue output = context.bot().eval().run(String.join(" ", args));

            context.sendOutput(Component.text(output.toString()).color(NamedTextColor.GREEN));
        } catch (Exception e) {
            return Component.text(e.toString()).color(NamedTextColor.RED);
        }

        return Component.text("success");
    }
}
