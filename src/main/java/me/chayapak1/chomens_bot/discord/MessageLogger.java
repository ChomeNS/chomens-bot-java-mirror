package me.chayapak1.chomens_bot.discord;

import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public class MessageLogger extends ListenerAdapter {
    private final JDA jda;

    public MessageLogger (final JDA jda) {
        this.jda = jda;

        jda.addEventListener(this);
    }

    @Override
    public void onMessageReceived (@NotNull final MessageReceivedEvent event) {
        final User author = event.getAuthor();

        if (author.getId().equals(jda.getSelfUser().getId())) return;

        final Message message = event.getMessage();

        final String source = event.isFromGuild()
                ? String.format(
                        "%s - %s (%s)",
                        event.getGuild().getName(),
                        event.getGuildChannel().getName(),
                        event.getGuildChannel().getId()
                )
                : event.getChannelType().name();

        // very simple, just the contents, no reply, forward, attachment, embeds, nothing but the raw contents
        final Component component = Component
                .translatable(
                        "[%s] %s › %s",
                        Component.text(source, NamedTextColor.GRAY),
                        Component.text(author.getName(), NamedTextColor.RED),
                        Component.text(message.getContentRaw(), NamedTextColor.GRAY)
                )
                .color(NamedTextColor.DARK_GRAY);

        LoggerUtilities.log(
                LogType.DISCORD,
                component
        );
    }
}
