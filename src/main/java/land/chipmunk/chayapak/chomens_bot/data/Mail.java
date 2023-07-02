package land.chipmunk.chayapak.chomens_bot.data;

public class Mail {
     public String sentBy;
     public String sentTo;
     public long timeSent;
     public String server;
     public String contents;
    
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
