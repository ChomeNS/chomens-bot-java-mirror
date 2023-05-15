package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.Mail;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class MailPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    // TODO: make this persistent
    @Getter private final List<Mail> mails = new ArrayList<>();

    public MailPlugin (Bot bot) {
        this.bot = bot;

        bot.players().addListener(this);
    }

    @Override
    public void playerJoined(MutablePlayerListEntry target) {
        final String name = target.profile().getName();

        final List<String> sendTos = new ArrayList<>(); // confusing name,.,.

        for (Mail mail : mails) sendTos.add(mail.sentTo());

        boolean shouldSend = false;
        for (Mail mail : mails) {
            if (mail.sentTo().equals(name)) {
                shouldSend = true;
                break;
            }
        }

        if (shouldSend) {
            final Component component = Component.translatable(
                    "You have %s new mail%s!\n" +
                            "Do %s or %s to read",
                    Component.text(sendTos.size()).color(NamedTextColor.GREEN),
                    Component.text((sendTos.size() > 1) ? "s" : ""),
                    Component.text(bot.config().commandSpyPrefixes().get(0) + "mail read").color(ColorUtilities.getColorByString(bot.config().colorPalette().primary())),
                    Component.text(bot.config().prefixes().get(0) + "mail read").color(ColorUtilities.getColorByString(bot.config().colorPalette().primary()))
            ).color(NamedTextColor.GOLD);

            bot.chat().tellraw(component, target.profile().getId());
        }
    }

    public void send (Mail mail) {
        mails.add(mail);
    }
}
