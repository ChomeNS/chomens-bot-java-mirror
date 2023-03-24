package me.chayapak1.chomens_bot.chatParsers;

import me.chayapak1.chomens_bot.chatParsers.data.ChatParser;
import me.chayapak1.chomens_bot.chatParsers.data.PlayerMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Might be a confusing name, but I mean the [Chat] chayapak custom chat thing or any other
// custom chat that uses the `[%s] %s › %s` translation
public class ChomeNSCustomChatParser implements ChatParser {

    public ChomeNSCustomChatParser () {
    }

    @Override
    public PlayerMessage parse (Component message) {
        if (message instanceof TranslatableComponent) return parse((TranslatableComponent) message);
        return null;
    }

    // very similar to MinecraftChatParser
    public PlayerMessage parse (TranslatableComponent message) {
        final List<Component> args = message.args();
        if (args.size() < 3 || !message.key().equals("[%s] %s › %s")) return null;

        final Map<String, Component> parameters = new HashMap<>();

        final Component username = args.get(1);
        final Component contents = args.get(2);

        parameters.put("sender", username);
        parameters.put("contents", contents);

        return new PlayerMessage(parameters);
    }
}
