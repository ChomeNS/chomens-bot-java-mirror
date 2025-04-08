package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import party.iroiro.luajava.Lua;
import party.iroiro.luajava.lua54.Lua54;
import party.iroiro.luajava.value.LuaValue;

import java.util.Arrays;

public class ServerEvalCommand extends Command {
    public Lua lua;

    public ServerEvalCommand () {
        super(
                "servereval",
                "Evaluate codes using LuaJ",
                new String[] { "<code>" },
                new String[] {},
                TrustLevel.OWNER
        );
    }

    @Override
    public Component execute (CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String code = context.getString(true, true);

        if (code.equalsIgnoreCase("reset")) {
            if (lua != null) lua.close();
            lua = null;

            return Component.text("Reset the Lua instance").color(bot.colorPalette.defaultColor);
        }

        bot.executorService.submit(() -> {
            try {
                if (lua == null) lua = new Lua54();

                lua.openLibraries();

                lua.set("bot", bot);
                lua.set("context", context);

                final LuaValue[] values = lua.eval(code);

                final String output = values.length < 1 ? Arrays.toString(values) : values[0].toString();

                context.sendOutput(Component.text(output).color(NamedTextColor.GREEN));
            } catch (Exception e) {
                context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
            }
        });

        return null;
    }
}
