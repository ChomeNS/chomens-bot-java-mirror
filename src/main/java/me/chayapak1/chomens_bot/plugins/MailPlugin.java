package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.mail.Mail;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MailPlugin implements Listener {
    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS mails (sentBy VARCHAR(255), sentTo VARCHAR(255), timeSent BIGINT, server VARCHAR(255), contents TEXT);";
    private static final String INSERT_MAIL = "INSERT INTO mails (sentBy, sentTo, timeSent, server, contents) VALUES (?, ?, ?, ?, ?);";
    private static final String LIST_MAILS = "SELECT * FROM mails;";
    private static final String REMOVE_MAIL = "DELETE FROM mails WHERE sentTo = ?;";

    static {
        if (Main.database != null) {
            DatabasePlugin.EXECUTOR_SERVICE.execute(() -> {
                try {
                    Main.database.execute(CREATE_TABLE);
                } catch (final SQLException e) {
                    LoggerUtilities.error(e);
                }
            });
        }
    }

    private final Bot bot;

    public MailPlugin (final Bot bot) {
        this.bot = bot;

        if (Main.database == null) return;

        bot.listener.addListener(this);
    }

    @Override
    public void onPlayerJoined (final PlayerEntry target) {
        DatabasePlugin.EXECUTOR_SERVICE.execute(() -> {
            final String name = target.profile.getName();

            int sendToTargetSize = 0;

            final List<Mail> mails = list();

            for (final Mail mail : mails) {
                if (!mail.sentTo().equals(name)) continue;

                sendToTargetSize++;
            }

            if (sendToTargetSize > 0) {
                final Component component = Component.translatable(
                        "You have %s new mail%s!\n" +
                                "Run %s or %s to read",
                        NamedTextColor.GOLD,
                        Component.text(sendToTargetSize, NamedTextColor.GREEN),
                        Component.text((sendToTargetSize > 1) ? "s" : ""),
                        Component.text(bot.config.commandSpyPrefixes.getFirst() + "mail read", bot.colorPalette.primary),
                        Component.text(bot.config.prefixes.getFirst() + "mail read", bot.colorPalette.primary)
                );

                bot.chat.tellraw(component, target.profile.getId());
            }
        });
    }

    public void send (final Mail mail) throws CommandException {
        final List<Mail> mails = list();

        int count = 0;
        for (final Mail eachMail : mails) {
            if (!eachMail.sentBy().equals(mail.sentBy())) continue;

            if (count > 50) throw new CommandException(Component.translatable("commands.mail.error.spam"));

            count++;
        }

        try {
            final PreparedStatement statement = Main.database.connection.prepareStatement(INSERT_MAIL);

            statement.setString(1, mail.sentBy());
            statement.setString(2, mail.sentTo());
            statement.setLong(3, mail.timeSent());
            statement.setString(4, mail.server());
            statement.setString(5, mail.contents());

            statement.executeUpdate();
        } catch (final SQLException e) {
            bot.logger.error(e);
        }
    }

    public void clear (final String sentTo) {
        try {
            final PreparedStatement statement = Main.database.connection.prepareStatement(REMOVE_MAIL);

            statement.setString(1, sentTo);

            statement.executeUpdate();
        } catch (final SQLException e) {
            bot.logger.error(e);
        }
    }

    public List<Mail> list () {
        final List<Mail> output = new ArrayList<>();

        try (final ResultSet result = Main.database.query(LIST_MAILS)) {
            if (result == null) return output;

            while (result.next()) {
                final Mail mail = new Mail(
                        result.getString("sentBy"),
                        result.getString("sentTo"),
                        result.getLong("timeSent"),
                        result.getString("server"),
                        result.getString("contents")
                );

                output.add(mail);
            }
        } catch (final SQLException e) {
            bot.logger.error(e);
        }

        return output;
    }
}
