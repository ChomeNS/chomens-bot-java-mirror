package me.chayapak1.chomens_bot.plugins;

import lombok.Getter;
import me.chayapak1.chomens_bot.Bot;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

public class EvalRunnerPlugin {
    @Getter private final Globals globals = JsePlatform.standardGlobals();

    public EvalRunnerPlugin (Bot bot) {
        globals.set("bot", CoerceJavaToLua.coerce(bot));
    }

    public LuaValue run (String code) {
        LuaValue chunk = globals.load(code);

        return chunk.call();
    }
}
