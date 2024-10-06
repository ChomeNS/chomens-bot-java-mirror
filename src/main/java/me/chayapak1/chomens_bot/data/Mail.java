package me.chayapak1.chomens_bot.data;

public class Mail {
     public final String sentBy;
     public final String sentTo;
     public final long timeSent;
     public final String server;
     public final String contents;
    
    public Mail (
            String sentBy,
            String sentTo,
            long timeSent,
            String server,
            String contents
    ) {
        this.sentBy = sentBy;
        this.sentTo = sentTo;
        this.timeSent = timeSent;
        this.server = server;
        this.contents = contents;
    }
}
