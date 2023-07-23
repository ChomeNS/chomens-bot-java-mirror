package land.chipmunk.chayapak.chomens_bot.evalFunctions;

import land.chipmunk.chayapak.chomens_bot.Bot;

public class ChatFunction extends EvalFunction {
    public ChatFunction (Bot bot) {
        super("chat", bot);
    }

    @Override
    public Output execute(Object... args) {
        final String message = (String) args[0];

        bot.chat.send(message);

        return null;
    }
}
