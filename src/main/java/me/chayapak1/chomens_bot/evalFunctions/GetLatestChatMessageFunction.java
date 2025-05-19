package me.chayapak1.chomens_bot.evalFunctions;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.util.SNBTUtilities;
import net.kyori.adventure.text.Component;

public class GetLatestChatMessageFunction extends EvalFunction implements Listener {
    private String latestMessage = "";

    public GetLatestChatMessageFunction (final Bot bot) {
        super("getLatestChatMessage", bot);

        bot.listener.addListener(this);
    }

    @Override
    public boolean onSystemMessageReceived (final Component component, final String string, final String ansi) {
        latestMessage = SNBTUtilities.fromComponent(false, component);

        return true;
    }

    @Override
    public Output execute (final Object... args) {
        return new Output(latestMessage, true);
    }
}
