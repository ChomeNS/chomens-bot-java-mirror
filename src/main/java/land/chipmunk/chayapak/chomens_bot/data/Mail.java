package land.chipmunk.chayapak.chomens_bot.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joda.time.DateTime;

@AllArgsConstructor
public class Mail {
    @Getter private String sentBy;
    @Getter private String sentTo;
    @Getter private long timeSent;
    @Getter private String server;
    @Getter private String contents;
}
