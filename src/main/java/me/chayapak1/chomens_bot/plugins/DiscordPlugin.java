package me.chayapak1.chomens_bot.plugins;

import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import lombok.Getter;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.DiscordCommandContext;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.EscapeCodeBlock;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

public class DiscordPlugin {
    @Getter private JDA jda;

    public final Map<String, String> servers;

    public final String prefix;

    @Getter private static CountDownLatch readyLatch = new CountDownLatch(1);

    private final Map<String, Boolean> alreadyAddedListeners = new HashMap<>();

    public DiscordPlugin (Configuration config) {
        final Configuration.Discord options = config.discord();
        this.prefix = options.prefix();
        this.servers = options.servers();

        JDABuilder builder = JDABuilder.createDefault(options.token());

        new Thread(() -> {
            try {
                jda = builder.build();
                jda.awaitReady();
            } catch (LoginException e) {
                System.err.println("Failed to login to Discord, stacktrace:");
                e.printStackTrace();
                System.exit(1);
            } catch (InterruptedException ignored) {
                System.exit(1);
            }

            readyLatch.countDown();
        }).start();

        try {
            Main.latch.await();
        } catch (InterruptedException ignored) { System.exit(1); }

        for (Bot bot : Main.allBots) {
            String channelId = servers.get(bot.host() + ":" + bot.port());

            boolean channelAlreadyAddedListeners = alreadyAddedListeners.getOrDefault(channelId, false);

              bot.addListener(new SessionAdapter() {
                  @Override
                  public void connected(ConnectedEvent event) {
                      boolean channelAlreadyAddedListeners = alreadyAddedListeners.getOrDefault(channelId, false);
                      if (channelAlreadyAddedListeners) return;

                      sendMessageInstantly("Successfully connected to: " + "`" + bot.host() + ":" + bot.port() + "`", channelId);

                      jda.addEventListener(new ListenerAdapter() {
                          @Override
                          public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                              if (
                                      !event.getChannel().getId().equals(channelId) ||
                                              event.getAuthor().getId().equals(jda.getSelfUser().getId())
                              ) return;

                              final String tag = event.getAuthor().getAsTag();
                              final String message = event.getMessage().getContentRaw();

                              if (message.startsWith(prefix)) {
                                  final DiscordCommandContext context = new DiscordCommandContext(bot, prefix, event, null, null);

                                  final Component output = bot.commandHandler().executeCommand(message.substring(prefix.length()), context, true, null, null, event);
                                  final String textOutput = ((TextComponent) output).content();

                                  if (!textOutput.equals("success")) {
                                      context.sendError(output);
                                  }

                                  return;
                              }

                              final Component component = Component.translatable(
                                      "[%s %s] %s › %s",
                                      Component.text("ChomeNS").color(NamedTextColor.YELLOW),
                                      Component.text("Discord").color(NamedTextColor.BLUE),
                                      Component.text(tag).color(NamedTextColor.RED),
                                      Component.text(message).color(NamedTextColor.GRAY)
                              ).color(NamedTextColor.DARK_GRAY);
                              bot.chat().tellraw(component);
                          }
                      });

                      alreadyAddedListeners.put(channelId, true);

                      bot.chat().addListener(new ChatPlugin.ChatListener() {
                          @Override
                          public void systemMessageReceived (String ignoredMessage, Component component) {
                              final String content = ComponentUtilities.stringifyAnsi(component);
                              sendMessage(EscapeCodeBlock.escape(content.replace("\u001b[9", "\u001b[3")), channelId);
                          }
                      });

                      final TimerTask task = new TimerTask() {
                          @Override
                          public void run() {
                              onDiscordTick(channelId);
                          }
                      };

                      final Timer timer = new Timer();
                      timer.schedule(task, 50, 50);
                  }

                  @Override
                  public void disconnected(DisconnectedEvent event) {
                      sendMessageInstantly("Disconnected: " + "`" + event.getReason().replace("`", "\\`") + "`", channelId);
                  }
              });
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
                if (message.length() >= 2000) {
                    message = message.substring(0, 2000);
                }
                logMessage.setLength(0);
            }
            sendMessageInstantly("```ansi\n" + message + "\n```", channelId);
        }
    }
}
