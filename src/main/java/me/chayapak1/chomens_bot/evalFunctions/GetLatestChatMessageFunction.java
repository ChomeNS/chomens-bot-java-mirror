package me.chayapak1.chomens_bot.evalFunctions;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;
import me.chayapak1.chomens_bot.plugins.ChatPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class GetLatestChatMessageFunction extends EvalFunction implements ChatPlugin.Listener {
    private String latestMessage = "";

    public GetLatestChatMessageFunction (Bot bot) {
        super("getLatestChatMessage", bot);

        bot.chat.addListener(this);
    }

    @Override
    public boolean systemMessageReceived (Component component, String string, String ansi) {
        latestMessage = GsonComponentSerializer.gson().serialize(component);

        return true;
    }

    @Override
    public Output execute(Object... args) {
        return new Output(latestMessage, true);
    }
}
