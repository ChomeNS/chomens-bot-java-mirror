package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

public class ServerEvalCommand extends Command {
    public ServerEvalCommand () {
        super(
                "servereval",
                "Evaluate codes using LuaJ",
                new String[] { "<ownerHash> <code>" },
                new String[] {},
                TrustLevel.OWNER,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        try {
            final Bot bot = context.bot;

            final Globals globals = JsePlatform.standardGlobals();

            globals.set("bot", CoerceJavaToLua.coerce(bot));
            globals.set("class", CoerceJavaToLua.coerce(Class.class));
            globals.set("context", CoerceJavaToLua.coerce(context));

            LuaValue chunk = globals.load(context.getString(true, true));

            final LuaValue output = chunk.call();

            return Component.text(output.toString()).color(NamedTextColor.GREEN);
        } catch (Exception e) {
            throw new CommandException(Component.text(e.toString()));
        }
    }
}
