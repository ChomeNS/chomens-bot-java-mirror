package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class ServerEvalCommand extends Command {
    public ServerEvalCommand () {
        super(
                "servereval",
                "Evaluate codes using LuaJ",
                new String[] { "<ownerHash> <{code}>" },
                new String[] { "lastseen" },
                TrustLevel.OWNER
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        try {
            final Bot bot = context.bot();

            bot.eval().globals().set("context", CoerceJavaToLua.coerce(context));

            final LuaValue output = context.bot().eval().run(String.join(" ", args));

            return Component.text(output.toString()).color(NamedTextColor.GREEN);
        } catch (Exception e) {
            return Component.text(e.toString()).color(NamedTextColor.RED);
        }
    }
}
