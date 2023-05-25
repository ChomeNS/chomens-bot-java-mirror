package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.Bot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// idea totallynotskidded™ from chipmunkbot (the js one)
public class ClearChatUsernamePlugin extends ChatPlugin.Listener {
    private final Bot bot;

    public static final Pattern PATTERN = Pattern.compile("/clearchat.*|/cc.*|/extras:clearchat.*|/extras:cc.*");

    public ClearChatUsernamePlugin(Bot bot) {
        this.bot = bot;

        bot.chat().addListener(this);
    }

    @Override
    public void commandSpyMessageReceived (PlayerMessage message) {
        final String username = message.sender().profile().getName();
        final String command = ((TextComponent) message.contents()).content();

        final Matcher matcher = PATTERN.matcher(command);

        if (matcher.find()) {
            bot.chat().tellraw(
                    Component.empty()
                            .append(Component.text(username))
                            .append(Component.text(" cleared the chat"))
                            .color(NamedTextColor.DARK_GREEN)
            );
        }
    }
}
