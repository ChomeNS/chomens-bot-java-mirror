package me.chayapak1.chomens_bot.data.chat;

public enum ChatPacketType {
    PLAYER("P"),
    DISGUISED("D"),
    SYSTEM("S");

    public final String shortName;

    ChatPacketType (final String shortName) {
        this.shortName = shortName;
    }
}
