package me.chayapak1.chomens_bot.irc;

public class MessageBuffer {
    private String buffer;

    public MessageBuffer() {
        buffer = "";
    }

    public void append (byte[] bytes) {
        buffer += new String(bytes);
    }

    public boolean hasCompleteMessage() {
        return buffer.contains("\r\n");
    }

    public String getNextMessage() {
        int index = buffer.indexOf("\r\n");
        String message = "";

        if (index > -1) {
            message = buffer.substring(0, index);
            buffer = buffer.substring(index + 2);
        }

        return message;
    }
}
