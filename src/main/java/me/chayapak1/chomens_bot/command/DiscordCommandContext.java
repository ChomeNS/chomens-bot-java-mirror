package me.chayapak1.chomens_bot.command;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.CodeBlockUtilities;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;

import java.awt.*;
import java.nio.charset.StandardCharsets;

public class DiscordCommandContext extends CommandContext {
    private final MessageReceivedEvent event;

    private final Bot bot;

    public DiscordCommandContext(Bot bot, String prefix, MessageReceivedEvent event) {
        super(
                bot,
                prefix,
                new PlayerEntry(
                        new GameProfile(
                                UUIDUtilities.getOfflineUUID(event.getAuthor().getName()),
                                event.getAuthor().getName()
                        ),
                        GameMode.SURVIVAL,
                        -69420,
                        Component.text(event.getAuthor().getName()),
                        0L,
                        null,
                        new byte[0],
                        true
                ),
                false
        );
        this.bot = bot;
        this.event = event;
    }

    @Override
    public void sendOutput (Component component) {
        String output = ComponentUtilities.stringifyDiscordAnsi(component);

        if (output.length() > 2048) {
            output = ComponentUtilities.stringify(component);

            event.getMessage().replyFiles(
                    FileUpload.fromData(
                            output.getBytes(StandardCharsets.UTF_8),
                            String.format("output-%d.txt", System.currentTimeMillis())
                    )
            ).queue();
        } else {
            final EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Output");
            builder.setColor(Color.decode(bot.config.discord.embedColors.normal));
            builder.setDescription("```ansi\n" + CodeBlockUtilities.escape(output) + "\n```");

            final MessageEmbed embed = builder.build();

            event.getMessage().replyEmbeds(embed).queue();
        }
    }

    @Override
    public Component displayName () {
        return Component.text(event.getAuthor().getName());
    }
}
