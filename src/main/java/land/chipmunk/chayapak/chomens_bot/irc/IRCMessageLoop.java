package land.chipmunk.chayapak.chomens_bot.irc;

import land.chipmunk.chayapak.chomens_bot.data.IRCMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// https://gist.github.com/kaecy/286f8ad334aec3fcb588516feb727772#file-ircmessageloop-java
public abstract class IRCMessageLoop implements Runnable {
    private Socket socket;
    private OutputStream out;

    public List<String> channelList;

    protected boolean initialSetupStatus = false;

    public IRCMessageLoop (String host, int port) {
        channelList = new ArrayList<>();
        try {
            socket = new Socket(host, port);
            out = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String text) {
        byte[] bytes = (text + "\r\n").getBytes();

        try {
            out.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void nick(String nickname) {
        final String msg = "NICK " + nickname;
        send(msg);
    }

    public void user(String username, String hostname, String serverName, String realName) {
        final String msg = "USER " + username + " " + hostname + " " + serverName +  " :" + realName;
        send(msg);
    }

    public void join(String channel) {
        if (!initialSetupStatus) {
            channelList.add(channel);
            return;
        }

        final String msg = "JOIN " + channel;
        send(msg);
    }

    public void part(String channel) {
        final String msg = "PART " + channel;
        send(msg);
    }

    public void channelMessage (String channel, String message) {
        final String msg = "PRIVMSG " + channel + " :" + message;
        send(msg);
    }

    public void pong (String server) {
        final String msg = "PONG " + server;
        send(msg);
    }

    public void quit (String reason) {
        final String msg = "QUIT :Quit: " + reason;
        send(msg);
    }

    protected abstract void onMessage (IRCMessage message);

    private void initialSetup() {
        initialSetupStatus = true;

        // now join the channels. you need to wait for message 001 before you join a channel.
        for (String channel: channelList) {
            join(channel);
        }
    }

    private void processMessage(String ircMessage) {
        final IRCMessage message = MessageParser.message(ircMessage);

        switch (message.command) {
            case "privmsg" -> onMessage(message);
            case "001" -> initialSetup();
            case "ping" -> pong(message.content);
        }
    }

    public void run() {
        final InputStream stream;

        try {
            stream = socket.getInputStream();
            final MessageBuffer messageBuffer = new MessageBuffer();
            final byte[] buffer = new byte[512];

            int count;

            while (true) {
                count = stream.read(buffer);
                if (count == -1) break;
                messageBuffer.append(Arrays.copyOfRange(buffer, 0, count));
                while (messageBuffer.hasCompleteMessage()) {
                    String ircMessage = messageBuffer.getNextMessage();

                    processMessage(ircMessage);
                }
            }
        } catch (IOException e) {
            quit("Internal Error: " + e);
            e.printStackTrace();
        }
    }
}
