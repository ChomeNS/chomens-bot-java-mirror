package land.chipmunk.chayapak.chomens_bot.command;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.util.CodeBlockUtilities;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyori.adventure.text.Component;

import java.awt.*;
import java.util.UUID;

public class DiscordCommandContext extends CommandContext {
    private final MessageReceivedEvent event;

    private final Bot bot;

    public DiscordCommandContext(Bot bot, String prefix, MessageReceivedEvent event, String hash, String ownerHash) {
        super(
                bot,
                prefix,
                new MutablePlayerListEntry(
                        new GameProfile(
                                new UUID(0L, 0L),
                                null
                        ),
                        GameMode.SURVIVAL,
                        0,
                        Component.text(event.getAuthor().getName()),
                        0L,
                        null,
                        new byte[0],
                        true
                ),
                hash,
                ownerHash
        );
        this.bot = bot;
        this.event = event;
    }

    @Override
    public void sendOutput (Component component) {
        final String output = ComponentUtilities.stringifyAnsi(component);
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Output");
        builder.setColor(Color.decode(bot.config().discord().embedColors().normal()));
        builder.setDescription("```ansi\n" + CodeBlockUtilities.escape(output.replace("\u001b[9", "\u001b[3")) + "\n```");

        final MessageEmbed embed = builder.build();

        event.getMessage().replyEmbeds(embed).queue();
    }

    @Override
    public Component displayName () {
        return Component.text(event.getAuthor().getName());
    }
}
