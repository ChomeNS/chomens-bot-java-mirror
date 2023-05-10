package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Configuration;
import land.chipmunk.chayapak.chomens_bot.Main;
import land.chipmunk.chayapak.chomens_bot.command.DiscordCommandContext;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.CodeBlockUtilities;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

// please ignore my shitcode
public class DiscordPlugin {
    @Getter private JDA jda;

    public final Map<String, String> servers;

    public final String prefix;

    private final Map<String, Boolean> alreadyAddedListeners = new HashMap<>();

    public DiscordPlugin (Configuration config, JDA jda) {
        final Configuration.Discord options = config.discord();
        this.prefix = options.prefix();
        this.servers = options.servers();
        this.jda = jda;

        for (Bot bot : Main.bots) {
            String channelId = servers.get(bot.host() + ":" + bot.port());

              bot.addListener(new Bot.Listener() {
                  @Override
                  public void connecting() {
                      sendMessageInstantly(
                              String.format(
                                      "Connecting to: `%s:%s`",
                                      bot.host(),
                                      bot.port()
                              ),
                              channelId
                      );
                  }

                  @Override
                  public void connected (ConnectedEvent event) {
                      boolean channelAlreadyAddedListeners = alreadyAddedListeners.getOrDefault(channelId, false);

                      sendMessageInstantly(
                              String.format(
                                      "Successfully connected to: `%s:%s`",
                                      bot.host(),
                                      bot.port()
                              ),
                              channelId
                      );

                      if (channelAlreadyAddedListeners) return;

                      bot.tick().addListener(new TickPlugin.Listener() {
                          @Override
                          public void onTick() {
                              onDiscordTick(channelId);
                          }
                      });

                      bot.chat().addListener(new ChatPlugin.Listener() {
                          @Override
                          public void systemMessageReceived (Component component) {
                              final String content = ComponentUtilities.stringifyAnsi(component);
                              sendMessage(CodeBlockUtilities.escape(content.replace("\u001b[9", "\u001b[3")), channelId);
                          }
                      });

                      jda.addEventListener(new ListenerAdapter() {
                          @Override
                          public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                              if (
                                      !event.getChannel().getId().equals(channelId) ||
                                              event.getAuthor().getId().equals(jda.getSelfUser().getId())
                              ) return;

                              final Message _message = event.getMessage();
                              final String message = _message.getContentRaw();

                              if (message.startsWith(prefix)) {
                                  final DiscordCommandContext context = new DiscordCommandContext(bot, prefix, event, null, null);

                                  final Component output = bot.commandHandler().executeCommand(message.substring(prefix.length()), context, false, true, false, null, null, event);

                                  if (output != null) {
                                      context.sendOutput(output);
                                  }

                                  return;
                              }

                              Component attachmentsComponent = Component.empty();
                              if (_message.getAttachments().size() > 0) {
                                  for (Message.Attachment attachment : _message.getAttachments()) {
                                      attachmentsComponent = attachmentsComponent
                                              .append(Component.text(" "))
                                              .append(
                                                Component
                                                          .text("[Attachment]")
                                                          .clickEvent(ClickEvent.openUrl(attachment.getProxyUrl()))
                                                          .color(NamedTextColor.GREEN)
                                              );
                                  }
                              }

                              final String tag = event.getMember().getUser().getDiscriminator();

                              String name = event.getMember().getNickname();
                              final String fallbackName = event.getAuthor().getName();
                              if (name == null) name = fallbackName;

                              final Component nameComponent = Component
                                      .text(name)
                                      .clickEvent(ClickEvent.copyToClipboard(fallbackName + "#" + tag))
                                      .hoverEvent(
                                              HoverEvent.showText(
                                                      Component.translatable(
                                                              "%s#%s\n%s",
                                                              Component.text(fallbackName).color(NamedTextColor.WHITE),
                                                              Component.text(tag).color(NamedTextColor.GRAY),
                                                              Component.text("Click here to copy the tag to your clipboard").color(NamedTextColor.GREEN)
                                                      ).color(NamedTextColor.DARK_GRAY)
                                              )
                                      )
                                      .color(NamedTextColor.RED);

                              final String discordUrl = config.discord().inviteLink();

                              final Component discordComponent = Component.empty()
                                      .append(Component.text("ChomeNS ").color(NamedTextColor.YELLOW))
                                      .append(Component.text("Discord").color(NamedTextColor.BLUE))
                                      .hoverEvent(
                                              HoverEvent.showText(
                                                      Component.text("Click here to join the Discord server").color(NamedTextColor.GREEN)
                                              )
                                      )
                                      .clickEvent(ClickEvent.openUrl(discordUrl));

                              final Component deserialized = LegacyComponentSerializer.legacyAmpersand().deserialize(message.replace("\uD83D\uDC80", "☠"));

                              final Component messageComponent = Component
                                      .text("")
                                      .color(NamedTextColor.GRAY)
                                      .append(deserialized.append(attachmentsComponent));

                              final Component component = Component.translatable(
                                      "[%s] %s › %s",
                                      discordComponent,
                                      nameComponent,
                                      messageComponent
                              ).color(NamedTextColor.DARK_GRAY);
                              bot.chat().tellraw(component);
                          }
                      });

                      alreadyAddedListeners.put(channelId, true);
                  }

                  @Override
                  public void disconnected(DisconnectedEvent event) {
                      final String reason = ComponentUtilities.stringifyAnsi(event.getReason());
                      sendMessageInstantly(
                              "Disconnected: \n" +
                                      "```ansi\n" +
                                      reason.replace("`", "\\`") +
                                      "\n```"
                              , channelId
                      );
                  }
              });

            bot.discord(this);
        }
    }

    // totallynotskidded™️ from HBot (and changed a bit)
    final Map<String, StringBuilder> logMessages = new HashMap<>();
    final Map<String, Long> nextLogTimes = new HashMap<>();
    final Map<String, Boolean> doneSendingInLogs = new HashMap<>();

    public void sendMessage(String message, String channelId) {
        synchronized (logMessages) {
            if (!logMessages.containsKey(channelId)) {
                logMessages.put(channelId, new StringBuilder());
            }
            StringBuilder logMessage = logMessages.get(channelId);
            if (logMessage.length() < 2000) {
                if (logMessage.length() > 0) {
                    logMessage.append('\n');
                }
                logMessage.append(message);
            }
        }
    }

    public void sendMessageInstantly (String message, String channelId) {
        final TextChannel logChannel = jda.getTextChannelById(channelId);
        logChannel.sendMessage(message).queue(
                (msg) -> doneSendingInLogs.put(channelId, true),
                (err) -> doneSendingInLogs.put(channelId, false)
        );
    }

    public void onDiscordTick(String channelId) {
        synchronized (logMessages) {
            if (!logMessages.containsKey(channelId) || logMessages.get(channelId).length() == 0) {
                return;
            }
        }

        long currentTime = System.currentTimeMillis();
        if (!nextLogTimes.containsKey(channelId) || (currentTime >= nextLogTimes.get(channelId) && doneSendingInLogs.get(channelId))
                || currentTime - nextLogTimes.get(channelId) > 5000) {
            long logDelay = 2000; // mabe don't hardcode this

            nextLogTimes.put(channelId, currentTime + logDelay);
            String message;
            synchronized (logMessages) {
                StringBuilder logMessage = logMessages.get(channelId);
                message = logMessage.toString();
                final int maxLength = 2_000 - ("""
                        ```ansi

                        ```"""
                ).length(); // kinda sus
                if (message.length() >= maxLength) {
                    message = message.substring(0, maxLength);
                }
                logMessage.setLength(0);
            }
            sendMessageInstantly("```ansi\n" + message + "\n```", channelId);
        }
    }
}
