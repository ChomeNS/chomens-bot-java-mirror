package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
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
                new String[] { "<code>" },
                new String[] {},
                TrustLevel.OWNER,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        bot.executorService.submit(() -> {
            try {
                final Globals globals = JsePlatform.standardGlobals();

                globals.set("bot", CoerceJavaToLua.coerce(bot));
                globals.set("class", CoerceJavaToLua.coerce(Class.class));
                globals.set("context", CoerceJavaToLua.coerce(context));

                LuaValue chunk = globals.load(context.getString(true, true));

                final LuaValue output = chunk.call();

                context.sendOutput(Component.text(output.toString()).color(NamedTextColor.GREEN));
            } catch (CommandException e) {
                context.sendOutput(e.message.color(NamedTextColor.RED));
            } catch (Exception e) {
                context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
            }
        });

        return null;
    }
}
