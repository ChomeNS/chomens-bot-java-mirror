package me.chayapak1.chomens_bot.data.mail;

public record Mail(String sentBy, String sentTo, long timeSent, String server, String contents) {
    @Override
    public String toString () {
        return "Mail{" +
                "sentBy='" + sentBy + '\'' +
                ", sentTo='" + sentTo + '\'' +
                ", timeSent=" + timeSent +
                ", server='" + server + '\'' +
                '}';
    }
}
