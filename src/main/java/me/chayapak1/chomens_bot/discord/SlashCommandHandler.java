package me.chayapak1.chomens_bot.discord;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.contexts.DiscordCommandContext;
import me.chayapak1.chomens_bot.plugins.CommandHandlerPlugin;
import me.chayapak1.chomens_bot.util.I18nUtilities;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;

public class SlashCommandHandler extends ListenerAdapter {
    private final JDA jda;

    public SlashCommandHandler (final JDA jda) {
        this.jda = jda;

        addCommands();

        jda.addEventListener(this);
    }

    private void addCommands () {
        CommandListUpdateAction commandListAction = jda.updateCommands();
        for (final Command command : CommandHandlerPlugin.COMMANDS) {
            if (command.consoleOnly) continue;

            final SlashCommandData commandData = Commands.slash(
                    command.name,
                    I18nUtilities.get(
                            String.format(
                                    "commands.%s.description",
                                    command.name
                            )
                    )
            );

            commandData
                    .setContexts(InteractionContextType.GUILD)
                    .setDefaultPermissions(DefaultMemberPermissions.ENABLED)
                    .addOption(OptionType.STRING, "args", "Arguments for the command");

            commandListAction = commandListAction.addCommands(commandData);
        }

        commandListAction.queue();
    }

    @Override
    public void onSlashCommandInteraction (@NotNull final SlashCommandInteractionEvent event) {
        boolean found = false;

        for (final Bot bot : Main.bots) {
            final String channelId = bot.options.discordChannelId;

            if (channelId == null || !event.getChannel().getId().equals(channelId)) continue;

            if (!found) found = true;

            final DiscordCommandContext context = new DiscordCommandContext(
                    bot,
                    "/",
                    event.getMember(),
                    event.getUser().getName(),
                    fileUpload -> event.getInteraction().replyFiles(fileUpload).queue(),
                    embed -> event.getInteraction().replyEmbeds(embed).queue()
            );

            final OptionMapping args = event.getOption("args");

            // seems very ass
            final String input = event.getName() +
                    (args != null
                            ? " " + args.getAsString()
                            : ""
                    );

            bot.commandHandler.executeCommand(input, context);
        }

        if (!found) event.reply("You are not in one of the bot's log channels!").queue();
    }
}
