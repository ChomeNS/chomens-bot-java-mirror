package land.chipmunk.chayapak.chomens_bot.irc;

import land.chipmunk.chayapak.chomens_bot.data.IRCMessage;

// https://gist.github.com/kaecy/286f8ad334aec3fcb588516feb727772#file-messageparser-java
public class MessageParser {
    public static IRCMessage message (String ircMessage) {
        final IRCMessage message = new IRCMessage();

        int spIndex;

        if (ircMessage.startsWith(":")) {
            spIndex = ircMessage.indexOf(' ');
            if (spIndex > -1) {
                message.origin = ircMessage.substring(1, spIndex);
                ircMessage = ircMessage.substring(spIndex + 1);

                int uIndex = message.origin.indexOf('!');
                if (uIndex > -1) {
                    message.nickName = message.origin.substring(0, uIndex);

                    message.origin = message.origin.substring(uIndex + 1);
                }
            }
        }

        spIndex = ircMessage.indexOf(' ');
        if (spIndex == -1) {
            message.command = "null";
            return message;
        }

        message.command = ircMessage.substring(0, spIndex).toLowerCase();
        ircMessage = ircMessage.substring(spIndex + 1);

        // parse privmsg params
        if (message.command.equals("privmsg")) {
            spIndex = ircMessage.indexOf(' ');
            message.channel = ircMessage.substring(0, spIndex);
            ircMessage = ircMessage.substring(spIndex + 1);

            if (ircMessage.startsWith(":")) {
                message.content = ircMessage.substring(1);
            } else {
                message.content = ircMessage;
            }
        }

        // parse quit/join
        if (message.command.equals("quit") || message.command.equals("join")) {
            if (ircMessage.startsWith(":")) {
                message.content = ircMessage.substring(1);
            } else {
                message.content = ircMessage;
            }
        }

        // parse ping params
        if (message.command.equals("ping")) {
            spIndex = ircMessage.indexOf(' ');
            if (spIndex > -1) {
                message.content = ircMessage.substring(0, spIndex);
            } else {
                message.content = ircMessage;
            }
        }

        return message;
    }
}
