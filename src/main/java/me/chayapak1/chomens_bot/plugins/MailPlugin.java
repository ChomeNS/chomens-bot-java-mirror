package me.chayapak1.chomens_bot.plugins;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.Mail;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import me.chayapak1.chomens_bot.util.PersistentDataUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MailPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    public static JsonArray mails = new JsonArray();

    private final Gson gson = new Gson();

    static {
        if (PersistentDataUtilities.jsonObject.has("mails")) {
            mails = PersistentDataUtilities.jsonObject.get("mails").getAsJsonArray();
        }
    }

    public MailPlugin (Bot bot) {
        this.bot = bot;

        bot.players.addListener(this);
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        final String name = target.profile.getName();

        int sendToTargetSize = 0;

        boolean shouldSend = false;
        for (JsonElement mailElement : mails) {
            final Mail mail = gson.fromJson(mailElement, Mail.class);

            if (!mail.sentTo.equals(name)) continue;

            shouldSend = true;

            sendToTargetSize++;
        }

        if (shouldSend) {
            final Component component = Component.translatable(
                    "You have %s new mail%s!\n" +
                            "Do %s or %s to read",
                    Component.text(sendToTargetSize).color(NamedTextColor.GREEN),
                    Component.text((sendToTargetSize > 1) ? "s" : ""),
                    Component.text(bot.config.commandSpyPrefixes.get(0) + "mail read").color(ColorUtilities.getColorByString(bot.config.colorPalette.primary)),
                    Component.text(bot.config.prefixes.get(0) + "mail read").color(ColorUtilities.getColorByString(bot.config.colorPalette.primary))
            ).color(NamedTextColor.GOLD);

            bot.chat.tellraw(component, target.profile.getId());
        }
    }

    public void send (Mail mail) {
        mails.add(gson.fromJson(gson.toJson(mail), JsonElement.class)); // is this the best way?

        PersistentDataUtilities.put("mails", mails);
    }
}
