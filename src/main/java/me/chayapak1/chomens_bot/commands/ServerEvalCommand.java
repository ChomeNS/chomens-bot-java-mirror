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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ServerEvalCommand extends Command {
    public Lua lua;

    public ServerEvalCommand () {
        super(
                "servereval",
                new String[] { "reset", "<code>" },
                new String[] {},
                TrustLevel.OWNER
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String code = context.getString(true, true);

        if (code.equalsIgnoreCase("reset")) {
            if (lua != null) lua.close();
            lua = null;

            return Component.translatable("commands.servereval.reset", bot.colorPalette.defaultColor);
        }

        bot.executorService.execute(() -> {
            try {
                if (lua == null) lua = new Lua54();

                lua.openLibraries();

                lua.set("lua", lua);
                lua.set("bot", bot);
                lua.set("context", context);
                lua.set("shell", new Shell());

                final LuaValue[] values = lua.eval(code);

                final StringBuilder output = new StringBuilder();

                if (values.length != 1) {
                    output.append('[');

                    int i = 1;
                    for (final LuaValue value : values) {
                        output.append(getString(value));
                        if (i++ != values.length) output.append(", ");
                    }

                    output.append(']');
                } else {
                    output.append(getString(values[0]));
                }

                context.sendOutput(Component.text(output.toString(), NamedTextColor.GREEN));
            } catch (final Exception e) {
                context.sendOutput(Component.text(e.toString(), NamedTextColor.RED));
            }
        });

        return null;
    }

    private String getString (final LuaValue luaValue) {
        final Object javaObject = luaValue.toJavaObject();

        if (javaObject == null) return luaValue.toString();
        else return javaObject.toString();
    }

    @SuppressWarnings("unused") // we actually use it in lua itself :)
    public static final class Shell {
        public String execute (final String[] command) throws Exception {
            final ProcessBuilder processBuilder = new ProcessBuilder();

            processBuilder.command(command);

            final Process process = processBuilder.start();

            final StringBuilder output = new StringBuilder();

            final BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            int character;

            while ((character = stdoutReader.read()) != -1) {
                final char[] chars = Character.toChars(character);
                final String string = new String(chars);
                output.append(string);
            }

            final BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            while ((character = stderrReader.read()) != -1) {
                final char[] chars = Character.toChars(character);
                final String string = new String(chars);
                output.append("[STDERR] ").append(string);
            }

            process.waitFor(10, TimeUnit.SECONDS);

            return output.toString();
        }
    }
}
