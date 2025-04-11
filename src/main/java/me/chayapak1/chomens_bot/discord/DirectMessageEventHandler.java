package me.chayapak1.chomens_bot.discord;

import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.plugins.HashingPlugin;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class DirectMessageEventHandler extends ListenerAdapter {
    private static final String HASH_MESSAGE = "hash";

    private final JDA jda;

    private final Configuration.Discord options;

    public DirectMessageEventHandler (
            final JDA jda,
            final Configuration.Discord options
    ) {
        this.jda = jda;
        this.options = options;

        jda.addEventListener(this);
    }

    @Override
    public void onMessageReceived (@NotNull final MessageReceivedEvent event) {
        if (!options.enableDiscordHashing || event.getChannelType() != ChannelType.PRIVATE) return;

        final Message message = event.getMessage();

        if (!message.getContentDisplay().equalsIgnoreCase(HASH_MESSAGE)) return;

        final Guild guild;

        try {
            guild = jda.getGuildById(options.serverId);
        } catch (final NumberFormatException e) {
            // there will be a custom error message
            // from JDA when it fails to parse a snowflake,
            // the user will read that
            LoggerUtilities.error(e);

            return;
        }

        if (guild == null) return;

        guild.retrieveMember(message.getAuthor())
                .queue(
                        member -> {
                            if (member == null) return;

                            final TrustLevel trustLevel = TrustLevel.fromDiscordRoles(member.getRoles());

                            if (trustLevel == TrustLevel.PUBLIC) {
                                LoggerUtilities.log(
                                        LogType.DISCORD,
                                        Component.translatable(
                                                "User %s tried to get hash in Discord without any trusted roles!",
                                                Component.text(member.toString())
                                        )
                                );
                                message.reply("You do not have any trusted roles!")
                                        .queue();
                                return;
                            }

                            sendHash(trustLevel, message, member);
                        },
                        exception -> {
                            if (!(exception instanceof final ErrorResponseException error)) return;

                            final ErrorResponse errorResponse = error.getErrorResponse();

                            if (errorResponse == ErrorResponse.UNKNOWN_MEMBER) {
                                message.reply("You are not in " + guild.getName() + "!")
                                        .queue();
                            } else if (errorResponse == ErrorResponse.UNKNOWN_USER) {
                                LoggerUtilities.error(
                                        Component.translatable(
                                                "Got ErrorResponse.UNKNOWN_USER while trying to " +
                                                        "retrieve member! Weird user. User: %s",
                                                Component.text(message.getAuthor().toString())
                                        )
                                );
                            }
                            // that's all the possible errors, the javadocs said
                        }
                );
    }

    private void sendHash (final TrustLevel trustLevel, final Message message, final Member member) {
        final String result = HashingPlugin.generateDiscordHash(member.getIdLong(), trustLevel);

        message
                .reply(
                        String.format(
                                "Hash for %s trust level: **%s**",
                                trustLevel,
                                result
                        )
                )
                .queue();

        LoggerUtilities.log(
                LogType.DISCORD,
                Component.translatable(
                        "Generated hash %s (%s) for user %s",
                        Component.text(result),
                        Component.text(trustLevel.toString()),
                        Component.text(member.getEffectiveName())
                )
        );
    }
}
