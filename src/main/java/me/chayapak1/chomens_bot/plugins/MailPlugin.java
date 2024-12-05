package me.chayapak1.chomens_bot.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.Mail;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import me.chayapak1.chomens_bot.util.PersistentDataUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MailPlugin extends PlayersPlugin.Listener {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    private final Bot bot;

    public static ArrayNode mails = (ArrayNode) PersistentDataUtilities.getOrDefault("mails", JsonNodeFactory.instance.arrayNode());

    public MailPlugin (Bot bot) {
        this.bot = bot;

        bot.players.addListener(this);
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        final String name = target.profile.getName();

        int sendToTargetSize = 0;

        boolean shouldSend = false;
        for (JsonNode mailNode : mails.deepCopy()) {
            try {
                final Mail mail = objectMapper.treeToValue(mailNode, Mail.class);

                if (!mail.sentTo.equals(name)) continue;

                shouldSend = true;

                sendToTargetSize++;
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
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
        mails.add(objectMapper.valueToTree(mail));

        PersistentDataUtilities.put("mails", mails);
    }

    public void remove (JsonNode mail) {
        for (int i = 0; i < mails.size(); i++) {
            final JsonNode currentNode = mails.get(i);

            if (currentNode.equals(mail)) {
                mails.remove(i);
                break;
            }
        }
    }
}
