package me.chayapak1.chomens_bot.discord;

import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.util.HashingUtilities;
import me.chayapak1.chomens_bot.util.I18nUtilities;
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
    private static final String KEY_MESSAGE = "key";
    private static final String FORCED_KEY_MESSAGE = "key force";

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

        if (
                !message.getContentDisplay().equalsIgnoreCase(HASH_MESSAGE)
                        && !message.getContentDisplay().equalsIgnoreCase(KEY_MESSAGE)
                        && !message.getContentDisplay().equalsIgnoreCase(FORCED_KEY_MESSAGE)
        ) return;

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
                                                I18nUtilities.get("hashing.discord_direct_message.error.no_roles.log"),
                                                Component.text(member.toString())
                                        )
                                );
                                message
                                        .reply(I18nUtilities.get("hashing.discord_direct_message.error.no_roles"))
                                        .queue();
                                return;
                            }

                            switch (message.getContentDisplay().toLowerCase()) {
                                case HASH_MESSAGE -> sendHash(trustLevel, message, member);
                                case KEY_MESSAGE -> sendKey(trustLevel, message, member, false);
                                case FORCED_KEY_MESSAGE -> sendKey(trustLevel, message, member, true);
                            }
                        },
                        exception -> {
                            if (!(exception instanceof final ErrorResponseException error)) return;

                            final ErrorResponse errorResponse = error.getErrorResponse();

                            if (errorResponse == ErrorResponse.UNKNOWN_MEMBER) {
                                message
                                        .reply(
                                                String.format(
                                                        I18nUtilities.get("hashing.discord_direct_message.error.not_in_guild"),
                                                        guild.getName()
                                                )
                                        )
                                        .queue();
                            } else if (errorResponse == ErrorResponse.UNKNOWN_USER) {
                                LoggerUtilities.error(
                                        Component.translatable(
                                                I18nUtilities.get("hashing.discord_direct_message.error.log_unknown_user"),
                                                Component.text(message.getAuthor().toString())
                                        )
                                );
                            }
                            // that's all the possible errors, the javadocs said
                        }
                );
    }

    private void sendHash (final TrustLevel trustLevel, final Message message, final Member member) {
        final String result = HashingUtilities.generateDiscordHash(member.getIdLong(), trustLevel);

        message
                .reply(
                        String.format(
                                I18nUtilities.get("hashing.discord_direct_message.hash_generated"),
                                trustLevel,
                                result
                        )
                )
                .queue();

        LoggerUtilities.log(
                LogType.DISCORD,
                Component.translatable(
                        I18nUtilities.get("hashing.discord_direct_message.hash_generated.log"),
                        Component.text(result),
                        Component.text(trustLevel.toString()),
                        Component.text(member.getEffectiveName())
                )
        );
    }

    private void sendKey (final TrustLevel trustLevel, final Message message, final Member member, final boolean force) {
        try {
            final String generatedKey = HashingUtilities.KEY_MANAGER.generate(
                    trustLevel,
                    member.getId(),
                    force,
                    String.format(
                            // long ahh
                            I18nUtilities.get("hashing.discord_direct_message.error.key_for_trust_level_already_exists"),

                            trustLevel,
                            FORCED_KEY_MESSAGE
                    )
            );

            message
                    .reply(
                            String.format(
                                    I18nUtilities.get("hashing.discord_direct_message.key_generated"),
                                    trustLevel,
                                    generatedKey
                            )
                    )
                    .queue();
        } catch (final IllegalStateException e) {
            message.reply(e.getMessage()).queue();
        }
    }
}
