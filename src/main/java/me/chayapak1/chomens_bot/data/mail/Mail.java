package me.chayapak1.chomens_bot.data.mail;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Mail(String sentBy, String sentTo, long timeSent, String server, String contents) {
    @JsonCreator
    public Mail (
            @JsonProperty("sentBy") String sentBy,
            @JsonProperty("sentTo") String sentTo,
            @JsonProperty("timeSent") long timeSent,
            @JsonProperty("server") String server,
            @JsonProperty("contents") String contents
    ) {
        this.sentBy = sentBy;
        this.sentTo = sentTo;
        this.timeSent = timeSent;
        this.server = server;
        this.contents = contents;
    }

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
